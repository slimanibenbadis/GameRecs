package com.gamerecs.back.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import com.gamerecs.back.dto.IGDBGameDTO;
import com.gamerecs.back.model.Game;

import jakarta.annotation.PostConstruct;

/**
 * Service for handling asynchronous IGDB API updates.
 * This service offloads the API calls and database updates to background threads.
 */
@Service
public class AsyncIGDBUpdateService {
    private static final Logger logger = LoggerFactory.getLogger(AsyncIGDBUpdateService.class);
    private static final int MIN_TIMEOUT_SECONDS = 10; // Minimum timeout to prevent immediate timeout in tests
    
    private final IGDBClientService igdbClientService;
    private final GameSyncService gameSyncService;
    
    @Value("${igdb.update.timeout:30}")
    private int configuredTimeoutSeconds;
    
    private int igdbUpdateTimeoutSeconds;
    
    public AsyncIGDBUpdateService(IGDBClientService igdbClientService, GameSyncService gameSyncService) {
        this.igdbClientService = igdbClientService;
        this.gameSyncService = gameSyncService;
    }
    
    @PostConstruct
    public void init() {
        // Ensure a minimum timeout value to prevent immediate timeouts in tests
        this.igdbUpdateTimeoutSeconds = Math.max(configuredTimeoutSeconds, MIN_TIMEOUT_SECONDS);
        logger.info("IGDB update timeout configured to {} seconds (minimum: {} seconds)", 
                  igdbUpdateTimeoutSeconds, MIN_TIMEOUT_SECONDS);
    }
    
