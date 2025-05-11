package com.gamerecs.back.service;

import com.gamerecs.back.dto.IGDBGameDTO;
import com.gamerecs.back.exception.UserNotFoundException;
import com.gamerecs.back.model.Game;
import com.gamerecs.back.model.GameLibrary;
import com.gamerecs.back.model.User;
import com.gamerecs.back.repository.GameLibraryRepository;
import com.gamerecs.back.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SteamImportServiceTest {

    @Mock
    private SteamClientService steamClientService;
    @Mock
    private IGDBClientService igdbClientService;
    @Mock
    private GameSyncService gameSyncService;
    @Mock
    private GameLibraryRepository gameLibraryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;

    @InjectMocks
    private SteamImportService steamImportService;

    private User testUser;
    private SteamCredentials testCredentials;
    private GameLibrary testGameLibrary;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUsername("testuser");

        testCredentials = new SteamCredentials("steamProfileId", "steamApiKey");

        testGameLibrary = new GameLibrary();
        testGameLibrary.setLibraryId(1L);
        testGameLibrary.setUser(testUser);
        testGameLibrary.setGames(new HashSet<>());
    }

    @Test
    void testImportSteamLibrary_UserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            steamImportService.importSteamLibrary(1L);
        });

        assertEquals("User not found with ID: 1", exception.getMessage());
        verify(userService, never()).getDecryptedSteamCredentials(anyLong());
    }

    @Test
    void testImportSteamLibrary_SteamCredentialsNotConfigured() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userService.getDecryptedSteamCredentials(1L)).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            steamImportService.importSteamLibrary(1L);
        });

        assertEquals("Steam credentials not configured for this user.", exception.getMessage());
        verify(steamClientService, never()).getOwnedGameAppIds(anyString(), anyString());
    }

    @Test
    void testImportSteamLibrary_NoOwnedGamesFoundOnSteam() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userService.getDecryptedSteamCredentials(1L)).thenReturn(Optional.of(testCredentials));
        when(steamClientService.getOwnedGameAppIds(testCredentials.profileId(), testCredentials.apiKey())).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> steamImportService.importSteamLibrary(1L));

        verify(igdbClientService, never()).searchGamesBySteamAppIds(anyList());
        verify(gameLibraryRepository, never()).save(any(GameLibrary.class));
    }

    @Test
    void testImportSteamLibrary_NoGamesFoundOnIGDB() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userService.getDecryptedSteamCredentials(1L)).thenReturn(Optional.of(testCredentials));
        when(steamClientService.getOwnedGameAppIds(testCredentials.profileId(), testCredentials.apiKey())).thenReturn(List.of("app1"));
        when(igdbClientService.searchGamesBySteamAppIds(List.of("app1"))).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> steamImportService.importSteamLibrary(1L));

        verify(gameLibraryRepository, never()).findByUserWithGames(any(User.class)); // Should not proceed to library operations
        verify(gameLibraryRepository, never()).save(any(GameLibrary.class));
    }

    @Test
    void testImportSteamLibrary_GameLibraryNotFound_CreatesNewAndSaves() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userService.getDecryptedSteamCredentials(1L)).thenReturn(Optional.of(testCredentials));
        when(steamClientService.getOwnedGameAppIds(testCredentials.profileId(), testCredentials.apiKey())).thenReturn(List.of("app1"));
        IGDBGameDTO igdbGameDTO = new IGDBGameDTO();
        igdbGameDTO.setIgdbId(123L);
        igdbGameDTO.setTitle("Test Game");
        when(igdbClientService.searchGamesBySteamAppIds(List.of("app1"))).thenReturn(List.of(igdbGameDTO));
        
        Game newGame = new Game();
        newGame.setGameId(1L);
        newGame.setTitle("Test Game");
        when(gameSyncService.upsertGame(igdbGameDTO)).thenReturn(newGame);

        when(gameLibraryRepository.findByUserWithGames(testUser)).thenReturn(Optional.empty());

        java.util.concurrent.atomic.AtomicInteger saveCallCount = new java.util.concurrent.atomic.AtomicInteger(0);
        // To store the state of the library as captured during the second save call for final assertions.
        java.util.concurrent.atomic.AtomicReference<GameLibrary> capturedFinalLibraryState = new java.util.concurrent.atomic.AtomicReference<>();

        when(gameLibraryRepository.save(any(GameLibrary.class))).thenAnswer(invocation -> {
            GameLibrary libraryBeingSaved = invocation.getArgument(0);
            int callNum = saveCallCount.incrementAndGet();

            if (callNum == 1) { // First save call (from orElseGet, creating the new library)
                assertNotNull(libraryBeingSaved.getUser(), "User must be set on the new library at first save.");
                assertEquals(testUser, libraryBeingSaved.getUser(), "User should match testUser at first save.");
                assertNotNull(libraryBeingSaved.getGames(), "Games set should be initialized (e.g., by constructor) at first save.");
                assertTrue(libraryBeingSaved.getGames().isEmpty(), "Newly created library should have no games at the point of its first save.");
                assertNull(libraryBeingSaved.getLibraryId(), "Library ID should be null before repository assigns it on first save.");
                
                // Simulate repository assigning an ID upon saving a new entity
                libraryBeingSaved.setLibraryId(2L); 
            } else if (callNum == 2) { // Second save call (after games are added to the library)
                assertNotNull(libraryBeingSaved.getGames(), "Games set should exist at second save.");
                assertFalse(libraryBeingSaved.getGames().isEmpty(), "Library should have games on second save.");
                assertTrue(libraryBeingSaved.getGames().contains(newGame), "Library should contain the newly added game on second save.");
                assertEquals(1, libraryBeingSaved.getGames().size(), "Only one new game should be in the library at second save for this test case.");
                assertEquals(Long.valueOf(2L), libraryBeingSaved.getLibraryId(), "Library ID (assigned during first save) should persist for the second save.");
                
                // Capture the state for later verification if needed, though assertions here are primary.
                // Create a defensive copy if necessary, but returning the same instance is fine if the service expects it.
                GameLibrary copiedState = new GameLibrary();
                copiedState.setLibraryId(libraryBeingSaved.getLibraryId());
                copiedState.setUser(libraryBeingSaved.getUser());
                copiedState.setGames(new HashSet<>(libraryBeingSaved.getGames()));
                capturedFinalLibraryState.set(copiedState);
            } else {
                fail("gameLibraryRepository.save() was called more than expected times.");
            }
            return libraryBeingSaved; // Return the instance; the service reuses it.
        });

        assertDoesNotThrow(() -> steamImportService.importSteamLibrary(1L));

        verify(gameLibraryRepository, times(2)).save(any(GameLibrary.class));
        assertEquals(2, saveCallCount.get(), "Save method should have been invoked exactly twice.");
        
        GameLibrary finalLibrary = capturedFinalLibraryState.get();
        assertNotNull(finalLibrary, "Final library state should have been captured from the second save.");
        assertNotNull(finalLibrary.getGames());
        assertTrue(finalLibrary.getGames().contains(newGame), "Asserting on captured final state: library should contain the new game.");
        assertEquals(1, finalLibrary.getGames().size(), "Asserting on captured final state: game count should be 1.");
        assertEquals(Long.valueOf(2L), finalLibrary.getLibraryId(), "Asserting on captured final state: library ID should be 2L.");
    }

    @Test
    void testImportSteamLibrary_LibraryGamesIsNull_InitializesSet() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userService.getDecryptedSteamCredentials(1L)).thenReturn(Optional.of(testCredentials));
        when(steamClientService.getOwnedGameAppIds(testCredentials.profileId(), testCredentials.apiKey())).thenReturn(List.of("app1"));
        IGDBGameDTO igdbGameDTO = new IGDBGameDTO();
        igdbGameDTO.setIgdbId(123L); 
        igdbGameDTO.setTitle("Game 1");
        when(igdbClientService.searchGamesBySteamAppIds(List.of("app1"))).thenReturn(List.of(igdbGameDTO));

        // Critical part: make the library's game set null initially
        testGameLibrary.setGames(null);
        when(gameLibraryRepository.findByUserWithGames(testUser)).thenReturn(Optional.of(testGameLibrary));

        Game game1 = new Game(); game1.setGameId(1L); game1.setTitle("Game 1");
        when(gameSyncService.upsertGame(igdbGameDTO)).thenReturn(game1);

        assertDoesNotThrow(() -> steamImportService.importSteamLibrary(1L));

        verify(gameLibraryRepository).save(testGameLibrary);
        assertNotNull(testGameLibrary.getGames(), "Games set should be initialized.");
        assertTrue(testGameLibrary.getGames().contains(game1), "Game should be added to the initialized set.");
    }


    @Test
    void testImportSteamLibrary_AllGamesAlreadyInLibrary() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userService.getDecryptedSteamCredentials(1L)).thenReturn(Optional.of(testCredentials));
        when(steamClientService.getOwnedGameAppIds(testCredentials.profileId(), testCredentials.apiKey())).thenReturn(List.of("app1"));
        
        IGDBGameDTO igdbGameDTO1 = new IGDBGameDTO(); 
