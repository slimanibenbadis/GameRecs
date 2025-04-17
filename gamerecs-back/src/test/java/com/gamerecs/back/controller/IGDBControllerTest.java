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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.gamerecs.back.model.Game;
import com.gamerecs.back.service.AsyncIGDBUpdateService;
import com.gamerecs.back.service.GameService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
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
    void testUpdateAndSearch_Success() throws Exception {
        // Arrange
        List<Game> mockGames = List.of(new Game());
        mockGames.get(0).setTitle("Halo");
        Page<Game> mockPage = new PageImpl<>(mockGames, PageRequest.of(0, 50), 1);
        
        when(asyncIGDBUpdateService.updateGamesFromIGDB("Halo")).thenReturn(CompletableFuture.completedFuture(1));
        when(gameService.searchGamesByTitle(eq("Halo"), anyInt(), anyInt())).thenReturn(mockPage);
        
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
            .andExpect(status().isInternalServerError());
    }
} 
 