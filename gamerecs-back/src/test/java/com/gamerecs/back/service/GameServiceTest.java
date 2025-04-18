package com.gamerecs.back.service;

import com.gamerecs.back.model.Game;
import com.gamerecs.back.repository.GameRepository;
import com.gamerecs.back.util.StringNormalizer;
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
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private GameService gameService;

    private List<Game> sampleGames;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);
        
        // Set up sample games for testing
        sampleGames = new ArrayList<>();
        
        // Create Pokémon game
        Game pokemon = new Game();
        pokemon.setGameId(1L);
        pokemon.setIgdbId(1001L);
        pokemon.setTitle("Pokémon Red");
        pokemon.setDescription("The original Pokémon game");
        pokemon.setReleaseDate(LocalDate.of(1996, 2, 27));
        pokemon.setCoverImageUrl("http://example.com/pokemon.jpg");
        pokemon.setUpdatedAt(LocalDateTime.now());
        pokemon.setGenres(new HashSet<>());
        pokemon.setPlatforms(new HashSet<>());
        pokemon.setPublishers(new HashSet<>());
        pokemon.setDevelopers(new HashSet<>());
        sampleGames.add(pokemon);
        
        // Create Assassin's Creed game
        Game assassinsCreed = new Game();
        assassinsCreed.setGameId(2L);
        assassinsCreed.setIgdbId(1002L);
        assassinsCreed.setTitle("Assassin's Creed");
        assassinsCreed.setDescription("Action-adventure stealth game");
        assassinsCreed.setReleaseDate(LocalDate.of(2007, 11, 13));
        assassinsCreed.setCoverImageUrl("http://example.com/ac.jpg");
        assassinsCreed.setUpdatedAt(LocalDateTime.now());
        assassinsCreed.setGenres(new HashSet<>());
        assassinsCreed.setPlatforms(new HashSet<>());
        assassinsCreed.setPublishers(new HashSet<>());
        assassinsCreed.setDevelopers(new HashSet<>());
        sampleGames.add(assassinsCreed);
    }

    @Test
    void searchGamesByTitleNormalized_shouldReturnGames_whenQueryMatches() {
        // Arrange
        Page<Game> mockPage = new PageImpl<>(sampleGames, pageable, sampleGames.size());
        when(gameRepository.findByNormalizedTitleContaining(anyString(), any(Pageable.class)))
            .thenReturn(mockPage);

        // Act
        Page<Game> result = gameService.searchGamesByTitleNormalized("pokemon", 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals("Pokémon Red", result.getContent().get(0).getTitle());
    }

    @Test
    void searchGamesByTitleNormalized_shouldHandleAccents_whenQueryContainsAccents() {
        // Arrange
        Page<Game> mockPage = new PageImpl<>(List.of(sampleGames.get(0)), pageable, 1);
        String normalizedQuery = StringNormalizer.normalize("Pokémon");
        when(gameRepository.findByNormalizedTitleContaining(eq(normalizedQuery), any(Pageable.class)))
            .thenReturn(mockPage);

        // Act
        Page<Game> result = gameService.searchGamesByTitleNormalized("Pokémon", 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Pokémon Red", result.getContent().get(0).getTitle());
    }

    @Test
    void searchGamesByTitleNormalized_shouldHandlePunctuation_whenQueryContainsPunctuation() {
        // Arrange
        Page<Game> mockPage = new PageImpl<>(List.of(sampleGames.get(1)), pageable, 1);
        String normalizedQuery = StringNormalizer.normalize("Assassin's Creed");
        when(gameRepository.findByNormalizedTitleContaining(eq(normalizedQuery), any(Pageable.class)))
            .thenReturn(mockPage);

        // Act
        Page<Game> result = gameService.searchGamesByTitleNormalized("Assassin's Creed", 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Assassin's Creed", result.getContent().get(0).getTitle());
    }

    @Test
    void searchGamesByTitleNormalized_shouldHandleAlternativeQueries_whenQueryHasNoAccentsOrPunctuation() {
        // Arrange
        Page<Game> mockPage = new PageImpl<>(List.of(sampleGames.get(1)), pageable, 1);
        String normalizedQuery = StringNormalizer.normalize("assassins creed");
        when(gameRepository.findByNormalizedTitleContaining(eq(normalizedQuery), any(Pageable.class)))
            .thenReturn(mockPage);

        // Act
        Page<Game> result = gameService.searchGamesByTitleNormalized("assassins creed", 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Assassin's Creed", result.getContent().get(0).getTitle());
    }

    @Test
    void searchGamesByTitleNormalized_shouldThrowException_whenQueryIsTooShort() {
        // Act & Assert
        Exception exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.searchGamesByTitleNormalized("a", 0, 10);
        });
        
        assertTrue(exception.getMessage().contains("Search query must be at least 2 characters long"));
    }

    @Test
    void searchGamesByTitleNormalized_shouldThrowException_whenQueryIsNull() {
        // Act & Assert
        Exception exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.searchGamesByTitleNormalized(null, 0, 10);
        });
        
        assertTrue(exception.getMessage().contains("Search query must be at least 2 characters long"));
    }

    @Test
    void searchGamesByTitleNormalized_shouldThrowException_whenNormalizedQueryTooShort() {
        // Arrange - a query that becomes too short after normalization (e.g., just punctuation)
        String queryThatNormalizesToNothing = "''";
        
        // Act & Assert
        Exception exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.searchGamesByTitleNormalized(queryThatNormalizesToNothing, 0, 10);
        });
        
        assertTrue(exception.getMessage().contains("Search query must be at least 2 characters long after normalization"));
    }
} 
