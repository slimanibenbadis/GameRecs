package com.gamerecs.back.controller;

import com.gamerecs.back.model.GameLibrary;
import com.gamerecs.back.model.PaginatedGameLibraryResponse;
import com.gamerecs.back.security.CustomUserDetails;
import com.gamerecs.back.service.GameLibraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Game Library", description = "Endpoints for accessing a user's game library")
@RestController
@RequestMapping("/api")
public class GameLibraryController {

    private final GameLibraryService gameLibraryService;

    @Autowired
    public GameLibraryController(GameLibraryService gameLibraryService) {
        this.gameLibraryService = gameLibraryService;
    }

    @Operation(summary = "Get the authenticated user's game library",
               description = "Returns the game library associated with the authenticated user. "
                           + "Requires valid authentication. Returns HTTP 404 if no library exists. "
                           + "Supports sorting by 'title' or 'releaseDate' and filtering by genre name.")
    @GetMapping("/game-library")
    public ResponseEntity<GameLibrary> getGameLibrary(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request,
            @Parameter(description = "Field to sort games by (title or releaseDate)", example = "title")
            @RequestParam(required = false, defaultValue = "title") String sortBy,
            @Parameter(description = "Genre name to filter games by (empty for no filtering)", example = "Action")
            @RequestParam(required = false, defaultValue = "") String filterByGenre) {
        // The authenticated user's id is provided by CustomUserDetails
        GameLibrary library = gameLibraryService.getLibraryForUser(userDetails.getUserId(), sortBy, filterByGenre);
        return ResponseEntity.ok(library);
    }
    
    @Operation(summary = "Get the authenticated user's game library with pagination",
               description = "Returns a paginated game library associated with the authenticated user. "
                           + "Requires valid authentication. Returns HTTP 404 if no library exists. "
                           + "Supports sorting by 'title' or 'releaseDate', filtering by genre name, "
                           + "and pagination parameters (page, size).")
    @GetMapping("/game-library/paginated")
    public ResponseEntity<PaginatedGameLibraryResponse> getPaginatedGameLibrary(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request,
            @Parameter(description = "Field to sort games by (title or releaseDate)", example = "title")
            @RequestParam(required = false, defaultValue = "title") String sortBy,
            @Parameter(description = "Genre name to filter games by (empty for no filtering)", example = "Action")
            @RequestParam(required = false, defaultValue = "") String filterByGenre,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(required = false, defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(required = false, defaultValue = "10") int size) {
        
        // Validate page and size (must be non-negative and positive where appropriate)
        if (page < 0 || size <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination parameters");
        }
        
        // The authenticated user's id is provided by CustomUserDetails
        PaginatedGameLibraryResponse response = gameLibraryService.getPaginatedLibraryForUser(
            userDetails.getUserId(), sortBy, filterByGenre, page, size);
        
        return ResponseEntity.ok(response);
    }
} 