igdbGameDTO1.setIgdbId(101L); igdbGameDTO1.setTitle("Existing Game 1");
        when(igdbClientService.searchGamesBySteamAppIds(List.of("app1"))).thenReturn(List.of(igdbGameDTO1));

        Game existingGame1 = new Game(); existingGame1.setGameId(1L); existingGame1.setTitle("Existing Game 1");
        testGameLibrary.getGames().add(existingGame1); // Game already in library
        when(gameLibraryRepository.findByUserWithGames(testUser)).thenReturn(Optional.of(testGameLibrary));
        when(gameSyncService.upsertGame(igdbGameDTO1)).thenReturn(existingGame1);

        assertDoesNotThrow(() -> steamImportService.importSteamLibrary(1L));

        verify(gameLibraryRepository, never()).save(testGameLibrary); // No save if no games added
        assertEquals(1, testGameLibrary.getGames().size());
    }

    @Test
    void testImportSteamLibrary_SuccessfulImport_AddsNewGames() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userService.getDecryptedSteamCredentials(1L)).thenReturn(Optional.of(testCredentials));
        when(steamClientService.getOwnedGameAppIds(testCredentials.profileId(), testCredentials.apiKey())).thenReturn(List.of("app1", "app2"));

        IGDBGameDTO igdbGameDTO1 = new IGDBGameDTO(); igdbGameDTO1.setIgdbId(101L); igdbGameDTO1.setTitle("New Game 1");
        IGDBGameDTO igdbGameDTO2 = new IGDBGameDTO(); igdbGameDTO2.setIgdbId(102L); igdbGameDTO2.setTitle("New Game 2");
        when(igdbClientService.searchGamesBySteamAppIds(List.of("app1", "app2"))).thenReturn(List.of(igdbGameDTO1, igdbGameDTO2));

        when(gameLibraryRepository.findByUserWithGames(testUser)).thenReturn(Optional.of(testGameLibrary));

        Game newGame1 = new Game(); newGame1.setGameId(1L); newGame1.setTitle("New Game 1");
        Game newGame2 = new Game(); newGame2.setGameId(2L); newGame2.setTitle("New Game 2");
        when(gameSyncService.upsertGame(igdbGameDTO1)).thenReturn(newGame1);
        when(gameSyncService.upsertGame(igdbGameDTO2)).thenReturn(newGame2);

        assertDoesNotThrow(() -> steamImportService.importSteamLibrary(1L));

        verify(gameLibraryRepository).save(testGameLibrary);
        assertEquals(2, testGameLibrary.getGames().size());
        assertTrue(testGameLibrary.getGames().contains(newGame1));
        assertTrue(testGameLibrary.getGames().contains(newGame2));
    }
    
    @Test
    void testImportSteamLibrary_SuccessfulImport_SomeNewSomeExisting() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userService.getDecryptedSteamCredentials(1L)).thenReturn(Optional.of(testCredentials));
        when(steamClientService.getOwnedGameAppIds(testCredentials.profileId(), testCredentials.apiKey())).thenReturn(List.of("app1", "app2", "app3"));

        IGDBGameDTO igdbGameDTO1 = new IGDBGameDTO(); igdbGameDTO1.setIgdbId(101L); igdbGameDTO1.setTitle("Existing Game 1");
        IGDBGameDTO igdbGameDTO2 = new IGDBGameDTO(); igdbGameDTO2.setIgdbId(102L); igdbGameDTO2.setTitle("New Game 1");
        IGDBGameDTO igdbGameDTO3 = new IGDBGameDTO(); igdbGameDTO3.setIgdbId(103L); igdbGameDTO3.setTitle("New Game 2");
        when(igdbClientService.searchGamesBySteamAppIds(List.of("app1", "app2", "app3"))).thenReturn(List.of(igdbGameDTO1, igdbGameDTO2, igdbGameDTO3));

        Game existingGame = new Game(); existingGame.setGameId(1L); existingGame.setTitle("Existing Game 1");
        testGameLibrary.getGames().add(existingGame);
        when(gameLibraryRepository.findByUserWithGames(testUser)).thenReturn(Optional.of(testGameLibrary));

        Game newGame1 = new Game(); newGame1.setGameId(2L); newGame1.setTitle("New Game 1");
        Game newGame2 = new Game(); newGame2.setGameId(3L); newGame2.setTitle("New Game 2");
        when(gameSyncService.upsertGame(igdbGameDTO1)).thenReturn(existingGame); // Corresponds to existing
        when(gameSyncService.upsertGame(igdbGameDTO2)).thenReturn(newGame1);       // Corresponds to new
        when(gameSyncService.upsertGame(igdbGameDTO3)).thenReturn(newGame2);       // Corresponds to new

        assertDoesNotThrow(() -> steamImportService.importSteamLibrary(1L));

        verify(gameLibraryRepository).save(testGameLibrary);
        assertEquals(3, testGameLibrary.getGames().size());
        assertTrue(testGameLibrary.getGames().contains(existingGame));
        assertTrue(testGameLibrary.getGames().contains(newGame1));
        assertTrue(testGameLibrary.getGames().contains(newGame2));
    }

    @Test
    void testImportSteamLibrary_ErrorDuringGameProcessing_ContinuesAndLogs() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userService.getDecryptedSteamCredentials(1L)).thenReturn(Optional.of(testCredentials));
        when(steamClientService.getOwnedGameAppIds(testCredentials.profileId(), testCredentials.apiKey())).thenReturn(List.of("app1", "appError", "app3"));

        IGDBGameDTO igdbGameDTO1 = new IGDBGameDTO(); igdbGameDTO1.setIgdbId(101L); igdbGameDTO1.setTitle("Good Game 1");
        IGDBGameDTO igdbGameDTOError = new IGDBGameDTO(); igdbGameDTOError.setIgdbId(666L); igdbGameDTOError.setTitle("Error Game");
        IGDBGameDTO igdbGameDTO3 = new IGDBGameDTO(); igdbGameDTO3.setIgdbId(103L); igdbGameDTO3.setTitle("Good Game 3");
        when(igdbClientService.searchGamesBySteamAppIds(anyList())).thenReturn(List.of(igdbGameDTO1, igdbGameDTOError, igdbGameDTO3));

        when(gameLibraryRepository.findByUserWithGames(testUser)).thenReturn(Optional.of(testGameLibrary));

        Game goodGame1 = new Game(); goodGame1.setGameId(1L); goodGame1.setTitle("Good Game 1");
        Game goodGame3 = new Game(); goodGame3.setGameId(3L); goodGame3.setTitle("Good Game 3");

        when(gameSyncService.upsertGame(igdbGameDTO1)).thenReturn(goodGame1);
        when(gameSyncService.upsertGame(igdbGameDTOError)).thenThrow(new RuntimeException("Simulated DB error for game 666"));
        when(gameSyncService.upsertGame(igdbGameDTO3)).thenReturn(goodGame3);

        assertDoesNotThrow(() -> steamImportService.importSteamLibrary(1L));

        verify(gameLibraryRepository).save(testGameLibrary);
        assertEquals(2, testGameLibrary.getGames().size(), "Should only contain the two good games");
        assertTrue(testGameLibrary.getGames().contains(goodGame1));
        assertFalse(testGameLibrary.getGames().stream().anyMatch(g -> g.getTitle().equals("Error Game")));
        assertTrue(testGameLibrary.getGames().contains(goodGame3));
        // Also verify logging for the error, but that requires a logging framework testing utility or capturing logs.
        // For now, focus on behavior.
    }
} 
