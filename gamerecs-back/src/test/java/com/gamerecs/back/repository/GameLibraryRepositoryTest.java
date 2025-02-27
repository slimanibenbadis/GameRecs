package com.gamerecs.back.repository;

import com.gamerecs.back.model.Game;
import com.gamerecs.back.model.GameLibrary;
import com.gamerecs.back.model.User;
import com.gamerecs.back.util.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GameLibraryRepositoryTest extends BaseIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(GameLibraryRepositoryTest.class);

    @Autowired
    private GameLibraryRepository gameLibraryRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private GameRepository gameRepository;

    private User testUser;
    private Game testGame;

    @BeforeEach
    void setUp() {
        logger.debug("Setting up test data");
        gameLibraryRepository.deleteAll();
        userRepository.deleteAll();
        gameRepository.deleteAll();
        
        // Create a test user
        testUser = User.builder()
                .username("libraryUser")
                .email("library@example.com")
                .passwordHash("password123")
                .build();
        testUser = userRepository.save(testUser);
        
        // Create a test game
        testGame = new Game();
        testGame.setTitle("Test Game");
        testGame.setIgdbId(12345L);
        testGame = gameRepository.save(testGame);
    }

    @Test
    @DisplayName("Should create a game library for a user")
    void testCreateGameLibraryForUser() {
        logger.debug("Testing game library creation for user");
        
        // Create a game library for the user
        GameLibrary library = new GameLibrary();
        library.setUser(testUser);
        library = gameLibraryRepository.save(library);

        Optional<GameLibrary> found = gameLibraryRepository.findById(library.getLibraryId());
        
        assertTrue(found.isPresent(), "Game library should be present");
        assertEquals(testUser.getUserId(), found.get().getUser().getUserId(), "User should be associated with the library");
    }

    @Test
    @DisplayName("Should add a game to the library")
    void testAddGameToLibrary() {
        logger.debug("Testing adding a game to the library");
        
        // Create a game library for the user
        GameLibrary library = new GameLibrary();
        library.setUser(testUser);
        Set<Game> games = new HashSet<>();
        games.add(testGame);
        library.setGames(games);
        library = gameLibraryRepository.save(library);

        Optional<GameLibrary> found = gameLibraryRepository.findById(library.getLibraryId());
        
        assertTrue(found.isPresent(), "Game library should be present");
        assertEquals(1, found.get().getGames().size(), "Library should contain 1 game");
        assertTrue(found.get().getGames().contains(testGame), "Library should contain the test game");
    }

    @Test
    @DisplayName("Should remove a game from the library")
    void testRemoveGameFromLibrary() {
        logger.debug("Testing removing a game from the library");
        
        // Create a game library with a game
        GameLibrary library = new GameLibrary();
        library.setUser(testUser);
        Set<Game> games = new HashSet<>();
        games.add(testGame);
        library.setGames(games);
        library = gameLibraryRepository.save(library);

        // Remove game from library
        library.setGames(new HashSet<>());
        library = gameLibraryRepository.save(library);
        
        Optional<GameLibrary> found = gameLibraryRepository.findById(library.getLibraryId());
        
        assertTrue(found.isPresent(), "Game library should be present");
        assertEquals(0, found.get().getGames().size(), "Library should contain 0 games");
    }

    @Test
    @DisplayName("Should enforce one-to-one relationship between User and GameLibrary")
    void testOneToOneRelationship() {
        logger.debug("Testing one-to-one relationship constraint");
        
        // Create first library for user
        GameLibrary library1 = new GameLibrary();
        library1.setUser(testUser);
        library1 = gameLibraryRepository.save(library1);
        
        // Create second library for same user - should fail
        GameLibrary library2 = new GameLibrary();
        library2.setUser(testUser);
        
        assertThrows(DataIntegrityViolationException.class, () -> {
            gameLibraryRepository.save(library2);
            gameLibraryRepository.flush();
        }, "Should not allow multiple libraries for the same user");
    }

    @Test
    @DisplayName("Should find library by user")
    void testFindLibraryByUser() {
        logger.debug("Testing finding library by user");
        
        // Create a game library for the user
        GameLibrary library = new GameLibrary();
        library.setUser(testUser);
        library = gameLibraryRepository.save(library);

        Optional<GameLibrary> found = gameLibraryRepository.findByUser(testUser);
        
        assertTrue(found.isPresent(), "Game library should be found by user");
        assertEquals(library.getLibraryId(), found.get().getLibraryId(), "Found library should match created library");
    }
} 
