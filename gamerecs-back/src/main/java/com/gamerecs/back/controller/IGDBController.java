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
import com.gamerecs.back.service.GameSyncService;
import com.gamerecs.back.model.Game;
import org.springframework.security.access.prepost.PreAuthorize;
import com.gamerecs.back.dto.GameSearchResponse;
import com.gamerecs.back.service.GameService;
import org.springframework.data.domain.Page;

@RestController
@RequestMapping("/api/igdb")
public class IGDBController {
    private static final Logger logger = LoggerFactory.getLogger(IGDBController.class);
    private final IGDBClientService igdbClientService;
    private final GameSyncService gameSyncService;
    private final GameService gameService;

    public IGDBController(IGDBClientService igdbClientService, GameSyncService gameSyncService, GameService gameService) {
        this.igdbClientService = igdbClientService;
        this.gameSyncService = gameSyncService;
        this.gameService = gameService;
    }

    /**
     * Triggers an IGDB update using the search query provided via a query parameter.
     * Requires authentication to access this endpoint.
     */
    @PostMapping("/update")
    public ResponseEntity<?> updateIGDBData(@RequestParam("query") String query) {
        logger.debug("Received IGDB update request");
        
        // Check authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            logger.warn("Unauthorized access attempt to IGDB update endpoint");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        logger.debug("User {} requesting IGDB update", userDetails.getUsername());
        
        try {
            // Trigger the IGDB search and sync to database
            List<IGDBGameDTO> igdbResponse = igdbClientService.searchGames(query);
            List<Game> savedGames = gameSyncService.syncGamesFromSearch(igdbResponse);
            
            logger.debug("IGDB search and sync completed successfully for user {}, found and processed {} games", 
                userDetails.getUsername(), savedGames.size());
            
            return ResponseEntity.ok().body(
                new ApiResponse("IGDB update completed and data persisted.", igdbResponse)
            );
        } catch (Exception e) {
            logger.error("Error during IGDB update process", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse("Error during IGDB update process", List.of()));
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
            // Step 1: Trigger the IGDB search and sync to database
            List<IGDBGameDTO> igdbResponse = igdbClientService.searchGames(query);
            List<Game> savedGames = gameSyncService.syncGamesFromSearch(igdbResponse);
            
            logger.debug("IGDB update completed successfully, synced {} games", savedGames.size());
            
            // Step 2: Perform search against the updated database
            logger.debug("Executing search against updated database");
            Page<Game> searchResults = gameService.searchGamesByTitle(query, page, size);
            
            // Step 3: Create and return response
            GameSearchResponse response = new GameSearchResponse();
            response.setGames(searchResults.getContent());
            response.setCurrentPage(searchResults.getNumber());
            response.setTotalPages(searchResults.getTotalPages());
            response.setTotalElements(searchResults.getTotalElements());
            response.setPageSize(searchResults.getSize());
            response.setQuery(query);
            
            logger.debug("Search completed successfully against updated database, found {} results", 
                searchResults.getTotalElements());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error during combined IGDB update and search process", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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