    /**
     * Asynchronously fetches game data from IGDB API and updates the local database.
     * This method returns immediately while processing continues in a background thread.
     * 
     * @param query the search query to use for IGDB API
     * @return CompletableFuture containing the number of games processed
     */
    @Async("taskExecutor")
    @Retryable(
        value = { Exception.class },
        exclude = { IllegalArgumentException.class, TimeoutException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public CompletableFuture<Integer> updateGamesFromIGDB(String query) {
        logger.info("Starting asynchronous IGDB update for query: {}", query);
        
        // Create a CompletableFuture to return
        CompletableFuture<Integer> resultFuture = new CompletableFuture<>();
        
        try {
            // Sanitize and validate the query
            String sanitizedQuery = sanitizeQuery(query);
            if (sanitizedQuery.isEmpty()) {
                logger.warn("Empty query after sanitization, aborting IGDB update");
                resultFuture.complete(0);
                return resultFuture;
            }
            
            // Create a CompletableFuture for the actual processing
            CompletableFuture<Integer> processingFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    // Fetch game data from IGDB API
                    logger.debug("Fetching game data from IGDB API for query: {}", sanitizedQuery);
                    List<IGDBGameDTO> igdbGames = igdbClientService.searchGames(sanitizedQuery);
                    logger.debug("Retrieved {} games from IGDB API for query: {}", igdbGames.size(), sanitizedQuery);
                    
                    if (igdbGames.isEmpty()) {
                        logger.info("No games found in IGDB for query: {}", sanitizedQuery);
                        return 0;
                    }
                    
                    // Sync games to local database
                    logger.debug("Syncing {} games to local database for query: {}", igdbGames.size(), sanitizedQuery);
                    List<Game> savedGames = gameSyncService.syncGamesFromSearch(igdbGames);
                    logger.info("Successfully synced {} games from IGDB to database for query: {}", 
                             savedGames.size(), sanitizedQuery);
                    
                    return savedGames.size();
                } catch (Exception e) {
                    logger.error("Error during game data processing for query: {}", sanitizedQuery, e);
                    throw e;
                }
            });
            
            // Log the timeout value being used
            logger.debug("Setting timeout of {} seconds for IGDB update operation", igdbUpdateTimeoutSeconds);
            
            // Add timeout to the processing future
            CompletableFuture<Integer> timeoutFuture = processingFuture.orTimeout(igdbUpdateTimeoutSeconds, TimeUnit.SECONDS);
            
            // Handle the result or exceptions
            timeoutFuture.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    if (throwable instanceof TimeoutException) {
                        logger.error("IGDB update timed out after {} seconds for query: {}", 
                                  igdbUpdateTimeoutSeconds, sanitizedQuery);
                        resultFuture.completeExceptionally(
                            new TimeoutException("IGDB update timed out after " + igdbUpdateTimeoutSeconds + " seconds"));
                    } else {
                        logger.error("IGDB update failed for query: {}", sanitizedQuery, throwable);
                        resultFuture.completeExceptionally(throwable);
                    }
                } else {
                    logger.info("IGDB update completed successfully for query: {}, processed {} games", 
                             sanitizedQuery, result);
                    resultFuture.complete(result);
                }
            });
            
            return resultFuture;
        } catch (Exception e) {
            logger.error("Error setting up asynchronous IGDB update for query: {}", query, e);
            resultFuture.completeExceptionally(e);
            return resultFuture;
        }
    }
    
    /**
     * For testing purposes - allows overriding the timeout value
     * 
     * @param timeoutSeconds the new timeout value in seconds
     */
    void setTimeoutForTests(int timeoutSeconds) {
        this.igdbUpdateTimeoutSeconds = Math.max(timeoutSeconds, MIN_TIMEOUT_SECONDS);
        logger.debug("Timeout value set to {} seconds for testing", this.igdbUpdateTimeoutSeconds);
    }
    
    /**
     * Waits for the IGDB update to complete with a timeout.
     * This method is useful when you need to ensure the update is complete before proceeding.
     * 
     * @param updateFuture the CompletableFuture returned by updateGamesFromIGDB
     * @param timeoutSeconds maximum time to wait in seconds
     * @return the number of games updated
     * @throws TimeoutException if the update takes longer than the specified timeout
     * @throws ExecutionException if the update fails
     * @throws InterruptedException if the waiting thread is interrupted
     */
    public int waitForUpdateCompletion(CompletableFuture<Integer> updateFuture, int timeoutSeconds) 
            throws TimeoutException, ExecutionException, InterruptedException {
        // Ensure a minimum timeout value
        int effectiveTimeout = Math.max(timeoutSeconds, MIN_TIMEOUT_SECONDS);
        
        try {
            return updateFuture.get(effectiveTimeout, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            logger.error("Timed out waiting for IGDB update to complete after {} seconds", effectiveTimeout);
            throw new TimeoutException("IGDB update timed out after " + effectiveTimeout + " seconds");
        } catch (ExecutionException e) {
            logger.error("IGDB update failed with exception", e.getCause());
            throw e;
        } catch (InterruptedException e) {
            logger.warn("Thread interrupted while waiting for IGDB update to complete");
            Thread.currentThread().interrupt();
            throw e;
        }
    }
    
    /**
     * Sanitizes and validates the search query
     * 
     * @param query the original search query
     * @return sanitized query
     */
    private String sanitizeQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "";
        }
        
        // Basic sanitization - trim and encode
        String sanitized = query.trim();
        
        // Remove any special characters that might cause issues with the IGDB API
        sanitized = sanitized.replaceAll("[<>\"'%;(){}\\[\\]]", "");
        
        // URL encode the query to ensure it's properly formatted for the API
        sanitized = UriUtils.encode(sanitized, StandardCharsets.UTF_8);
        
        // Limit query length
        if (sanitized.length() > 100) {
            logger.warn("Query too long, truncating from {} to 100 characters", sanitized.length());
            sanitized = sanitized.substring(0, 100);
        }
        
        // Validate minimum length
        if (sanitized.length() < 2) {
            logger.warn("Query too short after sanitization: '{}'", sanitized);
            return "";
        }
        
        return sanitized;
    }
} 
