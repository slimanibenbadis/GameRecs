package com.gamerecs.back.service;

import com.gamerecs.back.dto.IGDBGameDTO;
import com.gamerecs.back.model.Game;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.web.util.UriUtils;
import java.nio.charset.StandardCharsets;

/**
 * Service for handling asynchronous IGDB API updates.
 * This service offloads the API calls and database updates to background threads.
 */
@Service
@RequiredArgsConstructor
public class AsyncIGDBUpdateService {
    private static final Logger logger = LoggerFactory.getLogger(AsyncIGDBUpdateService.class);
    
    private final IGDBClientService igdbClientService;
    private final GameSyncService gameSyncService;
    
    /**
     * Asynchronously fetches game data from IGDB API and updates the local database.
     * This method returns immediately while processing continues in a background thread.
     * 
     * @param query the search query to use for IGDB API
     * @return CompletableFuture containing the number of games processed
     */
    @Async("taskExecutor")
    public CompletableFuture<Integer> updateGamesFromIGDB(String query) {
        logger.info("Starting asynchronous IGDB update for query: {}", query);
        
        try {
            // Sanitize and validate the query
            String sanitizedQuery = sanitizeQuery(query);
            if (sanitizedQuery.isEmpty()) {
                logger.warn("Empty query after sanitization, aborting IGDB update");
                return CompletableFuture.completedFuture(0);
            }
            
            // Fetch game data from IGDB API
            List<IGDBGameDTO> igdbGames = igdbClientService.searchGames(sanitizedQuery);
            logger.debug("Retrieved {} games from IGDB API for query: {}", igdbGames.size(), sanitizedQuery);
            
            if (igdbGames.isEmpty()) {
                logger.info("No games found in IGDB for query: {}", sanitizedQuery);
                return CompletableFuture.completedFuture(0);
            }
            
            // Sync games to local database
            List<Game> savedGames = gameSyncService.syncGamesFromSearch(igdbGames);
            logger.info("Successfully synced {} games from IGDB to database for query: {}", 
                     savedGames.size(), sanitizedQuery);
            
            return CompletableFuture.completedFuture(savedGames.size());
        } catch (Exception e) {
            logger.error("Error during asynchronous IGDB update for query: {}", query, e);
            // Complete exceptionally, allowing the caller to handle the exception if needed
            CompletableFuture<Integer> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
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
        sanitized = sanitized.replaceAll("[<>\"'%;()]", "");
        
        // URL encode the query to ensure it's properly formatted for the API
        sanitized = UriUtils.encode(sanitized, StandardCharsets.UTF_8);
        
        // Limit query length
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }
        
        return sanitized;
    }
} 
