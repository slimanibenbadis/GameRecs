package com.gamerecs.back.controller;

import com.gamerecs.back.model.GameLibrary;
import com.gamerecs.back.security.CustomUserDetails;
import com.gamerecs.back.service.GameLibraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
} 
