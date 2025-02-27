package com.gamerecs.back.controller;

import com.gamerecs.back.model.GameLibrary;
import com.gamerecs.back.model.User;
import com.gamerecs.back.security.CustomUserDetails;
import com.gamerecs.back.service.GameLibraryService;
import com.gamerecs.back.util.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.HashSet;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GameLibraryControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GameLibraryService gameLibraryService;

    private Authentication authentication;
    private static final Long TEST_USER_ID = 1L;
    private GameLibrary testLibrary;

    @BeforeEach
    void setUp() {
        // Set up authentication
        CustomUserDetails userDetails = new CustomUserDetails(
                "test@example.com",
                "password",
                true,
                TEST_USER_ID);
        authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        
        // Set up test library
        testLibrary = new GameLibrary();
        testLibrary.setLibraryId(1L);
        testLibrary.setUser(new User());
        testLibrary.setGames(new HashSet<>());
    }

    @Test
    void testGetGameLibrary_Success() throws Exception {
        // Mock the service to return a test library
        when(gameLibraryService.getLibraryForUser(TEST_USER_ID)).thenReturn(testLibrary);

        // Perform the request with authentication
        mockMvc.perform(get("/api/game-library")
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.libraryId").value(1));
    }

    @Test
    void testGetGameLibrary_NotFound() throws Exception {
        // Mock the service to throw a not found exception
        when(gameLibraryService.getLibraryForUser(TEST_USER_ID))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game library not found"));

        // Perform the request with authentication
        mockMvc.perform(get("/api/game-library")
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Game library not found"));
    }

    @Test
    void testGetGameLibrary_Unauthorized() throws Exception {
        // Perform the request without authentication
        mockMvc.perform(get("/api/game-library")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
} 
