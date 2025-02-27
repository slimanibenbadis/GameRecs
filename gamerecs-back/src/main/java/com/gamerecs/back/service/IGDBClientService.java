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
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import jakarta.annotation.PostConstruct;

@Service
public class IGDBClientService {
    private static final Logger logger = LoggerFactory.getLogger(IGDBClientService.class);
    
    private final RestTemplate restTemplate;
    private final String clientId;
    private final String accessToken;
    private final ObjectMapper objectMapper;
    
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
        logger.debug("Cache miss for IGDB game search");
        
        // Acquire a permit from the rate limiter before proceeding
        double waitTime = rateLimiter.acquire();
        if (waitTime > 0.0) {
            logger.debug("Rate limiter delay: {} seconds", waitTime);
        }
        
        try {
            String endpoint = "https://api.igdb.com/v4/games";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Client-ID", clientId);
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.TEXT_PLAIN);
            
            String body = """
                search "%s";
                fields name,cover.url,first_release_date,summary,platforms.name,genres.name,
                      involved_companies.company.name,involved_companies.developer,involved_companies.publisher,
                      updated_at;
                      where first_release_date != null & version_parent = null & game_type = 0;
                limit 500;
                """.formatted(query);
            
            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                endpoint,
                HttpMethod.POST,
                request,
                String.class
            );
            
            List<IGDBGameDTO> games = objectMapper.readValue(
                response.getBody(),
                new TypeReference<List<IGDBGameDTO>>() {}
            );
            
            // Post-process the games
            for (IGDBGameDTO game : games) {
                // Convert cover URL
                if (game.getCoverImage() != null) {
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
            
            logger.info("Successfully retrieved and processed {} games from IGDB API", games.size());
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
    @CacheEvict(value = CacheConfig.IGDB_GAME_SEARCH_CACHE, allEntries = true)
    public void clearGameSearchCache() {
        logger.info("Manually cleared IGDB game search cache");
    }
} 
