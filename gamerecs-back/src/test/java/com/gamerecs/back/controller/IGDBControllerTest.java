package com.gamerecs.back.controller;

import com.gamerecs.back.service.IGDBClientService;
import com.gamerecs.back.service.GameSyncService;
import com.gamerecs.back.dto.IGDBGameDTO;
import com.gamerecs.back.service.JwtService;
import com.gamerecs.back.security.CustomUserDetails;
import com.gamerecs.back.util.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

import com.gamerecs.back.model.Game;
import com.gamerecs.back.service.AsyncIGDBUpdateService;
import com.gamerecs.back.service.GameService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

class IGDBControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IGDBClientService igdbClientService;

    @MockBean
    private GameSyncService gameSyncService;
    
    @MockBean
    private AsyncIGDBUpdateService asyncIGDBUpdateService;
    
    @MockBean
    private GameService gameService;

    @MockBean
    private JwtService jwtService;

    private IGDBGameDTO sampleGame;
    private Authentication authentication;
    private Authentication adminAuthentication;
    private static final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        sampleGame = new IGDBGameDTO();
        // Set sample game properties here if needed

        // Create CustomUserDetails for authentication
        CustomUserDetails userDetails = new CustomUserDetails(
            "testuser",
            "password",
            true,
            TEST_USER_ID
        );

        // Create authentication token with CustomUserDetails
        authentication = new UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        
        // Create admin authentication
        CustomUserDetails adminUserDetails = new CustomUserDetails(
            "admin",
            "password",
            true,
            2L
        );
        
        adminAuthentication = new UsernamePasswordAuthenticationToken(
            adminUserDetails,
            null,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        
        // Mock async service to return a CompletableFuture
        when(asyncIGDBUpdateService.updateGamesFromIGDB(anyString()))
            .thenReturn(CompletableFuture.completedFuture(1));
    }

    @Test
    void testTriggerIGDBUpdate() throws Exception {
        // Perform the request and verify response
        mockMvc.perform(post("/api/igdb/update")
                .with(csrf())
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON)
                .param("query", "Halo"))
            .andExpect(status().isAccepted())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("IGDB update initiated and will be processed asynchronously"))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void testTriggerIGDBUpdateWithEmptyResponse() throws Exception {
        // Perform the request and verify response
        mockMvc.perform(post("/api/igdb/update")
                .with(csrf())
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON)
                .param("query", "NonExistentGame"))
            .andExpect(status().isAccepted())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("IGDB update initiated and will be processed asynchronously"))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void testTriggerIGDBUpdateWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/igdb/update")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .param("query", "Halo"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testTriggerIGDBUpdateWithEmptyQuery() throws Exception {
        mockMvc.perform(post("/api/igdb/update")
                .with(csrf())
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON)
                .param("query", ""))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("Search query cannot be empty"));
    }
    
    @Test
    void testTriggerIGDBUpdateWithNullQuery() throws Exception {
        // Test with null query parameter - should behave like empty query
        mockMvc.perform(post("/api/igdb/update")
                .with(csrf())
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void testTriggerIGDBUpdateWithTooShortQuery() throws Exception {
        mockMvc.perform(post("/api/igdb/update")
                .with(csrf())
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON)
                .param("query", "A"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("Search query must be at least 2 characters long"));
    }
    
    @Test
    void testTriggerIGDBUpdateWithQueryNeedingSanitization() throws Exception {
        String queryWithSpecialChars = "Game;Title<with>[special]{chars}";
        String expectedSanitized = "GameTitlewithspecialchars";
        
        mockMvc.perform(post("/api/igdb/update")
                .with(csrf())
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON)
                .param("query", queryWithSpecialChars))
            .andExpect(status().isAccepted());
            
        verify(asyncIGDBUpdateService).updateGamesFromIGDB(expectedSanitized);
    }
    
    @Test
    void testTriggerIGDBUpdateWithLongQuery() throws Exception {
        // Create a query that exceeds the max length (100 characters)
        StringBuilder longQuery = new StringBuilder();
        for (int i = 0; i < 15; i++) {
            longQuery.append("ThisIsAVeryLongGameTitle");
        }
        
        mockMvc.perform(post("/api/igdb/update")
                .with(csrf())
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON)
                .param("query", longQuery.toString()))
            .andExpect(status().isAccepted());
            
        // Verify that the query was truncated to 100 characters
        verify(asyncIGDBUpdateService).updateGamesFromIGDB(longQuery.substring(0, 100));
    }
    
    @Test
    void testTriggerIGDBUpdateWithException() throws Exception {
        // Setup the async service to throw an exception
        when(asyncIGDBUpdateService.updateGamesFromIGDB("ErrorGame"))
            .thenThrow(new RuntimeException("Test error"));
            
        mockMvc.perform(post("/api/igdb/update")
                .with(csrf())
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON)
                .param("query", "ErrorGame"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("Error starting IGDB update process: Test error"));
    }
    
    @Test
    void testClearIGDBCache_Success() throws Exception {
        mockMvc.perform(post("/api/igdb/clear-cache")
                .with(csrf())
                .with(authentication(adminAuthentication))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string("IGDB cache successfully cleared"));
        
        verify(igdbClientService).clearGameSearchCache();
    }
    
    @Test
    void testClearIGDBCache_Exception() throws Exception {
        // Setup mock to throw an exception
        doThrow(new RuntimeException("Cache clear error"))
            .when(igdbClientService).clearGameSearchCache();
            
        mockMvc.perform(post("/api/igdb/clear-cache")
                .with(csrf())
                .with(authentication(adminAuthentication))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andExpect(content().string("Failed to clear IGDB cache: Cache clear error"));
    }
    
    @Test
    void testUpdateAndSearch_Success() throws Exception {
        // Arrange
        List<Game> mockGames = List.of(new Game());
        mockGames.get(0).setTitle("Halo");
        Page<Game> mockPage = new PageImpl<>(mockGames, PageRequest.of(0, 50), 1);
        
        when(asyncIGDBUpdateService.updateGamesFromIGDB("Halo")).thenReturn(CompletableFuture.completedFuture(1));
        when(gameService.searchGamesByTitleNormalized(eq("Halo"), anyInt(), anyInt())).thenReturn(mockPage);
        
        // Act & Assert
        mockMvc.perform(post("/api/igdb/update-and-search")
                .with(csrf())
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON)
                .param("query", "Halo"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.games").isArray())
            .andExpect(jsonPath("$.games.length()").value(1))
            .andExpect(jsonPath("$.games[0].title").value("Halo"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.currentPage").value(0));
    }
    
    @Test
    void testUpdateAndSearch_EmptyQuery() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/igdb/update-and-search")
                .with(csrf())
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON)
                .param("query", ""))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void testUpdateAndSearch_WithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/igdb/update-and-search")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .param("query", "Halo"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testUpdateAndSearch_AsyncUpdateFails() throws Exception {
        // Arrange
        CompletableFuture<Integer> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("API error"));
        
        when(asyncIGDBUpdateService.updateGamesFromIGDB("Halo")).thenReturn(failedFuture);
        
        // Act & Assert
        mockMvc.perform(post("/api/igdb/update-and-search")
                .with(csrf())
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON)
                .param("query", "Halo"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(result -> {
                Exception exception = result.getResolvedException();
                assert exception instanceof ResponseStatusException;
                ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                assert responseStatusException.getStatusCode().equals(HttpStatus.SERVICE_UNAVAILABLE);
                assert responseStatusException.getReason().contains("IGDB service is temporarily unavailable");
            });
    }
    
    @Test
    void testUpdateAndSearch_WithCompletionException_Timeout() throws Exception {
        // Directly mock the get() method to throw ExecutionException wrapping a TimeoutException,
        // which is what would happen in the real code when CompletableFuture.get() is called
        CompletableFuture<Integer> mockFuture = mock(CompletableFuture.class);
        when(mockFuture.get()).thenThrow(new java.util.concurrent.ExecutionException(new TimeoutException("IGDB API timeout")));
        
        when(asyncIGDBUpdateService.updateGamesFromIGDB("Halo")).thenReturn(mockFuture);
        
        // Act & Assert
        mockMvc.perform(post("/api/igdb/update-and-search")
                .with(csrf())
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON)
                .param("query", "Halo"))
            .andExpect(status().isGatewayTimeout());
    }
    
    @Test
    void testUpdateAndSearch_WithCompletionException_Other() throws Exception {
        // Directly mock the get() method to throw ExecutionException wrapping a non-TimeoutException,
        // which is what would happen in the real code when CompletableFuture.get() is called
        CompletableFuture<Integer> mockFuture = mock(CompletableFuture.class);
        when(mockFuture.get()).thenThrow(new java.util.concurrent.ExecutionException(new IllegalStateException("IGDB API unavailable")));
        
        when(asyncIGDBUpdateService.updateGamesFromIGDB("Halo")).thenReturn(mockFuture);
        
        // Act & Assert
        mockMvc.perform(post("/api/igdb/update-and-search")
                .with(csrf())
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON)
                .param("query", "Halo"))
            .andExpect(status().isServiceUnavailable());
    }
    
    @Test
    void testUpdateAndSearch_InvalidPagination() throws Exception {
        // Test negative page
        mockMvc.perform(post("/api/igdb/update-and-search")
                .with(csrf())
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON)
                .param("query", "Halo")
                .param("page", "-1"))
            .andExpect(status().isBadRequest())
            .andExpect(result -> {
                Exception exception = result.getResolvedException();
                assert exception instanceof ResponseStatusException;
                ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                assert responseStatusException.getReason().contains("Page number cannot be negative");
            });
            
        // Test zero page size
        mockMvc.perform(post("/api/igdb/update-and-search")
                .with(csrf())
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON)
                .param("query", "Halo")
                .param("size", "0"))
            .andExpect(status().isBadRequest())
            .andExpect(result -> {
                Exception exception = result.getResolvedException();
                assert exception instanceof ResponseStatusException;
                ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                assert responseStatusException.getReason().contains("Page size must be between 1 and 100");
            });
            
        // Test too large page size
        mockMvc.perform(post("/api/igdb/update-and-search")
                .with(csrf())
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON)
                .param("query", "Halo")
                .param("size", "101"))
            .andExpect(status().isBadRequest())
            .andExpect(result -> {
                Exception exception = result.getResolvedException();
                assert exception instanceof ResponseStatusException;
                ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                assert responseStatusException.getReason().contains("Page size must be between 1 and 100");
            });
    }
    
    @Test
    void testUpdateAndSearch_SearchFails() throws Exception {
        // Setup the service to succeed for the update but throw an exception during search
        when(asyncIGDBUpdateService.updateGamesFromIGDB("Halo")).thenReturn(CompletableFuture.completedFuture(1));
        when(gameService.searchGamesByTitleNormalized(eq("Halo"), anyInt(), anyInt()))
            .thenThrow(new RuntimeException("Search error"));
        
        // Act & Assert
        mockMvc.perform(post("/api/igdb/update-and-search")
                .with(csrf())
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON)
                .param("query", "Halo"))
            .andExpect(status().isInternalServerError())
            .andExpect(result -> {
                Exception exception = result.getResolvedException();
                assert exception instanceof ResponseStatusException;
                ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                assert responseStatusException.getStatusCode().equals(HttpStatus.INTERNAL_SERVER_ERROR);
                assert responseStatusException.getReason().contains("Error during game search");
            });
    }
    
    @Test
    void testUpdateAndSearch_SearchThrowsResponseStatusException() throws Exception {
        // Setup the service to succeed for the update but throw a ResponseStatusException during search
        when(asyncIGDBUpdateService.updateGamesFromIGDB("Halo")).thenReturn(CompletableFuture.completedFuture(1));
        when(gameService.searchGamesByTitleNormalized(eq("Halo"), anyInt(), anyInt()))
            .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Custom conflict error"));
        
        // Act & Assert
        mockMvc.perform(post("/api/igdb/update-and-search")
                .with(csrf())
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON)
                .param("query", "Halo"))
            .andExpect(status().isConflict())
            .andExpect(result -> {
                Exception exception = result.getResolvedException();
                assert exception instanceof ResponseStatusException;
                ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                assert responseStatusException.getStatusCode().equals(HttpStatus.CONFLICT);
                assert responseStatusException.getReason().contains("Custom conflict error");
            });
    }
    
    @Test
    void testUpdateAndSearch_GenericUnhandledException() throws Exception {
        // Setup a test to trigger the final catch block for unexpected exceptions
        when(asyncIGDBUpdateService.updateGamesFromIGDB("Halo"))
            .thenReturn(CompletableFuture.completedFuture(1));
            
        // Have the mock throw exception when toString() is called on GameSearchResponse
        when(gameService.searchGamesByTitleNormalized(eq("Halo"), anyInt(), anyInt()))
            .thenAnswer(invocation -> {
                // Return a mock page that will cause an exception later in the controller
                Page<Game> mockPage = mock(Page.class);
                doThrow(new RuntimeException("Unexpected runtime error")).when(mockPage).getContent();
                return mockPage;
            });
        
        mockMvc.perform(post("/api/igdb/update-and-search")
                .with(csrf())
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON)
                .param("query", "Halo"))
            .andExpect(status().isInternalServerError());
    }
} 
 