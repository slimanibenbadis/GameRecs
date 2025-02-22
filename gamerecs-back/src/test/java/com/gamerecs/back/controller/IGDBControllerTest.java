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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import com.gamerecs.back.model.Game;

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
    }

    @Test
    void testTriggerIGDBUpdate() throws Exception {
        // Create mock response
        List<IGDBGameDTO> mockResponse = Arrays.asList(sampleGame);

        // Mock service responses
        when(igdbClientService.searchGames("Halo")).thenReturn(mockResponse);
        when(gameSyncService.syncGamesFromSearch(mockResponse)).thenReturn(List.of(new Game()));

        // Perform the request and verify response
        mockMvc.perform(post("/api/igdb/update")
                .with(csrf())
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON)
                .param("query", "Halo"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("IGDB update completed and data persisted."))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void testTriggerIGDBUpdateWithEmptyResponse() throws Exception {
        // Mock empty response from service
        List<IGDBGameDTO> emptyResponse = List.of();
        when(igdbClientService.searchGames("NonExistentGame")).thenReturn(emptyResponse);
        when(gameSyncService.syncGamesFromSearch(emptyResponse)).thenReturn(List.of());

        // Perform the request and verify response
        mockMvc.perform(post("/api/igdb/update")
                .with(csrf())
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON)
                .param("query", "NonExistentGame"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("IGDB update completed and data persisted."))
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
} 
 