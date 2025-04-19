package com.gamerecs.back.controller;

import com.gamerecs.back.service.IGDBClientService;
import com.gamerecs.back.dto.IGDBGameDTO;
import com.gamerecs.back.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import com.gamerecs.back.service.GameSyncService;
import com.gamerecs.back.model.Game;
import org.springframework.security.access.prepost.PreAuthorize;
import com.gamerecs.back.dto.GameSearchResponse;
import com.gamerecs.back.service.GameService;
import org.springframework.data.domain.Page;
import com.gamerecs.back.service.AsyncIGDBUpdateService;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/igdb")
public class IGDBController {
    private static final Logger logger = LoggerFactory.getLogger(IGDBController.class);
    private static final int MAX_QUERY_LENGTH = 100;
    private static final int MIN_QUERY_LENGTH = 2;
    
    private final IGDBClientService igdbClientService;
    private final GameSyncService gameSyncService;
    private final GameService gameService;
    private final AsyncIGDBUpdateService asyncIGDBUpdateService;

    public IGDBController(
            IGDBClientService igdbClientService, 
            GameSyncService gameSyncService, 
            GameService gameService,
            AsyncIGDBUpdateService asyncIGDBUpdateService) {
        this.igdbClientService = igdbClientService;
        this.gameSyncService = gameSyncService;
        this.gameService = gameService;
        this.asyncIGDBUpdateService = asyncIGDBUpdateService;
    }

    /**
     * Triggers an IGDB update using the search query provided via a query parameter.
     * Requires authentication to access this endpoint.
     * 
     * This endpoint now processes the update asynchronously, returning immediately
     * to the client while the update continues in the background.
     */
    @PostMapping("/update")
    public ResponseEntity<?> updateIGDBData(@RequestParam("query") String query) {
        logger.debug("Received IGDB update request for query: {}", query);
        
        // Check authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            logger.warn("Unauthorized access attempt to IGDB update endpoint");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        logger.debug("User {} requesting IGDB update", userDetails.getUsername());
        
        try {
            // Validate query
            if (query == null || query.trim().isEmpty()) {
                logger.warn("Empty search query received for IGDB update from user {}", userDetails.getUsername());
                return ResponseEntity.badRequest().body(
                    new ApiResponse("Search query cannot be empty", List.of())
                );
            }
            
            // Check minimum query length
            if (query.trim().length() < MIN_QUERY_LENGTH) {
                logger.warn("Query too short received for IGDB update from user {}: '{}'", 
                          userDetails.getUsername(), query);
                return ResponseEntity.badRequest().body(
                    new ApiResponse("Search query must be at least " + MIN_QUERY_LENGTH + " characters long", List.of())
                );
            }
            
            // Sanitize the query
            String sanitizedQuery = sanitizeQuery(query);
            if (!sanitizedQuery.equals(query)) {
                logger.warn("Query sanitized for user {}: '{}' -> '{}'", 
                          userDetails.getUsername(), query, sanitizedQuery);
            }
            
            // Trigger the asynchronous update process
            // This will return immediately and continue processing in the background
            asyncIGDBUpdateService.updateGamesFromIGDB(sanitizedQuery);
            
            logger.info("Asynchronous IGDB update initiated for query: {} by user {}", 
                     sanitizedQuery, userDetails.getUsername());
            
            return ResponseEntity.accepted().body(
                new ApiResponse("IGDB update initiated and will be processed asynchronously", List.of())
            );
        } catch (Exception e) {
            logger.error("Error starting IGDB update process for user {}", userDetails.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse("Error starting IGDB update process: " + e.getMessage(), List.of()));
        }
    }
    
    /**
     * Endpoint to manually clear the IGDB game search cache.
     * Requires ADMIN role to perform this operation.
     * 
     * @return ResponseEntity with a success message
     */
    @PostMapping("/clear-cache")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> clearIGDBCache() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        
        logger.info("Admin user {} requesting to clear IGDB cache", userDetails.getUsername());
        
        try {
            igdbClientService.clearGameSearchCache();
            return ResponseEntity.ok("IGDB cache successfully cleared");
        } catch (Exception e) {
            logger.error("Error while clearing IGDB cache", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to clear IGDB cache: " + e.getMessage());
        }
    }
    
