package com.gamerecs.back.service;

import com.gamerecs.back.model.Game;
import com.gamerecs.back.model.GameLibrary;
import com.gamerecs.back.model.Genre;
import com.gamerecs.back.model.User;
import com.gamerecs.back.repository.GameLibraryRepository;
import com.gamerecs.back.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GameLibraryServiceTest {

    @Mock
    private GameLibraryRepository gameLibraryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GameLibraryService gameLibraryService;

    private User testUser;
    private GameLibrary testLibrary;
    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setUserId(userId);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        // Create test library
        testLibrary = new GameLibrary();
        testLibrary.setLibraryId(1L);
        testLibrary.setUser(testUser);
    }

    @Test
    void getLibraryForUser_ValidUser_ReturnsLibrary() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(gameLibraryRepository.findByUserWithGamesAndCollections(testUser)).thenReturn(Optional.of(testLibrary));

        // Act
        GameLibrary result = gameLibraryService.getLibraryForUser(userId);

        // Assert
        assertNotNull(result);
        assertEquals(testLibrary.getLibraryId(), result.getLibraryId());
        assertEquals(testUser, result.getUser());
        
        // Verify repository methods were called
        verify(userRepository, times(1)).findById(userId);
        verify(gameLibraryRepository, times(1)).findByUserWithGamesAndCollections(testUser);
    }

    @Test
    void getLibraryForUser_UserNotFound_ThrowsUnauthorized() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> gameLibraryService.getLibraryForUser(userId)
        );
        
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("User not found", exception.getReason());
        
        // Verify repository method was called
        verify(userRepository, times(1)).findById(userId);
        verify(gameLibraryRepository, never()).findByUser(any());
    }

    @Test
    void getLibraryForUser_LibraryNotFound_ThrowsNotFound() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(gameLibraryRepository.findByUserWithGamesAndCollections(testUser)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> gameLibraryService.getLibraryForUser(userId)
        );
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Game library not found", exception.getReason());
        
        // Verify repository methods were called
        verify(userRepository, times(1)).findById(userId);
        verify(gameLibraryRepository, times(1)).findByUserWithGamesAndCollections(testUser);
    }

    @Test
    void getLibraryForUser_WithSorting_ReturnsSortedLibrary() {
        // Arrange
        // Create test games with different titles
        Game game1 = new Game();
        game1.setGameId(1L);
        game1.setTitle("Zelda");
        
        Game game2 = new Game();
        game2.setGameId(2L);
        game2.setTitle("Assassin's Creed");
        
        // Add games to the library
        testLibrary.getGames().add(game1);
        testLibrary.getGames().add(game2);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(gameLibraryRepository.findByUserWithGamesAndCollections(testUser)).thenReturn(Optional.of(testLibrary));
        
        // Act
        GameLibrary result = gameLibraryService.getLibraryForUser(userId, "title", "");
        
        // Assert
        assertNotNull(result);
        assertEquals(testLibrary.getLibraryId(), result.getLibraryId());
        
        // Since HashSet doesn't maintain order, we need to check if both games are present
        assertEquals(2, result.getGames().size());
        assertTrue(result.getGames().stream().anyMatch(game -> "Assassin's Creed".equals(game.getTitle())));
        assertTrue(result.getGames().stream().anyMatch(game -> "Zelda".equals(game.getTitle())));
        
        // To verify sorting, we need to convert to a list and sort it ourselves
        List<String> actualTitles = result.getGames().stream()
            .map(Game::getTitle)
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .collect(java.util.stream.Collectors.toList());
        
        List<String> expectedTitles = List.of("Assassin's Creed", "Zelda");
        assertEquals(expectedTitles, actualTitles);
    }
    
    @Test
    void getLibraryForUser_WithFiltering_ReturnsFilteredLibrary() {
        // Arrange
        // Create test games with different genres
        Game actionGame = new Game();
        actionGame.setGameId(1L);
        actionGame.setTitle("Action Game");
        
        Genre actionGenre = new Genre();
        actionGenre.setGenreId(1L);
        actionGenre.setName("Action");
        
        Game rpgGame = new Game();
        rpgGame.setGameId(2L);
        rpgGame.setTitle("RPG Game");
        
        Genre rpgGenre = new Genre();
        rpgGenre.setGenreId(2L);
        rpgGenre.setName("RPG");
        
        // Set up genres for games
        actionGame.setGenres(new LinkedHashSet<>(Collections.singletonList(actionGenre)));
        rpgGame.setGenres(new LinkedHashSet<>(Collections.singletonList(rpgGenre)));
        
        // Add games to the library
        testLibrary.getGames().add(actionGame);
        testLibrary.getGames().add(rpgGame);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(gameLibraryRepository.findByUserWithGamesAndCollections(testUser)).thenReturn(Optional.of(testLibrary));
        
        // Act
        GameLibrary result = gameLibraryService.getLibraryForUser(userId, "title", "Action");
        
        // Assert
        assertNotNull(result);
        assertEquals(testLibrary.getLibraryId(), result.getLibraryId());
        assertEquals(1, result.getGames().size());
        
        // Check if only action games are included
        Game filteredGame = result.getGames().iterator().next();
        assertEquals("Action Game", filteredGame.getTitle());
    }
    
    @Test
    void getLibraryForUser_WithSortingAndFiltering_ReturnsSortedAndFilteredLibrary() {
        // Arrange: Create a User with a game library that has a mix of games
        GameLibrary library = new GameLibrary();
        library.setLibraryId(2L);
        library.setUser(testUser);
        Set<Game> games = new HashSet<>();
        
        // Create games with different titles and genres
        Game game1 = new Game();
        game1.setGameId(1L);
        game1.setTitle("Zelda");
        Genre adventureGenre = new Genre();
        adventureGenre.setGenreId(1L);
        adventureGenre.setName("Adventure");
        game1.setGenres(new LinkedHashSet<>(Collections.singletonList(adventureGenre)));
        
        Game game2 = new Game();
        game2.setGameId(2L);
        game2.setTitle("Action Game");
        Genre actionGenre = new Genre();
        actionGenre.setGenreId(2L);
        actionGenre.setName("Action");
        game2.setGenres(new LinkedHashSet<>(Collections.singletonList(actionGenre)));
        
        Game game3 = new Game();
        game3.setGameId(3L);
        game3.setTitle("Mario");
        game3.setGenres(new LinkedHashSet<>(Collections.singletonList(adventureGenre)));
        
        // Add another Action game with a different title to test sorting
        Game game4 = new Game();
        game4.setGameId(4L);
        game4.setTitle("Battlefield");
        game4.setGenres(new LinkedHashSet<>(Collections.singletonList(actionGenre)));
        
        games.add(game1);
        games.add(game2);
        games.add(game3);
        games.add(game4);
        library.setGames(games);
        
        // Stub repository calls
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(gameLibraryRepository.findByUserWithGamesAndCollections(testUser)).thenReturn(Optional.of(library));
        
        // Act: request library sorted by title and filtered to only Action games
        GameLibrary result = gameLibraryService.getLibraryForUser(userId, "title", "Action");
        
        // Assert: The returned games should be sorted by title and only contain the Action genre games
        assertEquals(2, result.getGames().size());
        
        // Convert to list to verify order
        List<Game> sortedGames = new ArrayList<>(result.getGames());
        assertEquals("Action Game", sortedGames.get(0).getTitle());
        assertEquals("Battlefield", sortedGames.get(1).getTitle());
        
        // Verify all games have the Action genre
        for (Game game : result.getGames()) {
            boolean hasActionGenre = game.getGenres().stream()
                .anyMatch(genre -> "Action".equals(genre.getName()));
            assertTrue(hasActionGenre, "Game should have Action genre: " + game.getTitle());
        }
    }
} 
