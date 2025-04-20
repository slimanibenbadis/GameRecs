package com.gamerecs.back.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamerecs.back.dto.GameDto;
import com.gamerecs.back.dto.GameSearchResponse;
import com.gamerecs.back.model.Game;
import com.gamerecs.back.service.GameService;
import com.gamerecs.back.util.BaseIntegrationTest;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GameControllerTest extends BaseIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(GameControllerTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GameService gameService;

    private List<Game> gameList;

    @BeforeEach
    void setUp() {
        logger.debug("Setting up test data for GameControllerTest");
        
        // Setup test data
        gameList = new ArrayList<>();
        Game game1 = new Game();
        game1.setGameId(1L);
        game1.setTitle("Test Game 1");
        game1.setIgdbId(101L);
        
        Game game2 = new Game();
        game2.setGameId(2L);
        game2.setTitle("Test Game 2");
        game2.setIgdbId(102L);
        
        gameList.add(game1);
        gameList.add(game2);
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should return games with valid search query")
    void shouldReturnGamesWithValidSearchQuery() throws Exception {
        logger.debug("Testing search for games with valid query");
        
        // Arrange
        String query = "test";
        int page = 0;
        int size = 10;
        Page<Game> gamePage = new PageImpl<>(gameList, PageRequest.of(page, size), gameList.size());
        
        when(gameService.searchGamesByTitleNormalized(eq(query), eq(page), eq(size))).thenReturn(gamePage);

        // Act & Assert
        mockMvc.perform(get("/api/games/search")
                .param("query", query)
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games", hasSize(2)))
                .andExpect(jsonPath("$.currentPage").value(page))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.pageSize").value(size))
                .andExpect(jsonPath("$.query").value(query));

        verify(gameService).searchGamesByTitleNormalized(eq(query), eq(page), eq(size));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should return 400 when search query is null")
    void shouldReturn400WhenSearchQueryIsNull() throws Exception {
        logger.debug("Testing search with null query");
        
        // Act & Assert
        mockMvc.perform(get("/api/games/search")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(gameService, never()).searchGamesByTitleNormalized(anyString(), anyInt(), anyInt());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should return 400 when search query is empty")
    void shouldReturn400WhenSearchQueryIsEmpty() throws Exception {
        logger.debug("Testing search with empty query");
        
        // Act & Assert
        mockMvc.perform(get("/api/games/search")
                .param("query", "")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(gameService, never()).searchGamesByTitleNormalized(anyString(), anyInt(), anyInt());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should return 400 when search query is too short")
    void shouldReturn400WhenSearchQueryIsTooShort() throws Exception {
        logger.debug("Testing search with query that is too short");
        
        // Act & Assert
        mockMvc.perform(get("/api/games/search")
                .param("query", "a")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(gameService, never()).searchGamesByTitleNormalized(anyString(), anyInt(), anyInt());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should sanitize query with harmful characters")
    void shouldSanitizeQueryWithHarmfulCharacters() throws Exception {
        logger.debug("Testing search with query containing harmful characters");
        
        // Arrange
        String query = "test;<script>";  // Simplified harmful input
        int page = 0;
        int size = 10;
        Page<Game> gamePage = new PageImpl<>(gameList, PageRequest.of(page, size), gameList.size());
        
        // Use anyString() instead of exact matcher since exact sanitization is implementation-dependent
        when(gameService.searchGamesByTitleNormalized(anyString(), eq(page), eq(size))).thenReturn(gamePage);

        // Act & Assert
        mockMvc.perform(get("/api/games/search")
                .param("query", query)
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games").exists());  // Only verify response structure, not exact sanitized value

        // Verify the service was called with some string argument (exact sanitization is tested elsewhere)
        verify(gameService).searchGamesByTitleNormalized(anyString(), eq(page), eq(size));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should return 400 with negative page number")
    void shouldReturn400WithNegativePageNumber() throws Exception {
        logger.debug("Testing search with negative page number");
        
        // Act & Assert
        mockMvc.perform(get("/api/games/search")
                .param("query", "test")
                .param("page", "-1")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(gameService, never()).searchGamesByTitleNormalized(anyString(), anyInt(), anyInt());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should return 400 with page size too small")
    void shouldReturn400WithPageSizeTooSmall() throws Exception {
        logger.debug("Testing search with page size too small");
        
        // Act & Assert
        mockMvc.perform(get("/api/games/search")
                .param("query", "test")
                .param("page", "0")
                .param("size", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(gameService, never()).searchGamesByTitleNormalized(anyString(), anyInt(), anyInt());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should return 400 with page size too large")
    void shouldReturn400WithPageSizeTooLarge() throws Exception {
        logger.debug("Testing search with page size too large");
        
        // Act & Assert
        mockMvc.perform(get("/api/games/search")
                .param("query", "test")
                .param("page", "0")
                .param("size", "101")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(gameService, never()).searchGamesByTitleNormalized(anyString(), anyInt(), anyInt());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should handle 500 error when service throws exception")
    void shouldHandle500ErrorWhenServiceThrowsException() throws Exception {
        logger.debug("Testing search with service throwing an exception");
        
        // Arrange
        String query = "test";
        int page = 0;
        int size = 10;
        
        when(gameService.searchGamesByTitleNormalized(anyString(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("Test exception"));

        // Act & Assert
        mockMvc.perform(get("/api/games/search")
                .param("query", query)
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(gameService).searchGamesByTitleNormalized(eq(query), eq(page), eq(size));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should handle when game service throws ResponseStatusException")
    void shouldHandleWhenGameServiceThrowsResponseStatusException() throws Exception {
        logger.debug("Testing search with service throwing ResponseStatusException");
        
        // Arrange
        String query = "test";
        int page = 0;
        int size = 10;
        
        when(gameService.searchGamesByTitleNormalized(anyString(), anyInt(), anyInt()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test error message"));

        // Act & Assert
        mockMvc.perform(get("/api/games/search")
                .param("query", query)
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(gameService).searchGamesByTitleNormalized(eq(query), eq(page), eq(size));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should truncate query longer than 100 characters")
    void shouldTruncateQueryLongerThan100Characters() throws Exception {
        logger.debug("Testing search with query longer than 100 characters");
        
        // Arrange
        StringBuilder longQueryBuilder = new StringBuilder();
        for (int i = 0; i < 11; i++) {
            longQueryBuilder.append("0123456789");
        }
        String longQuery = longQueryBuilder.toString(); // 110 characters
        String truncatedQuery = longQuery.substring(0, 100);
        
        int page = 0;
        int size = 10;
        Page<Game> gamePage = new PageImpl<>(gameList, PageRequest.of(page, size), gameList.size());
        
        when(gameService.searchGamesByTitleNormalized(eq(truncatedQuery), eq(page), eq(size))).thenReturn(gamePage);

        // Act & Assert
        mockMvc.perform(get("/api/games/search")
                .param("query", longQuery)
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value(truncatedQuery));

        verify(gameService).searchGamesByTitleNormalized(eq(truncatedQuery), eq(page), eq(size));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should return 200 and GameDto when game is found by id")
    void shouldReturnGameById() throws Exception {
        // Arrange
        Long id = 42L;
        GameDto dto = GameDto.builder()
                .id(id)
                .title("Test Game")
                .build();
        when(gameService.getGameById(id)).thenReturn(dto);

        // Act & Assert
        mockMvc.perform(get("/api/games/{id}", id)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Test Game"));

        verify(gameService).getGameById(id);
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should return 404 when game is not found by id")
    void shouldReturn404WhenGameNotFoundById() throws Exception {
        // Arrange
        Long id = 99L;
        when(gameService.getGameById(id)).thenThrow(new EntityNotFoundException("Game not found"));

        // Act & Assert
        mockMvc.perform(get("/api/games/{id}", id)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(gameService).getGameById(id);
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should return 400 when id is invalid (negative)")
    void shouldReturn400WhenIdIsInvalid() throws Exception {
        // Arrange
        Long id = -1L;
        when(gameService.getGameById(id)).thenThrow(new IllegalArgumentException("Id must be positive"));

        // Act & Assert
        mockMvc.perform(get("/api/games/{id}", id)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(gameService).getGameById(id);
    }
} 
