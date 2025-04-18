package com.gamerecs.back.integration;

import com.gamerecs.back.model.Game;
import com.gamerecs.back.repository.GameRepository;
import com.gamerecs.back.service.GameService;
import com.gamerecs.back.util.StringNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class GameSearchIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(GameSearchIntegrationTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GameRepository gameRepository;
    
    @Autowired
    private GameService gameService;

    @BeforeEach
    void setUp() {
        // Clean up existing data
        gameRepository.deleteAll();
        
        // Create test games with various special characters in titles
        createTestGame(1001L, "Pokémon Red", "The original Pokémon game", LocalDate.of(1996, 2, 27));
        createTestGame(1002L, "Pokémon Blue", "The original Pokémon game - blue version", LocalDate.of(1996, 2, 27));
        createTestGame(1003L, "Assassin's Creed", "Action-adventure stealth game", LocalDate.of(2007, 11, 13));
        createTestGame(1004L, "The Legend of Zelda: Breath of the Wild", "Open-world adventure game", LocalDate.of(2017, 3, 3));
        createTestGame(1005L, "Final Fantasy VII", "JRPG classic", LocalDate.of(1997, 1, 31));
        createTestGame(1006L, "Cœur", "Game with special character", LocalDate.of(2023, 1, 1));
        createTestGame(1007L, "El Niño", "Game with Spanish character", LocalDate.of(2023, 2, 1));
        createTestGame(1008L, "Super Mario Bros.", "Classic platformer", LocalDate.of(1985, 9, 13));
        createTestGame(1009L, "Sid Meier's Civilization VI", "Turn-based strategy game", LocalDate.of(2016, 10, 21));
        createTestGame(1010L, "Star Wars: Knights of the Old Republic", "RPG set in the Star Wars universe", LocalDate.of(2003, 7, 15));
        createTestGame(1011L, "Mario & Luigi", "Nintendo game featuring brothers", LocalDate.of(2003, 11, 17));
        
        // Log all games for debugging
        List<Game> allGames = gameRepository.findAll();
        logger.info("Test database contains {} games:", allGames.size());
        for (Game game : allGames) {
            logger.info("Game: {}({}) - Normalized: {}", game.getTitle(), game.getIgdbId(), game.getNormalizedTitle());
        }
        
        // Verify directly with service that search works
        String testQuery = "pokemon";
        Page<Game> testResults = gameService.searchGamesByTitleNormalized(testQuery, 0, 10);
        logger.info("Direct service test search for '{}' returned {} results", 
            testQuery, testResults.getTotalElements());
        for (Game game : testResults.getContent()) {
            logger.info("Search result: {}", game.getTitle());
        }
    }
    
    private Game createTestGame(Long igdbId, String title, String description, LocalDate releaseDate) {
        Game game = new Game();
        game.setIgdbId(igdbId);
        game.setTitle(title);
        game.setDescription(description);
        game.setReleaseDate(releaseDate);
        game.setUpdatedAt(LocalDateTime.now());
        game.setGenres(new HashSet<>());
        game.setPlatforms(new HashSet<>());
        game.setPublishers(new HashSet<>());
        game.setDevelopers(new HashSet<>());
        
        // Set normalized title manually for testing
        game.setNormalizedTitle(StringNormalizer.normalize(title));
        
        return gameRepository.save(game);
    }

    @Test
    @WithMockUser
    void searchGames_shouldFindGameByExactTitle() throws Exception {
        String query = "Pokémon Red";
        String normalized = StringNormalizer.normalize(query);
        logger.info("Searching for '{}', normalized to '{}'", query, normalized);
        
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/api/games/search")
                .param("query", query)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
                
        String responseBody = result.getResponse().getContentAsString();
        logger.info("Response: {}", responseBody);
        
        // Direct test to verify games are in DB
        Page<Game> directResults = gameService.searchGamesByTitleNormalized(query, 0, 10);
        logger.info("Direct service call for query '{}' returned {} results", 
            query, directResults.getTotalElements());
        
        // Now perform actual assertions
        mockMvc.perform(MockMvcRequestBuilders.get("/api/games/search")
                .param("query", query)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.games[0].title", containsString("Pokémon")));
    }

    @Test
    @WithMockUser
    void searchGames_shouldFindGameWithoutAccents() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/api/games/search")
                .param("query", "Pokemon")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
                
        String responseBody = result.getResponse().getContentAsString();
        logger.info("Response for 'Pokemon': {}", responseBody);
        
        mockMvc.perform(MockMvcRequestBuilders.get("/api/games/search")
                .param("query", "Pokemon")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.games[0].title", containsString("Pokémon")));
    }

    @Test
    @WithMockUser
    void searchGames_shouldFindGameWithoutPunctuation() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/api/games/search")
                .param("query", "Assassins Creed")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
                
        String responseBody = result.getResponse().getContentAsString();
        logger.info("Response for 'Assassins Creed': {}", responseBody);
        
        mockMvc.perform(MockMvcRequestBuilders.get("/api/games/search")
                .param("query", "Assassins Creed")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.games[0].title", is("Assassin's Creed")));
    }

    @Test
    @WithMockUser
    void searchGames_shouldHandleSpecialCharacters() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/api/games/search")
                .param("query", "Assassin's: Creed")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
                
        String responseBody = result.getResponse().getContentAsString();
        logger.info("Response for 'Assassin's: Creed': {}", responseBody);
        
        mockMvc.perform(MockMvcRequestBuilders.get("/api/games/search")
                .param("query", "Assassin's: Creed")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.games[0].title", is("Assassin's Creed")));
    }

    @Test
    @WithMockUser
    void searchGames_shouldHandleCaseInsensitivity() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/api/games/search")
                .param("query", "pOkEmOn")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
                
        String responseBody = result.getResponse().getContentAsString();
        logger.info("Response for 'pOkEmOn': {}", responseBody);
        
        mockMvc.perform(MockMvcRequestBuilders.get("/api/games/search")
                .param("query", "pOkEmOn")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.games[0].title", containsString("Pokémon")));
    }

    @Test
    @WithMockUser
    void searchGames_shouldReturnEmptyResult_whenNoMatchingGames() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/games/search")
                .param("query", "NonExistentGameTitle")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    @WithMockUser
    void searchGames_shouldRespond400_whenQueryTooShort() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/games/search")
                .param("query", "a")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void searchGames_shouldHandlePagination() throws Exception {
        // Add a bunch of Pokemon games to ensure pagination
        for (int i = 0; i < 20; i++) {
            createTestGame(2000L + i, "Pokémon Game " + i, "Test game " + i, LocalDate.now());
        }

        // Test first page
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/api/games/search")
                .param("query", "Pokemon")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
                
        String responseBody = result.getResponse().getContentAsString();
        logger.info("Pagination test response: {}", responseBody);

        // Direct test to verify game count
        Page<Game> directResults = gameService.searchGamesByTitleNormalized("Pokemon", 0, 100);
        logger.info("Pagination test - Direct service call returned {} results", 
            directResults.getTotalElements());
        
        mockMvc.perform(MockMvcRequestBuilders.get("/api/games/search")
                .param("query", "Pokemon")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games", hasSize(10)))
                .andExpect(jsonPath("$.totalPages", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.currentPage", is(0)));

        // Test second page
        mockMvc.perform(MockMvcRequestBuilders.get("/api/games/search")
                .param("query", "Pokemon")
                .param("page", "1")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.currentPage", is(1)));
    }
} 
