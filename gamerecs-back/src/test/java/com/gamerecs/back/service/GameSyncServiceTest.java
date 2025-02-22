package com.gamerecs.back.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.gamerecs.back.dto.IGDBGameDTO;
import com.gamerecs.back.model.Game;
import com.gamerecs.back.repository.DeveloperRepository;
import com.gamerecs.back.repository.GameRepository;
import com.gamerecs.back.repository.PublisherRepository;
import com.gamerecs.back.repository.GenreRepository;
import com.gamerecs.back.repository.PlatformRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class GameSyncServiceTest {

    @Mock
    private GameRepository gameRepository;
    
    @Mock
    private PublisherRepository publisherRepository;
    
    @Mock
    private DeveloperRepository developerRepository;
    
    @Mock
    private GenreRepository genreRepository;
    
    @Mock
    private PlatformRepository platformRepository;
    
    @Mock
    private IGDBClientService igdbClientService;
    
    private GameSyncService gameSyncService;
    
    @BeforeEach
    void setUp() {
        gameSyncService = new GameSyncService(
            gameRepository, 
            publisherRepository, 
            developerRepository, 
            genreRepository,
            platformRepository,
            igdbClientService
        );
    }

    @Test
    void testUpdateGameRecordWhenUpdatedAtIsNewer() {
        // Arrange
        IGDBGameDTO incomingGame = new IGDBGameDTO();
        incomingGame.setIgdbId(1L);
        incomingGame.setTitle("Test Game");
        // Set updated_at to a newer timestamp (current time)
        long currentTime = Instant.now().getEpochSecond();
        incomingGame.setUpdatedAt(currentTime);
        
        Game existingGame = new Game();
        existingGame.setIgdbId(1L);
        existingGame.setTitle("Test Game");
        // Set existing game's updated_at to 1 hour ago
        existingGame.setUpdatedAt(
            Instant.ofEpochSecond(currentTime - 3600)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
        );
        
        when(gameRepository.findByIgdbId(1L)).thenReturn(Optional.of(existingGame));
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        Game updatedGame = gameSyncService.upsertGame(incomingGame);
        
        // Assert
        verify(gameRepository, times(2)).save(any(Game.class)); // Once for initial save, once after relationships
        assertEquals(
            Instant.ofEpochSecond(currentTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime(),
            updatedGame.getUpdatedAt()
        );
    }

    @Test
    void testSkipUpdateWhenExistingGameIsNewer() {
        // Arrange
        IGDBGameDTO incomingGame = new IGDBGameDTO();
        incomingGame.setIgdbId(1L);
        incomingGame.setTitle("Test Game");
        // Set updated_at to an older timestamp
        long oldTime = Instant.now().minusSeconds(3600).getEpochSecond();
        incomingGame.setUpdatedAt(oldTime);
        
        Game existingGame = new Game();
        existingGame.setIgdbId(1L);
        existingGame.setTitle("Test Game");
        // Set existing game's updated_at to current time
        existingGame.setUpdatedAt(LocalDateTime.now());
        
        when(gameRepository.findByIgdbId(1L)).thenReturn(Optional.of(existingGame));
        
        // Act
        Game result = gameSyncService.upsertGame(incomingGame);
        
        // Assert
        verify(gameRepository, never()).save(any(Game.class));
        assertEquals(existingGame.getUpdatedAt(), result.getUpdatedAt());
    }

    @Test
    void testSyncGamesFromSearch() {
        // Arrange
        IGDBGameDTO game1 = new IGDBGameDTO();
        game1.setIgdbId(1L);
        game1.setTitle("Test Game 1");
        game1.setUpdatedAt(Instant.now().getEpochSecond());

        IGDBGameDTO game2 = new IGDBGameDTO();
        game2.setIgdbId(2L);
        game2.setTitle("Test Game 2");
        game2.setUpdatedAt(Instant.now().getEpochSecond());

        List<IGDBGameDTO> mockSearchResults = List.of(game1, game2);
        when(igdbClientService.searchGames("test query")).thenReturn(mockSearchResults);
        when(gameRepository.findByIgdbId(anyLong())).thenReturn(Optional.empty());
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<Game> syncedGames = gameSyncService.syncGamesFromSearch("test query");

        // Assert
        assertEquals(2, syncedGames.size());
        verify(gameRepository, times(4)).save(any(Game.class)); // Two saves per game (initial + after relationships)
        assertEquals("Test Game 1", syncedGames.get(0).getTitle());
        assertEquals("Test Game 2", syncedGames.get(1).getTitle());
    }
} 
