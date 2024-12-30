package com.gamerecs.back.repository;

import com.gamerecs.back.model.User;
import com.gamerecs.back.util.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.*;

class UserRepositoryTest extends BaseIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(UserRepositoryTest.class);

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        logger.debug("Setting up test user");
        userRepository.deleteAll();
        
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("password123")
                .build();
    }

    @Test
    @DisplayName("Should save user successfully")
    void shouldSaveUser() {
        logger.debug("Testing user save operation");
        
        User savedUser = userRepository.save(testUser);
        
        assertNotNull(savedUser.getUserId(), "User ID should be generated");
        assertEquals(testUser.getUsername(), savedUser.getUsername(), "Username should match");
        assertEquals(testUser.getEmail(), savedUser.getEmail(), "Email should match");
        assertNotNull(savedUser.getJoinDate(), "Join date should be set");
    }

    @Test
    @DisplayName("Should find user by email")
    void shouldFindUserByEmail() {
        logger.debug("Testing find user by email");
        
        userRepository.save(testUser);
        var foundUser = userRepository.findByEmail(testUser.getEmail());
        
        assertTrue(foundUser.isPresent(), "User should be found");
        assertEquals(testUser.getUsername(), foundUser.get().getUsername(), "Username should match");
    }

    @Test
    @DisplayName("Should find user by username")
    void shouldFindUserByUsername() {
        logger.debug("Testing find user by username");
        
        userRepository.save(testUser);
        var foundUser = userRepository.findByUsername(testUser.getUsername());
        
        assertTrue(foundUser.isPresent(), "User should be found");
        assertEquals(testUser.getEmail(), foundUser.get().getEmail(), "Email should match");
    }

    @Test
    @DisplayName("Should check if email exists")
    void shouldCheckIfEmailExists() {
        logger.debug("Testing email existence check");
        
        userRepository.save(testUser);
        
        assertTrue(userRepository.existsByEmail(testUser.getEmail()), "Email should exist");
        assertFalse(userRepository.existsByEmail("nonexistent@example.com"), "Email should not exist");
    }

    @Test
    @DisplayName("Should check if username exists")
    void shouldCheckIfUsernameExists() {
        logger.debug("Testing username existence check");
        
        userRepository.save(testUser);
        
        assertTrue(userRepository.existsByUsername(testUser.getUsername()), "Username should exist");
        assertFalse(userRepository.existsByUsername("nonexistent"), "Username should not exist");
    }

    @Test
    @DisplayName("Should not save user with duplicate email")
    void shouldNotSaveUserWithDuplicateEmail() {
        logger.debug("Testing duplicate email constraint");
        
        userRepository.save(testUser);
        
        User duplicateEmailUser = User.builder()
                .username("different")
                .email(testUser.getEmail())
                .passwordHash("password123")
                .build();
        
        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.save(duplicateEmailUser);
            userRepository.flush();
        }, "Should throw exception for duplicate email");
    }

    @Test
    @DisplayName("Should not save user with duplicate username")
    void shouldNotSaveUserWithDuplicateUsername() {
        logger.debug("Testing duplicate username constraint");
        
        userRepository.save(testUser);
        
        User duplicateUsernameUser = User.builder()
                .username(testUser.getUsername())
                .email("different@example.com")
                .passwordHash("password123")
                .build();
        
        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.save(duplicateUsernameUser);
            userRepository.flush();
        }, "Should throw exception for duplicate username");
    }
} 
