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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.gamerecs.back.service.SteamImportService;
import com.gamerecs.back.exception.UserNotFoundException;

@Tag(name = "Game Library", description = "Endpoints for accessing and managing a user's game library")
@RestController
@RequestMapping("/api")
public class GameLibraryController {

    private static final Logger log = LoggerFactory.getLogger(GameLibraryController.class);

    private final GameLibraryService gameLibraryService;
    private final SteamImportService steamImportService;

    @Autowired
    public GameLibraryController(GameLibraryService gameLibraryService, SteamImportService steamImportService) {
        this.gameLibraryService = gameLibraryService;
        this.steamImportService = steamImportService;
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

    @Operation(summary = "Import Steam library for the authenticated user",
               description = "Initiates the import of the authenticated user's game library from Steam. "
                           + "Requires the user's 64-bit Steam ID. This operation can take some time depending on library size. "
                           + "Returns HTTP 200 OK on successful initiation, or an error status if the import fails (e.g., invalid Steam ID, external service issues).")
    @PostMapping("/game-library/import/steam")
    public ResponseEntity<Void> importSteamLibrary(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "The user's 64-bit Steam ID", required = true, example = "76561197960287930")
            @RequestParam String steamId) {

        log.info("Received request to import Steam library for user ID: {} with Steam ID: {}", userDetails.getUserId(), steamId);

        try {
            // Validate steamId format (basic check, more robust validation might be needed)
            if (steamId == null || !steamId.matches("\\d{17}")) {
                 log.warn("Invalid Steam ID format provided: {}", steamId);
                 throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Steam ID format. Please provide a 17-digit SteamID64.");
            }
            
            steamImportService.importSteamLibrary(userDetails.getUserId(), steamId);
            log.info("Successfully initiated Steam library import for user ID: {}", userDetails.getUserId());
            return ResponseEntity.ok().build();
        } catch (UserNotFoundException e) {
            // This shouldn't happen if the user is authenticated, but handle defensively
            log.error("User not found during Steam import attempt for authenticated user ID: {}", userDetails.getUserId(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "User not found unexpectedly.");
        } catch (ResponseStatusException rse) {
             // Re-throw client errors (like BAD_REQUEST for invalid steamId)
             throw rse;
        } catch (Exception e) {
            // Catch other potential exceptions from the service layer (e.g., Steam API errors, IGDB errors)
            // GlobalExceptionHandler will handle these and return appropriate 5xx responses
            log.error("Error during Steam library import for user ID: {}", userDetails.getUserId(), e);
            // Let GlobalExceptionHandler handle wrapping this as a 500 or 503 etc.
            throw new RuntimeException("Failed to import Steam library due to an internal error.", e);
        }
    }
} 
