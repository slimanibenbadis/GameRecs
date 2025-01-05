package com.gamerecs.back.service;

import com.gamerecs.back.util.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest extends BaseUnitTest {
    private static final Logger logger = LoggerFactory.getLogger(JwtServiceTest.class);

    @InjectMocks
    private JwtService jwtService;

    private UserDetails userDetails;
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_JWT_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long TEST_JWT_EXPIRATION = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        logger.debug("Setting up test environment");
        
        // Create test UserDetails
        userDetails = new User(TEST_USERNAME, "password", Collections.emptyList());
        
        // Set JWT configuration using reflection
        ReflectionTestUtils.setField(jwtService, "jwtSecret", TEST_JWT_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", TEST_JWT_EXPIRATION);
    }

    @Test
    @DisplayName("Should generate token successfully")
    void shouldGenerateToken() {
        logger.debug("Testing token generation");
        
        String token = jwtService.generateToken(userDetails);
        
        assertNotNull(token, "Generated token should not be null");
        assertTrue(token.length() > 0, "Generated token should not be empty");
    }

    @Test
    @DisplayName("Should generate token with extra claims")
    void shouldGenerateTokenWithExtraClaims() {
        logger.debug("Testing token generation with extra claims");
        
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", "USER");
        extraClaims.put("email", "test@example.com");
        
        String token = jwtService.generateToken(extraClaims, userDetails);
        
        assertNotNull(token, "Generated token should not be null");
        assertTrue(token.length() > 0, "Generated token should not be empty");
    }

    @Test
    @DisplayName("Should extract username from token")
    void shouldExtractUsername() {
        logger.debug("Testing username extraction from token");
        
        String token = jwtService.generateToken(userDetails);
        String extractedUsername = jwtService.extractUsername(token);
        
        assertEquals(TEST_USERNAME, extractedUsername, "Extracted username should match original username");
    }

    @Test
    @DisplayName("Should extract expiration from token")
    void shouldExtractExpiration() {
        logger.debug("Testing expiration extraction from token");
        
        String token = jwtService.generateToken(userDetails);
        Date expiration = jwtService.extractExpiration(token);
        
        assertNotNull(expiration, "Expiration date should not be null");
        assertTrue(expiration.after(new Date()), "Token should not be expired immediately after generation");
        assertTrue(expiration.before(new Date(System.currentTimeMillis() + TEST_JWT_EXPIRATION + 1000)), 
            "Expiration should be before current time plus expiration period");
    }

    @Test
    @DisplayName("Should validate token successfully")
    void shouldValidateToken() {
        logger.debug("Testing token validation");
        
        String token = jwtService.generateToken(userDetails);
        
        assertTrue(jwtService.isTokenValid(token, userDetails), "Token should be valid");
    }

    @Test
    @DisplayName("Should invalidate token for different user")
    void shouldInvalidateTokenForDifferentUser() {
        logger.debug("Testing token validation with different user");
        
        String token = jwtService.generateToken(userDetails);
        UserDetails differentUser = new User("different", "password", Collections.emptyList());
        
        assertFalse(jwtService.isTokenValid(token, differentUser), "Token should be invalid for different user");
    }

    @Test
    @DisplayName("Should extract claims from token")
    void shouldExtractClaims() {
        logger.debug("Testing claims extraction from token");
        
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", "USER");
        String token = jwtService.generateToken(extraClaims, userDetails);
        
        String subject = jwtService.extractClaim(token, claims -> claims.getSubject());
        
        assertEquals(TEST_USERNAME, subject, "Extracted subject should match username");
    }
} 
