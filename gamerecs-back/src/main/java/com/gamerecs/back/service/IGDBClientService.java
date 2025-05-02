package com.gamerecs.back.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamerecs.back.config.CacheConfig;
import com.gamerecs.back.dto.*;
import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.annotation.PostConstruct;

@Service
public class IGDBClientService {
    private static final Logger logger = LoggerFactory.getLogger(IGDBClientService.class);
    
    private final RestTemplate restTemplate;
    private final String clientId;
    private final String accessToken;
    private final ObjectMapper objectMapper;
    
    private static final int STEAM_ID_BATCH_SIZE = 100;
    private static final String IGDB_API_ENDPOINT = "https://api.igdb.com/v4/games";
    
    /**
     * RateLimiter to enforce a cap of 4 requests per second to the IGDB API
     */
    private RateLimiter rateLimiter;
    
    /**
     * Number of permits per second (API calls allowed per second)
     */
    private static final double RATE_LIMIT = 4.0;

    public IGDBClientService(
            RestTemplate restTemplate,
            @Qualifier("igdbClientId") String clientId,
            @Qualifier("igdbAccessToken") String accessToken,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.clientId = clientId;
        this.accessToken = accessToken;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Initialize the rate limiter after bean construction
     */
    @PostConstruct
    public void init() {
        this.rateLimiter = RateLimiter.create(RATE_LIMIT);
        logger.info("Initialized IGDB API rate limiter with {} requests per second", RATE_LIMIT);
    }

    /**
     * Search for games in the IGDB API based on the provided query
     * Results are cached to minimize external API calls
     * Rate limited to 4 requests per second
     * Auto-retries on transient failures
     * 
     * @param query the search query
     * @return list of IGDBGameDTO objects matching the search criteria
     */
    @Cacheable(value = CacheConfig.IGDB_GAME_SEARCH_CACHE, key = "#query")
    @Retryable(
        value = {RestClientException.class, ResourceAccessException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<IGDBGameDTO> searchGames(String query) {
        logger.debug("Attempting to search games from IGDB with query: '{}'", query);
        
        // Acquire a permit from the rate limiter before proceeding
        acquireRateLimitPermit();
        
        try {
            HttpHeaders headers = createHeaders();
            long currentTimestamp = Instant.now().getEpochSecond();
            String body = String.format(
                "search \"%s\";\n" +
                "fields name,cover.url,first_release_date,summary,platforms.name,genres.name,\n" +
                "      involved_companies.company.name,involved_companies.developer,involved_companies.publisher,\n" +
                "      updated_at;\n" +
                "where first_release_date != null & first_release_date <= %d & version_parent = null & parent_game = null;\n" + 
                "limit 500;",
                query, currentTimestamp
            );
            
            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                IGDB_API_ENDPOINT,
                HttpMethod.POST,
                request,
                String.class
            );
            
            List<IGDBGameDTO> games = objectMapper.readValue(
                response.getBody(),
                new TypeReference<List<IGDBGameDTO>>() {}
            );
            
            games = postProcessGames(games);
            
            logger.info("Successfully retrieved and processed {} games from IGDB API for query '{}'", games.size(), query);
            return games;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                logger.warn("IGDB API rate limit exceeded for query: {}", query);
                throw new RestClientException("IGDB API rate limit exceeded", e);
            }
            logger.error("HTTP error from IGDB API for query: {}: {}", query, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Failed to parse IGDB API response for query: {}", query, e);
            throw new RestClientException("Failed to parse IGDB API response", e);
        }
    }
    
    /**
     * Recovery method for searchGames when all retries are exhausted
     * 
     * @param e the exception that triggered recovery
     * @param query the original search query
     * @return an empty list as fallback
     */
    @Recover
    public List<IGDBGameDTO> recoverSearchGames(Exception e, String query) {
        logger.error("All retry attempts exhausted for IGDB search with query '{}'. Returning empty result.", query, e);
        return Collections.emptyList();
    }
    
    /**
     * Search for games in the IGDB API based on a list of Steam App IDs.
     * Results are cached. Handles batching for large lists.
     * Rate limited and includes retry logic.
     * 
     * @param steamAppIds List of Steam Application IDs (as strings).
     * @return List of matching IGDBGameDTO objects.
     */
    @Cacheable(value = CacheConfig.IGDB_STEAM_ID_SEARCH_CACHE, key = "'steamids:' + T(java.util.Collections).sort(#steamAppIds) + #steamAppIds.toString()")
    public List<IGDBGameDTO> searchGamesBySteamAppIds(List<String> steamAppIds) {
        if (steamAppIds == null || steamAppIds.isEmpty()) {
            logger.warn("Attempted to search games with null or empty Steam App ID list.");
            return Collections.emptyList();
        }
        
        // Sort for consistent caching key (already done in @Cacheable expression, but good practice here too)
        Collections.sort(steamAppIds);
        logger.debug("Searching games by Steam App IDs (count: {})", steamAppIds.size());
        
        List<IGDBGameDTO> allGames = new ArrayList<>();
        int listSize = steamAppIds.size();
        
        for (int i = 0; i < listSize; i += STEAM_ID_BATCH_SIZE) {
            int end = Math.min(i + STEAM_ID_BATCH_SIZE, listSize);
            List<String> batch = steamAppIds.subList(i, end);
            logger.debug("Processing batch {}/{} for Steam IDs (size: {})", (i / STEAM_ID_BATCH_SIZE) + 1, (int) Math.ceil((double) listSize / STEAM_ID_BATCH_SIZE), batch.size());
            try {
                List<IGDBGameDTO> batchResult = fetchGamesBatchBySteamAppIds(batch);
                allGames.addAll(batchResult);
            } catch (Exception e) {
                logger.error("Failed to process batch for Steam IDs: {} - {}. Error: {}", i, end - 1, e.getMessage());
                // Depending on requirements, could rethrow, or continue to next batch
            }
        }
        
        logger.info("Finished searching by Steam IDs. Total games found: {}", allGames.size());
        return allGames;
    }

    /**
     * Fetches a single batch of games from IGDB based on Steam App IDs.
     * Internal method with retry logic.
     *
     * @param batchSteamAppIds List of Steam App IDs for this batch (max size: STEAM_ID_BATCH_SIZE).
     * @return List of matching IGDBGameDTO objects.
     */
    @Retryable(
        value = {RestClientException.class, ResourceAccessException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1500, multiplier = 2) // Slightly longer delay for potentially heavier queries
    )
    private List<IGDBGameDTO> fetchGamesBatchBySteamAppIds(List<String> batchSteamAppIds) {
        logger.debug("Fetching batch of games from IGDB by Steam IDs: {}", batchSteamAppIds);
        
        acquireRateLimitPermit();
        
        try {
            HttpHeaders headers = createHeaders();
            long currentTimestamp = Instant.now().getEpochSecond();
            String steamIdsString = String.join(",", batchSteamAppIds);
            
            String body = String.format(
                "fields name,cover.url,first_release_date,summary,platforms.name,genres.name,\n" +
                "      involved_companies.company.name,involved_companies.developer,involved_companies.publisher,\n" +
                "      external_games.category, external_games.uid, updated_at;\n" +
                "where external_games.uid = (%s) & external_games.category = 1 & first_release_date != null & first_release_date <= %d & version_parent = null & parent_game = null;\n" +
                "limit %d;", // Use batch size as limit, though IGDB max is 500
                steamIdsString, currentTimestamp, STEAM_ID_BATCH_SIZE 
            );
            
            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                IGDB_API_ENDPOINT,
                HttpMethod.POST,
                request,
                String.class
            );
            
            List<IGDBGameDTO> games = objectMapper.readValue(
                response.getBody(),
                new TypeReference<List<IGDBGameDTO>>() {}
            );
            
            games = postProcessGames(games);
            
            logger.debug("Successfully retrieved batch of {} games from IGDB for Steam IDs", games.size());
            return games;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                logger.warn("IGDB API rate limit exceeded for Steam ID batch: {}", batchSteamAppIds);
                throw new RestClientException("IGDB API rate limit exceeded", e);
            }
            logger.error("HTTP error from IGDB API for Steam ID batch {}: {}", batchSteamAppIds, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Failed to parse IGDB API response for Steam ID batch: {}", batchSteamAppIds, e);
            throw new RestClientException("Failed to parse IGDB API response", e);
        }
    }
    
    /**
     * Recovery method for fetchGamesBatchBySteamAppIds when all retries are exhausted.
     *
     * @param e the exception that triggered recovery
     * @param batchSteamAppIds the list of Steam App IDs for the failed batch
     * @return an empty list as fallback
     */
    @Recover
    private List<IGDBGameDTO> recoverFetchGamesBatchBySteamAppIds(Exception e, List<String> batchSteamAppIds) {
        logger.error("All retry attempts exhausted for IGDB Steam ID batch '{}'. Returning empty result for this batch.", batchSteamAppIds, e);
        return Collections.emptyList();
    }

    /**
     * Helper method to perform post-processing on a list of IGDBGameDTOs.
     * Converts cover URLs, release dates, and processes involved companies.
     *
     * @param games List of IGDBGameDTOs to process.
     * @return The processed list of IGDBGameDTOs.
     */
    private List<IGDBGameDTO> postProcessGames(List<IGDBGameDTO> games) {
        if (games == null) return Collections.emptyList();
        
        for (IGDBGameDTO game : games) {
            // Convert cover URL
            if (game.getCoverImage() != null && game.getCoverImage().getUrl() != null) {
                game.setCoverImageUrl(convertCoverUrl(game.getCoverImage().getUrl()));
            }
            
            // Convert release date to LocalDate
            if (game.getReleaseDateTimestamp() != null) {
                game.setReleaseDate(
                    Instant.ofEpochSecond(game.getReleaseDateTimestamp())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                );
            }
            
            // Process involved companies into publishers and developers
            game.processInvolvedCompanies();
        }
        return games;
    }

    /**
     * Helper method to create standard HttpHeaders for IGDB requests.
     *
     * @return Configured HttpHeaders object.
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Client-ID", clientId);
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.TEXT_PLAIN);
        return headers;
    }

    /**
     * Acquires a permit from the rate limiter, logging the wait time if any.
     */
    private void acquireRateLimitPermit() {
        double waitTime = rateLimiter.acquire();
        if (waitTime > 0.0) {
            logger.debug("Rate limiter delay: {} seconds", String.format("%.3f", waitTime));
        }
    }

    /**
     * Converts the original cover URL from IGDB to the proper format
     * 
     * @param originalUrl the original URL from IGDB
     * @return the converted URL
     */
    public String convertCoverUrl(String originalUrl) {
        if (originalUrl == null) {
            return null;
        }
        
        String httpsUrl = originalUrl.startsWith("//") 
            ? "https:" + originalUrl 
            : originalUrl;
            
        return httpsUrl.replace("t_thumb", "t_cover_big");
    }
    
    /**
     * Manually evict all entries from the IGDB game search cache
     * Useful for administrative purposes or when force-refreshing data
     */
    @CacheEvict(value = {CacheConfig.IGDB_GAME_SEARCH_CACHE, CacheConfig.IGDB_STEAM_ID_SEARCH_CACHE}, allEntries = true)
    public void clearGameSearchCache() {
        logger.info("Manually cleared all IGDB search caches");
    }
} 
