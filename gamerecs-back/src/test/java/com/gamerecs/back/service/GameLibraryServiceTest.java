package com.gamerecs.back.service;

import com.gamerecs.back.model.*;
import com.gamerecs.back.repository.GameLibraryRepository;
import com.gamerecs.back.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
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
    private Game game1;
    private Game game2;
    private Genre actionGenre;
    private Genre rpgGenre;
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
        
        // Create test games
        game1 = new Game();
        game1.setGameId(1L);
        game1.setTitle("Zelda");
        game1.setReleaseDate(LocalDate.of(2017, 3, 3));
        
        game2 = new Game();
        game2.setGameId(2L);
        game2.setTitle("Assassin's Creed");
        game2.setReleaseDate(LocalDate.of(2007, 11, 13));
        
        // Create test genres
        actionGenre = new Genre();
        actionGenre.setGenreId(1L);
        actionGenre.setName("Action");
        
        rpgGenre = new Genre();
        rpgGenre.setGenreId(2L);
        rpgGenre.setName("RPG");
        
        // Set up genres for games
        game1.setGenres(new LinkedHashSet<>(Collections.singletonList(rpgGenre)));
        game2.setGenres(new LinkedHashSet<>(Collections.singletonList(actionGenre)));
        
        // Initialize empty collections for both games to avoid NPEs
        game1.setPlatforms(new LinkedHashSet<>());
        game1.setPublishers(new LinkedHashSet<>());
        game1.setDevelopers(new LinkedHashSet<>());
        
        game2.setPlatforms(new LinkedHashSet<>());
        game2.setPublishers(new LinkedHashSet<>());
        game2.setDevelopers(new LinkedHashSet<>());
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
        verify(gameLibraryRepository, never()).findByUserWithGamesAndCollections(any());
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
    void getLibraryForUser_WithSortingByTitle_ReturnsSortedLibrary() {
        // Arrange
        // Add games to the library
        Set<Game> games = new LinkedHashSet<>();
        games.add(game1);
        games.add(game2);
        testLibrary.setGames(games);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(gameLibraryRepository.findByUserWithGamesAndCollections(testUser)).thenReturn(Optional.of(testLibrary));
        
        // Act
        GameLibrary result = gameLibraryService.getLibraryForUser(userId, "title", "");
        
        // Assert
        assertNotNull(result);
        assertEquals(testLibrary.getLibraryId(), result.getLibraryId());
        assertEquals(2, result.getGames().size());
        
        // Check sorting by converting to a list
        List<Game> sortedGames = new ArrayList<>(result.getGames());
        assertEquals("Assassin's Creed", sortedGames.get(0).getTitle());
        assertEquals("Zelda", sortedGames.get(1).getTitle());
    }

    @Test
    void getLibraryForUser_WithSortingByReleaseDate_ReturnsSortedLibrary() {
        // Arrange
        // Add games to the library
        Set<Game> games = new LinkedHashSet<>();
        games.add(game1);
        games.add(game2);
        testLibrary.setGames(games);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(gameLibraryRepository.findByUserWithGamesAndCollections(testUser)).thenReturn(Optional.of(testLibrary));
        
        // Act
        GameLibrary result = gameLibraryService.getLibraryForUser(userId, "releasedate", "");
        
        // Assert
        assertNotNull(result);
        assertEquals(testLibrary.getLibraryId(), result.getLibraryId());
        assertEquals(2, result.getGames().size());
        
        // Check sorting by converting to a list
        List<Game> sortedGames = new ArrayList<>(result.getGames());
        assertEquals("Assassin's Creed", sortedGames.get(0).getTitle());
        assertEquals("Zelda", sortedGames.get(1).getTitle());
    }
    
    @Test
    void getLibraryForUser_WithGenreFiltering_ReturnsFilteredLibrary() {
        // Arrange
        // Add games to the library
        Set<Game> games = new LinkedHashSet<>();
        games.add(game1);
        games.add(game2);
        testLibrary.setGames(games);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(gameLibraryRepository.findByUserWithGamesAndCollections(testUser)).thenReturn(Optional.of(testLibrary));
        
        // Act
        GameLibrary result = gameLibraryService.getLibraryForUser(userId, "title", "RPG");
        
        // Assert
        assertNotNull(result);
        assertEquals(testLibrary.getLibraryId(), result.getLibraryId());
        assertEquals(1, result.getGames().size());
        
        // Check filtering result
        Game filteredGame = result.getGames().iterator().next();
        assertEquals("Zelda", filteredGame.getTitle());
    }
    
    @Test
    void getLibraryForUser_WithSortingAndFiltering_ReturnsSortedAndFilteredLibrary() {
        // Arrange
        // Create a third game with the same genre as game1 but different title
        Game game3 = new Game();
        game3.setGameId(3L);
        game3.setTitle("Final Fantasy");
        game3.setReleaseDate(LocalDate.of(1997, 1, 31));
        game3.setGenres(new LinkedHashSet<>(Collections.singletonList(rpgGenre)));
        game3.setPlatforms(new LinkedHashSet<>());
        game3.setPublishers(new LinkedHashSet<>());
        game3.setDevelopers(new LinkedHashSet<>());
        
        // Add games to the library
        Set<Game> games = new LinkedHashSet<>();
        games.add(game1);
        games.add(game2);
        games.add(game3);
        testLibrary.setGames(games);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(gameLibraryRepository.findByUserWithGamesAndCollections(testUser)).thenReturn(Optional.of(testLibrary));
        
        // Act: request library sorted by title and filtered to only RPG games
        GameLibrary result = gameLibraryService.getLibraryForUser(userId, "title", "RPG");
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.getGames().size());
        
        // Check sorting and filtering
        List<Game> sortedGames = new ArrayList<>(result.getGames());
        assertEquals("Final Fantasy", sortedGames.get(0).getTitle());
        assertEquals("Zelda", sortedGames.get(1).getTitle());
    }

    @Test
    void getPaginatedLibraryForUser_UserNotFound_ThrowsUnauthorized() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        
        // Act & Assert
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> gameLibraryService.getPaginatedLibraryForUser(userId, "title", "", 0, 10)
        );
        
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("User not found", exception.getReason());
        
        // Verify repository method was called
        verify(userRepository, times(1)).findById(userId);
        verifyNoMoreInteractions(gameLibraryRepository);
    }
    
    @Test
    void getPaginatedLibraryForUser_LibraryNotFound_ThrowsNotFound() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(gameLibraryRepository.findByUser(testUser)).thenReturn(Optional.empty());
        
        // Create a page of games
        List<Game> games = List.of(game1, game2);
        Page<Game> gamesPage = new PageImpl<>(games, PageRequest.of(0, 10), games.size());
        
        when(gameLibraryRepository.findGamesByUserOrderByTitle(eq(testUser), any(Pageable.class)))
            .thenReturn(gamesPage);
        
        // Act & Assert
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> gameLibraryService.getPaginatedLibraryForUser(userId, "title", "", 0, 10)
        );
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Game library not found", exception.getReason());
        
        // Verify repository methods were called
        verify(userRepository, times(1)).findById(userId);
        verify(gameLibraryRepository, times(1)).findGamesByUserOrderByTitle(eq(testUser), any(Pageable.class));
        verify(gameLibraryRepository, times(1)).findByUser(testUser);
    }
    
    @Test
    void getPaginatedLibraryForUser_SortByTitle_NoFilter_ReturnsPaginated() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        
        // Create a page of games
        List<Game> games = List.of(game1, game2);
        Page<Game> gamesPage = new PageImpl<>(games, PageRequest.of(0, 10), games.size());
        
        when(gameLibraryRepository.findGamesByUserOrderByTitle(eq(testUser), any(Pageable.class)))
            .thenReturn(gamesPage);
        when(gameLibraryRepository.findByUser(testUser)).thenReturn(Optional.of(testLibrary));
        
        // Act
        PaginatedGameLibraryResponse result = gameLibraryService.getPaginatedLibraryForUser(
            userId, "title", "", 0, 10);
        
        // Assert
        assertNotNull(result);
        assertEquals(testLibrary.getLibraryId(), result.getLibraryId());
        assertEquals(2, result.getGames().size());
        assertEquals(0, result.getCurrentPage());
        assertEquals(1, result.getTotalPages());
        assertEquals(2, result.getTotalElements());
        assertEquals(10, result.getPageSize());
        
        // Verify repository methods were called
        verify(userRepository, times(1)).findById(userId);
        verify(gameLibraryRepository, times(1)).findGamesByUserOrderByTitle(eq(testUser), any(Pageable.class));
        verify(gameLibraryRepository, times(1)).findByUser(testUser);
    }
    
    @Test
    void getPaginatedLibraryForUser_SortByReleaseDate_NoFilter_ReturnsPaginated() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        
        // Create a page of games
        List<Game> games = List.of(game2, game1);  // Ordered by release date
        Page<Game> gamesPage = new PageImpl<>(games, PageRequest.of(0, 10), games.size());
        
        when(gameLibraryRepository.findGamesByUserOrderByReleaseDate(eq(testUser), any(Pageable.class)))
            .thenReturn(gamesPage);
        when(gameLibraryRepository.findByUser(testUser)).thenReturn(Optional.of(testLibrary));
        
        // Act
        PaginatedGameLibraryResponse result = gameLibraryService.getPaginatedLibraryForUser(
            userId, "releasedate", "", 0, 10);
        
        // Assert
        assertNotNull(result);
        assertEquals(testLibrary.getLibraryId(), result.getLibraryId());
        assertEquals(2, result.getGames().size());
        assertEquals("Assassin's Creed", result.getGames().get(0).getTitle());
        assertEquals("Zelda", result.getGames().get(1).getTitle());
        
        // Verify repository methods were called
        verify(userRepository, times(1)).findById(userId);
        verify(gameLibraryRepository, times(1)).findGamesByUserOrderByReleaseDate(eq(testUser), any(Pageable.class));
        verify(gameLibraryRepository, times(1)).findByUser(testUser);
    }
    
    @Test
    void getPaginatedLibraryForUser_SortByTitle_WithGenreFilter_ReturnsPaginated() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        
        // Create a page of filtered games
        List<Game> filteredGames = List.of(game1);  // Only RPG game
        Page<Game> gamesPage = new PageImpl<>(filteredGames, PageRequest.of(0, 10), filteredGames.size());
        
        when(gameLibraryRepository.findGamesByUserAndGenreOrderByTitle(eq(testUser), eq("RPG"), any(Pageable.class)))
            .thenReturn(gamesPage);
        when(gameLibraryRepository.findByUser(testUser)).thenReturn(Optional.of(testLibrary));
        
        // Act
        PaginatedGameLibraryResponse result = gameLibraryService.getPaginatedLibraryForUser(
            userId, "title", "RPG", 0, 10);
        
        // Assert
        assertNotNull(result);
        assertEquals(testLibrary.getLibraryId(), result.getLibraryId());
        assertEquals(1, result.getGames().size());
        assertEquals("Zelda", result.getGames().get(0).getTitle());
        
        // Verify repository methods were called
        verify(userRepository, times(1)).findById(userId);
        verify(gameLibraryRepository, times(1)).findGamesByUserAndGenreOrderByTitle(eq(testUser), eq("RPG"), any(Pageable.class));
        verify(gameLibraryRepository, times(1)).findByUser(testUser);
    }
    
    @Test
    void getPaginatedLibraryForUser_SortByReleaseDate_WithGenreFilter_ReturnsPaginated() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        
        // Create a page of filtered games
        List<Game> filteredGames = List.of(game2);  // Only Action game
        Page<Game> gamesPage = new PageImpl<>(filteredGames, PageRequest.of(0, 10), filteredGames.size());
        
        when(gameLibraryRepository.findGamesByUserAndGenreOrderByReleaseDate(eq(testUser), eq("Action"), any(Pageable.class)))
            .thenReturn(gamesPage);
        when(gameLibraryRepository.findByUser(testUser)).thenReturn(Optional.of(testLibrary));
        
        // Act
        PaginatedGameLibraryResponse result = gameLibraryService.getPaginatedLibraryForUser(
            userId, "releasedate", "Action", 0, 10);
        
        // Assert
        assertNotNull(result);
        assertEquals(testLibrary.getLibraryId(), result.getLibraryId());
        assertEquals(1, result.getGames().size());
        assertEquals("Assassin's Creed", result.getGames().get(0).getTitle());
        
        // Verify repository methods were called
        verify(userRepository, times(1)).findById(userId);
        verify(gameLibraryRepository, times(1)).findGamesByUserAndGenreOrderByReleaseDate(eq(testUser), eq("Action"), any(Pageable.class));
        verify(gameLibraryRepository, times(1)).findByUser(testUser);
    }
    
    @Test
    void getPaginatedLibraryForUser_RepositoryException_ThrowsInternalServerError() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(gameLibraryRepository.findGamesByUserOrderByTitle(eq(testUser), any(Pageable.class)))
            .thenThrow(new RuntimeException("Database error"));
        
        // Act & Assert
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> gameLibraryService.getPaginatedLibraryForUser(userId, "title", "", 0, 10)
        );
        
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Error retrieving paginated game library"));
        
        // Verify repository methods were called
        verify(userRepository, times(1)).findById(userId);
        verify(gameLibraryRepository, times(1)).findGamesByUserOrderByTitle(eq(testUser), any(Pageable.class));
    }
} 
