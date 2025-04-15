package com.gamerecs.back.controller;

import com.gamerecs.back.dto.GameSearchResponse;
import com.gamerecs.back.model.Game;
import com.gamerecs.back.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Games", description = "Endpoints for accessing game data")
@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;

    @Autowired
    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @Operation(summary = "Search for games in the local database",
               description = "Returns games that match the provided search query. " +
                             "The query must be at least 2 characters long. " +
                             "Results are paginated and sorted alphabetically by title.")
    @GetMapping("/search")
    public ResponseEntity<GameSearchResponse> searchGames(
            @Parameter(description = "Search query (minimum 2 characters)", example = "zelda")
            @RequestParam String query,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(required = false, defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(required = false, defaultValue = "10") int size) {
        
        // Validate pagination parameters
        if (page < 0 || size <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination parameters");
        }
        
        // Call the service to perform the search
        Page<Game> gamePage = gameService.searchGamesByTitle(query, page, size);
        
        // Create response DTO
        GameSearchResponse response = new GameSearchResponse();
        response.setGames(gamePage.getContent());
        response.setCurrentPage(gamePage.getNumber());
        response.setTotalPages(gamePage.getTotalPages());
        response.setTotalElements(gamePage.getTotalElements());
        response.setPageSize(gamePage.getSize());
        response.setQuery(query);
        
        return ResponseEntity.ok(response);
    }
} 
