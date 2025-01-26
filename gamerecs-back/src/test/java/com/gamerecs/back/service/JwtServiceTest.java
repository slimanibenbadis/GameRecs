package com.gamerecs.back.service;

import com.gamerecs.back.util.BaseUnitTest;
import io.jsonwebtoken.Claims;
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
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest extends BaseUnitTest {
    private static final Logger logger = LoggerFactory.getLogger(JwtServiceTest.class);

    @InjectMocks
    private JwtService jwtService;

    private UserDetails userDetails;
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_EMAIL = "test@example.com";
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

    @Test
    @DisplayName("Should generate token with email successfully")
    void shouldGenerateTokenWithEmail() {
        logger.debug("Testing token generation with email");
        
        // Execute
        String token = jwtService.generateToken(TEST_EMAIL);
        
        // Verify token is not null or empty
        assertNotNull(token, "Generated token should not be null");
        assertTrue(token.length() > 0, "Generated token should not be empty");
        
        // Verify token structure (should have 3 parts separated by dots)
        String[] tokenParts = token.split("\\.");
        assertEquals(3, tokenParts.length, "Token should have header, payload, and signature");
        
        // Verify claims using public methods
        String subject = jwtService.extractUsername(token);
        String email = jwtService.extractClaim(token, claims -> claims.get("email", String.class));
        Date expiration = jwtService.extractExpiration(token);
        
        assertEquals(TEST_EMAIL, subject, "Token subject should match email");
        assertEquals(TEST_EMAIL, email, "Token email claim should match email");
        
        // Verify expiration
        assertNotNull(expiration, "Token should have expiration date");
        assertTrue(expiration.after(new Date()), "Token should not be expired immediately");
        assertTrue(expiration.before(new Date(System.currentTimeMillis() + TEST_JWT_EXPIRATION + 1000)), 
            "Token expiration should be within expected range");
    }

    @Test
    @DisplayName("Should generate unique tokens for different emails")
    void shouldGenerateUniqueTokens() {
        logger.debug("Testing unique token generation for different emails");
        
        String token1 = jwtService.generateToken(TEST_EMAIL);
        String token2 = jwtService.generateToken("different@example.com");
        
        assertNotEquals(token1, token2, "Tokens for different emails should be unique");
    }

    @Test
    @DisplayName("Should handle null email gracefully")
    void shouldHandleNullEmail() {
        logger.debug("Testing token generation with null email");
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            jwtService.generateToken((String)null);
        });
        
        assertEquals("Email cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should handle empty email gracefully")
    void shouldHandleEmptyEmail() {
        logger.debug("Testing token generation with empty email");
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            jwtService.generateToken("");
        });
        
        assertEquals("Email cannot be empty", exception.getMessage());
    }

    @Test
    @DisplayName("Should generate token with consistent claims")
    void shouldGenerateTokenWithConsistentClaims() {
        logger.debug("Testing token generation with consistent claims");
        
        // Generate multiple tokens for the same email
        String token1 = jwtService.generateToken(TEST_EMAIL);
        String token2 = jwtService.generateToken(TEST_EMAIL);
        
        // Extract claims using public methods
        String subject1 = jwtService.extractUsername(token1);
        String subject2 = jwtService.extractUsername(token2);
        String email1 = jwtService.extractClaim(token1, claims -> claims.get("email", String.class));
        String email2 = jwtService.extractClaim(token2, claims -> claims.get("email", String.class));
        
        // Verify consistent claims
        assertEquals(subject1, subject2, "Subject should be consistent");
        assertEquals(email1, email2, "Email claim should be consistent");
        assertEquals(TEST_EMAIL, email1, "Email claim should match input email");
        
        // Tokens should be different due to different nonce values
        assertNotEquals(token1, token2, "Tokens should be unique even for same email due to timing");
    }
} 
