package com.gamerecs.back.service;

import com.gamerecs.back.dto.IGDBGameDTO;
import com.gamerecs.back.exception.UserNotFoundException;
import com.gamerecs.back.model.Game;
import com.gamerecs.back.model.GameLibrary;
import com.gamerecs.back.model.User;
import com.gamerecs.back.repository.GameLibraryRepository;
import com.gamerecs.back.repository.UserRepository;
import com.gamerecs.back.service.SteamCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service responsible for orchestrating the import of a user's Steam game library.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SteamImportService {

    private final SteamClientService steamClientService;
    private final IGDBClientService igdbClientService;
    private final GameSyncService gameSyncService;
    private final GameLibraryRepository gameLibraryRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    /**
     * Imports a user's Steam library based on their stored credentials.
     * Fetches owned games from Steam, finds corresponding games on IGDB,
     * syncs game data to the local database, and adds new games to the user's library.
     *
     * // TODO: Consider making this operation asynchronous (@Async) for better performance with large libraries.
     *
     * @param userId The ID of the user performing the import.
     * @throws UserNotFoundException if the user with the given userId is not found.
     * @throws IllegalStateException if Steam credentials are not configured for the user.
     */
    @Transactional
    public void importSteamLibrary(Long userId) throws UserNotFoundException, IllegalStateException {
        log.info("Starting Steam library import for user ID: {}", userId);

        // 1. Retrieve the User entity
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        // 1.5 Fetch decrypted Steam credentials
        SteamCredentials credentials = userService.getDecryptedSteamCredentials(userId)
            .orElseThrow(() -> {
                log.warn("Steam credentials not configured for user ID: {}", userId);
                return new IllegalStateException("Steam credentials not configured for this user.");
            });
        log.info("Successfully retrieved Steam credentials for user ID: {}", userId);

        // 2. Fetch owned game App IDs from Steam using stored credentials
        List<String> steamAppIds = steamClientService.getOwnedGameAppIds(credentials.profileId(), credentials.apiKey());
        if (CollectionUtils.isEmpty(steamAppIds)) {
            log.warn("No owned games found or Steam API error for user ID: {}. Import process stopped.", userId);
            // Optionally, you might want to inform the user or throw a specific exception here.
            return;
        }
        log.info("Fetched {} App IDs from Steam for user ID: {}", steamAppIds.size(), userId);

        // 3. Fetch game details from IGDB using Steam App IDs
        // This might take time and involves external API calls with rate limiting and retries handled by IGDBClientService
        List<IGDBGameDTO> igdbGames = igdbClientService.searchGamesBySteamAppIds(steamAppIds);
        if (CollectionUtils.isEmpty(igdbGames)) {
            log.warn("No corresponding games found on IGDB for the fetched Steam App IDs for user ID: {}. Import process stopped.", userId);
            // Optionally, inform the user or handle this case.
            return;
        }
        log.info("Fetched {} game details from IGDB for user ID: {}", igdbGames.size(), userId);

        // 4. Retrieve the user's GameLibrary (should always exist)
        // Use findByUserWithGames to avoid lazy loading issues later
        GameLibrary library = gameLibraryRepository.findByUserWithGames(user)
                .orElseGet(() -> {
                    // This case should technically not happen if library is created on user registration
                    log.warn("GameLibrary not found for user ID: {}. Creating a new one.", userId);
                    GameLibrary newLibrary = new GameLibrary();
                    newLibrary.setUser(user);
                    return gameLibraryRepository.save(newLibrary); // Save immediately to ensure it exists
                });

        // Initialize the games set if it's null (though Fetch should handle this)
        if (library.getGames() == null) {
            library.setGames(new HashSet<>());
        }
        Set<Long> existingGameIdsInLibrary = library.getGames().stream()
                                                    .map(Game::getGameId)
                                                    .collect(Collectors.toSet());
        
        int gamesAddedCount = 0;
        // 5. Iterate, Upsert games locally, and add to library if new
        for (IGDBGameDTO igdbGame : igdbGames) {
            try {
                // Upsert game in local DB (ensures game exists and is up-to-date)
                Game game = gameSyncService.upsertGame(igdbGame);

                // Check if the game is already in the user's library
                if (!existingGameIdsInLibrary.contains(game.getGameId())) {
                    library.getGames().add(game);
                    existingGameIdsInLibrary.add(game.getGameId()); // Add to our tracking set
                    gamesAddedCount++;
                    log.debug("Added game '{}' (ID: {}) to library for user ID: {}", game.getTitle(), game.getGameId(), userId);
                } else {
                     log.trace("Game '{}' (ID: {}) already in library for user ID: {}. Skipping.", game.getTitle(), game.getGameId(), userId);
                }
            } catch (Exception e) {
                // Log error for specific game and continue with the rest
                log.error("Error processing game (IGDB ID: {}) for user ID: {}. Skipping this game. Error: {}",
                          igdbGame.getIgdbId(), userId, e.getMessage(), e);
            }
        }

        // 6. Save the library only once after adding all new games (if any were added)
        if (gamesAddedCount > 0) {
             gameLibraryRepository.save(library);
             log.info("Successfully added {} new games to the library for user ID: {}", gamesAddedCount, userId);
        } else {
             log.info("No new games were added to the library for user ID: {}. Library already up-to-date.", userId);
        }

        log.info("Steam library import finished for user ID: {}", userId);
    }
} 