    /**
     * Combined endpoint that updates IGDB data and then performs a search with the updated data.
     * This guarantees that search results include the most current game data.
     *
     * @param query The search query string
     * @param page The page number (0-indexed)
     * @param size The page size
     * @return GameSearchResponse containing search results after IGDB update
     */
    @PostMapping("/update-and-search")
    public ResponseEntity<GameSearchResponse> updateAndSearch(
            @RequestParam("query") String query,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "50") int size) {
        
        logger.debug("Received combined IGDB update and search request for query: {}", query);

        // Check authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            logger.warn("Unauthorized access attempt to update-and-search endpoint");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        logger.debug("User {} requesting IGDB update and search", userDetails.getUsername());
        
        try {
            // Validate query
            if (query == null || query.trim().isEmpty()) {
                logger.warn("Empty search query received for IGDB update-and-search from user {}", 
                          userDetails.getUsername());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search query cannot be empty");
            }
            
            // Check minimum query length
            if (query.trim().length() < MIN_QUERY_LENGTH) {
                logger.warn("Query too short received for IGDB update-and-search from user {}: '{}'", 
                          userDetails.getUsername(), query);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Search query must be at least " + MIN_QUERY_LENGTH + " characters long");
            }
            
            // Sanitize the query
            String sanitizedQuery = sanitizeQuery(query);
            if (!sanitizedQuery.equals(query)) {
                logger.warn("Query sanitized for user {}: '{}' -> '{}'", 
                          userDetails.getUsername(), query, sanitizedQuery);
            }
            
            // Validate pagination parameters
            if (page < 0) {
                logger.warn("Invalid page number from user {}: {}", userDetails.getUsername(), page);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page number cannot be negative");
            }
            
            if (size <= 0 || size > 100) {
                logger.warn("Invalid page size from user {}: {}", userDetails.getUsername(), size);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page size must be between 1 and 100");
            }
            
            // Step 1: Trigger the IGDB update for this specific request
            logger.info("Starting IGDB update for query '{}' by user {}", 
                      sanitizedQuery, userDetails.getUsername());
            
            Integer gamesUpdated;
            try {
                // The difference with the /update endpoint is that we wait for THIS update to complete
                // but any previous async updates can continue in the background
                gamesUpdated = asyncIGDBUpdateService.updateGamesFromIGDB(sanitizedQuery).get();
                logger.debug("IGDB update completed for current request, synced {} games", gamesUpdated);
            } catch (Exception e) {
                Throwable rootCause = e;
                
                // Extract the cause from CompletionException or ExecutionException
                if (e instanceof CompletionException || 
                    e.getClass().getName().equals("java.util.concurrent.ExecutionException")) {
                    rootCause = e.getCause();
                }
                
                logger.error("IGDB update failed for query '{}' by user {}", 
                          sanitizedQuery, userDetails.getUsername(), e);
                
                // Handle interrupted exception
                if (e.getClass().getName().equals("java.util.concurrent.InterruptedException")) {
                    Thread.currentThread().interrupt(); // Restore interrupted state
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, 
                        "IGDB update was interrupted. Please try again.");
                }
                
                // If the cause is a timeout, provide a specific message
                if (rootCause instanceof TimeoutException) {
                    throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, 
                        "IGDB service timed out. Please try again later.");
                }
                
                // For completion or execution exceptions with a different cause, return 503
                if (e instanceof CompletionException || 
                    e.getClass().getName().equals("java.util.concurrent.ExecutionException")) {
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, 
                        "IGDB service is temporarily unavailable. Search will proceed with existing data.");
                }
                
                // For all other exceptions, return 500
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Unexpected error during IGDB update: " + e.getMessage());
            }
            
            // Step 2: Perform search against the updated database
            logger.debug("Executing search against updated database for query '{}'", sanitizedQuery);
            Page<Game> searchResults;
            try {
                searchResults = gameService.searchGamesByTitleNormalized(sanitizedQuery, page, size);
            } catch (ResponseStatusException e) {
                // Re-throw with the same status code
                throw e;
            } catch (Exception e) {
                logger.error("Search failed after IGDB update for query '{}' by user {}", 
                          sanitizedQuery, userDetails.getUsername(), e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Error during game search: " + e.getMessage());
            }
            
            // Step 3: Create and return response
            GameSearchResponse response = new GameSearchResponse();
            response.setGames(searchResults.getContent());
            response.setCurrentPage(searchResults.getNumber());
            response.setTotalPages(searchResults.getTotalPages());
            response.setTotalElements(searchResults.getTotalElements());
            response.setPageSize(searchResults.getSize());
            response.setQuery(sanitizedQuery);
            
            logger.debug("Search completed successfully against updated database, found {} results", 
                searchResults.getTotalElements());
            
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            // Log the error and re-throw
            logger.warn("Response status exception during update-and-search: {} - {}", 
                     e.getStatusCode(), e.getReason());
            throw e;
        } catch (Exception e) {
            // Log the error and wrap with a ResponseStatusException
            logger.error("Unexpected error during update-and-search for query '{}' by user {}", 
                       query, userDetails.getUsername(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Unexpected error during update and search: " + e.getMessage());
        }
    }
    
    /**
     * Sanitizes a search query to prevent injection and other attacks
     * 
     * @param query The original query string
     * @return A sanitized query string
     */
    private String sanitizeQuery(String query) {
        if (query == null) {
            return "";
        }
        
        // Trim whitespace
        String sanitized = query.trim();
        
        // Remove potentially harmful characters
        sanitized = sanitized.replaceAll("[;\"'<>()\\[\\]{}]", "");
        
        // Limit length
        if (sanitized.length() > MAX_QUERY_LENGTH) {
            sanitized = sanitized.substring(0, MAX_QUERY_LENGTH);
        }
        
        return sanitized;
    }
    
    public static class ApiResponse {
        private final String message;
        private final List<IGDBGameDTO> data;

        public ApiResponse(String message, List<IGDBGameDTO> data) {
            this.message = message;
            this.data = data;
        }
        
        public String getMessage() {
            return message;
        }
        
        public List<IGDBGameDTO> getData() {
            return data;
        }
    }
} 
