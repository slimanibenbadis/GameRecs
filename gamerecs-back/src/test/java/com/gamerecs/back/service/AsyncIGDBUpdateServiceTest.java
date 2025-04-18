package com.gamerecs.back.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.gamerecs.back.dto.IGDBGameDTO;
import com.gamerecs.back.model.Game;

@ExtendWith(MockitoExtension.class)
class AsyncIGDBUpdateServiceTest {

    @Mock
    private IGDBClientService igdbClientService;

    @Mock
    private GameSyncService gameSyncService;

    @InjectMocks
    private AsyncIGDBUpdateService asyncIGDBUpdateService;
    
    private static final int TEST_TIMEOUT_SECONDS = 20;
    
    @BeforeEach
    void setUp() {
        // Manually initialize service to set appropriate timeout for tests
        // This prevents the timeout issue where the test is failing with 0 seconds timeout
        ReflectionTestUtils.setField(asyncIGDBUpdateService, "configuredTimeoutSeconds", TEST_TIMEOUT_SECONDS);
        ReflectionTestUtils.setField(asyncIGDBUpdateService, "igdbUpdateTimeoutSeconds", TEST_TIMEOUT_SECONDS);
        
        // Alternative approach using the new method
        // asyncIGDBUpdateService.setTimeoutForTests(TEST_TIMEOUT_SECONDS);
    }

    @Test
    void testUpdateGamesFromIGDB_Success() throws ExecutionException, InterruptedException, TimeoutException {
        // Arrange
        String query = "test query";
        List<IGDBGameDTO> mockIgdbGames = new ArrayList<>();
        mockIgdbGames.add(new IGDBGameDTO());
        mockIgdbGames.add(new IGDBGameDTO());
        
        List<Game> mockSavedGames = new ArrayList<>();
        mockSavedGames.add(new Game());
        mockSavedGames.add(new Game());
        
        when(igdbClientService.searchGames(anyString())).thenReturn(mockIgdbGames);
        when(gameSyncService.syncGamesFromSearch(anyList())).thenReturn(mockSavedGames);
        
        // Act
        CompletableFuture<Integer> result = asyncIGDBUpdateService.updateGamesFromIGDB(query);
        
        // Assert
        Integer count = result.get(5, TimeUnit.SECONDS); // Wait for the async operation to complete
        assertEquals(2, count);
        verify(igdbClientService).searchGames(anyString());
        verify(gameSyncService).syncGamesFromSearch(anyList());
    }
    
    @Test
    void testUpdateGamesFromIGDB_EmptyQuery() throws ExecutionException, InterruptedException, TimeoutException {
        // Arrange
        String query = "";
        
        // Act
        CompletableFuture<Integer> result = asyncIGDBUpdateService.updateGamesFromIGDB(query);
        
        // Assert
        Integer count = result.get(5, TimeUnit.SECONDS);
        assertEquals(0, count);
        verify(igdbClientService, never()).searchGames(anyString());
        verify(gameSyncService, never()).syncGamesFromSearch(anyList());
    }
    
    @Test
    void testUpdateGamesFromIGDB_Exception() {
        // Arrange
        String query = "test query";
        when(igdbClientService.searchGames(anyString())).thenThrow(new RuntimeException("API error"));
        
        // Act
        CompletableFuture<Integer> result = asyncIGDBUpdateService.updateGamesFromIGDB(query);
        
        // Assert
        assertThrows(Exception.class, () -> result.get(5, TimeUnit.SECONDS));
        verify(igdbClientService).searchGames(anyString());
        verify(gameSyncService, never()).syncGamesFromSearch(anyList());
    }
    
    @Test
    void testUpdateGamesFromIGDB_EmptyResults() throws ExecutionException, InterruptedException, TimeoutException {
        // Arrange
        String query = "test query";
        List<IGDBGameDTO> emptyList = new ArrayList<>();
        
        when(igdbClientService.searchGames(anyString())).thenReturn(emptyList);
        
        // Act
        CompletableFuture<Integer> result = asyncIGDBUpdateService.updateGamesFromIGDB(query);
        
        // Assert
        Integer count = result.get(5, TimeUnit.SECONDS);
        assertEquals(0, count);
        verify(igdbClientService).searchGames(anyString());
        verify(gameSyncService, never()).syncGamesFromSearch(anyList());
    }
    
    @Test
    void testWaitForUpdateCompletion() throws ExecutionException, InterruptedException, TimeoutException {
        // Arrange
        CompletableFuture<Integer> future = CompletableFuture.completedFuture(5);
        
        // Act
        int result = asyncIGDBUpdateService.waitForUpdateCompletion(future, 1);
        
        // Assert
        assertEquals(5, result);
    }
} 
