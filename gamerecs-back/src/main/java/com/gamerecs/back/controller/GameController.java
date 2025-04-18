package com.gamerecs.back.controller;

import com.gamerecs.back.dto.GameSearchResponse;
import com.gamerecs.back.model.Game;
import com.gamerecs.back.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Games", description = "Endpoints for accessing game data")
@RestController
@RequestMapping("/api/games")
public class GameController {
    private static final Logger logger = LoggerFactory.getLogger(GameController.class);
    private final GameService gameService;

    @Autowired
    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @Operation(summary = "Search for games in the local database",
               description = "Returns games that match the provided search query. " +
                             "The query must be at least 2 characters long. " +
                             "Results are paginated and sorted alphabetically by title. " +
                             "This search is accent-insensitive and punctuation-insensitive.")
    @GetMapping("/search")
    public ResponseEntity<GameSearchResponse> searchGames(
            @Parameter(description = "Search query (minimum 2 characters)", example = "zelda")
            @RequestParam String query,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(required = false, defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(required = false, defaultValue = "10") int size) {
        
        // Get the current user for logging
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "anonymous";
        
        logger.debug("User '{}' searching for games with query: '{}', page: {}, size: {}", 
                    username, query, page, size);
                    
        try {
            // Validate query parameter - check if null or empty
            if (query == null || query.trim().isEmpty()) {
                logger.warn("Empty search query from user '{}'", username);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search query cannot be empty");
            }
            
            // Validate query parameter - check minimum length
            if (query.trim().length() < 2) {
                logger.warn("Search query too short from user '{}': '{}'", username, query);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search query must be at least 2 characters long");
            }
            
            // Sanitize the query to prevent SQL injection
            String sanitizedQuery = sanitizeQuery(query);
            if (!sanitizedQuery.equals(query)) {
                logger.warn("Query sanitized for user '{}': '{}' -> '{}'", username, query, sanitizedQuery);
            }
            
            // Validate pagination parameters
            if (page < 0) {
                logger.warn("Invalid page number from user '{}': {}", username, page);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page number cannot be negative");
            }
            
            if (size <= 0 || size > 100) {
                logger.warn("Invalid page size from user '{}': {}", username, size);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page size must be between 1 and 100");
            }
            
            // Call the service to perform the search using the normalized search method
            logger.info("User '{}' searching for '{}' (page: {}, size: {})", username, sanitizedQuery, page, size);
            Page<Game> gamePage = gameService.searchGamesByTitleNormalized(sanitizedQuery, page, size);
            
            // Create response DTO
            GameSearchResponse response = new GameSearchResponse();
            response.setGames(gamePage.getContent());
            response.setCurrentPage(gamePage.getNumber());
            response.setTotalPages(gamePage.getTotalPages());
            response.setTotalElements(gamePage.getTotalElements());
            response.setPageSize(gamePage.getSize());
            response.setQuery(sanitizedQuery);
            
            logger.debug("Search results for '{}': found {} games total, returning page {} with {} games", 
                      sanitizedQuery, gamePage.getTotalElements(), page, gamePage.getContent().size());
            
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            // Re-throw ResponseStatusException as is
            throw e;
        } catch (Exception e) {
            // Log the error and wrap with a ResponseStatusException
            logger.error("Error processing search request from user '{}' for query '{}': {}", 
                       username, query, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error processing search request: " + e.getMessage(), e);
        }
    }
    
    /**
     * Sanitizes a search query to prevent SQL injection and other attacks
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
        // This is a simple approach - a production app might use a more sophisticated sanitization library
        sanitized = sanitized.replaceAll("[;\"'<>()\\[\\]{}]", "");
        
        // Limit length
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }
        
        return sanitized;
    }
} 
