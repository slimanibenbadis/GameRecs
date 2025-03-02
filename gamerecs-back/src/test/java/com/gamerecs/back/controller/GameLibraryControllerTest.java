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
import java.util.LinkedHashSet;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.eq;

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
        testLibrary.setGames(new LinkedHashSet<>());
    }

    @Test
    void testGetGameLibrary_Success() throws Exception {
        // Mock the service to return a test library
        when(gameLibraryService.getLibraryForUser(eq(TEST_USER_ID), eq("title"), eq(""))).thenReturn(testLibrary);

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
        when(gameLibraryService.getLibraryForUser(eq(TEST_USER_ID), eq("title"), eq("")))
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

    @Test
    void testGetGameLibrary_SortingAndFiltering() throws Exception {
        // Arrange (create some games with various titles, release dates, and genres)
        GameLibrary sortedLibrary = new GameLibrary();
        sortedLibrary.setLibraryId(2L);
        // Prepare a library with games already sorted or will be sorted in the service layer    
        // For brevity, assume testLibrary is a GameLibrary with a list of Game objects with various "title" values.
        
        // Stub the gameLibraryService to return the sorted/filtered library when query parameters are provided
        when(gameLibraryService.getLibraryForUser(eq(TEST_USER_ID), eq("title"), eq("Action")))
            .thenReturn(sortedLibrary);

        // Act: perform a GET with query parameters
        mockMvc.perform(get("/api/game-library")
                .with(authentication(authentication))
                .param("sortBy", "title")
                .param("filterByGenre", "Action")
                .contentType(MediaType.APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.libraryId").value(2));
    }
} 
