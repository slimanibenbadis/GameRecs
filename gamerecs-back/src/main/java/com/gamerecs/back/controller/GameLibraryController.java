package com.gamerecs.back.controller;

import com.gamerecs.back.model.GameLibrary;
import com.gamerecs.back.security.CustomUserDetails;
import com.gamerecs.back.service.GameLibraryService;
import io.swagger.v3.oas.annotations.Operation;
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
                           + "Requires valid authentication. Returns HTTP 404 if no library exists.")
    @GetMapping("/game-library")
    public ResponseEntity<GameLibrary> getGameLibrary(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                      HttpServletRequest request) {
        // The authenticated user's id is provided by CustomUserDetails
        GameLibrary library = gameLibraryService.getLibraryForUser(userDetails.getUserId());
        return ResponseEntity.ok(library);
    }
} 
