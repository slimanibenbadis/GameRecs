package com.gamerecs.back.config;

import com.gamerecs.back.service.JwtService;
import com.gamerecs.back.util.BaseUnitTest;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest extends BaseUnitTest {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilterTest.class);

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String TEST_USERNAME = "testuser";
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        logger.debug("Setting up test environment");
        
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        userDetails = new User(TEST_USERNAME, "password", Collections.emptyList());
        
        // Clear security context before each test
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should process valid JWT token successfully")
    void shouldProcessValidToken() throws Exception {
        logger.debug("Testing processing of valid JWT token");
        
        // Setup
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(jwtService.extractUsername(VALID_TOKEN)).thenReturn(TEST_USERNAME);
        when(userDetailsService.loadUserByUsername(TEST_USERNAME)).thenReturn(userDetails);
        when(jwtService.isTokenValid(VALID_TOKEN, userDetails)).thenReturn(true);

        // Execute
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Verify
        verify(filterChain).doFilter(request, response);
        verify(jwtService).extractUsername(VALID_TOKEN);
        verify(userDetailsService).loadUserByUsername(TEST_USERNAME);
        verify(jwtService).isTokenValid(VALID_TOKEN, userDetails);
        
        assertNotNull(SecurityContextHolder.getContext().getAuthentication(), 
            "Authentication should be set in SecurityContext");
        assertEquals(TEST_USERNAME, 
            SecurityContextHolder.getContext().getAuthentication().getName(),
            "Username in SecurityContext should match");
    }

    @Test
    @DisplayName("Should skip processing for missing Authorization header")
    void shouldSkipForMissingAuthHeader() throws Exception {
        logger.debug("Testing behavior with missing Authorization header");
        
        // Setup
        when(request.getHeader("Authorization")).thenReturn(null);

        // Execute
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Verify
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).extractUsername(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        
        assertNull(SecurityContextHolder.getContext().getAuthentication(),
            "No authentication should be set in SecurityContext");
    }

    @Test
    @DisplayName("Should skip processing for invalid Authorization header format")
    void shouldSkipForInvalidAuthHeaderFormat() throws Exception {
        logger.debug("Testing behavior with invalid Authorization header format");
        
        // Setup
        when(request.getHeader("Authorization")).thenReturn("InvalidFormat " + VALID_TOKEN);

        // Execute
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Verify
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).extractUsername(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        
        assertNull(SecurityContextHolder.getContext().getAuthentication(),
            "No authentication should be set in SecurityContext");
    }

    @Test
    @DisplayName("Should handle invalid JWT token")
    void shouldHandleInvalidToken() throws Exception {
        logger.debug("Testing handling of invalid JWT token");
        
        // Setup
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(jwtService.extractUsername(VALID_TOKEN)).thenReturn(TEST_USERNAME);
        when(userDetailsService.loadUserByUsername(TEST_USERNAME)).thenReturn(userDetails);
        when(jwtService.isTokenValid(VALID_TOKEN, userDetails)).thenReturn(false);

        // Execute
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Verify
        verify(filterChain).doFilter(request, response);
        verify(jwtService).extractUsername(VALID_TOKEN);
        verify(userDetailsService).loadUserByUsername(TEST_USERNAME);
        verify(jwtService).isTokenValid(VALID_TOKEN, userDetails);
        
        assertNull(SecurityContextHolder.getContext().getAuthentication(),
            "No authentication should be set in SecurityContext for invalid token");
    }

    @Test
    @DisplayName("Should handle JWT extraction exception")
    void shouldHandleJwtExtractionException() throws Exception {
        logger.debug("Testing handling of JWT extraction exception");
        
        // Setup
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(jwtService.extractUsername(VALID_TOKEN)).thenThrow(new RuntimeException("Invalid token"));

        // Execute
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Verify
        verify(filterChain).doFilter(request, response);
        verify(jwtService).extractUsername(VALID_TOKEN);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        
        assertNull(SecurityContextHolder.getContext().getAuthentication(),
            "No authentication should be set in SecurityContext when exception occurs");
    }

    @Test
    @DisplayName("Should handle UserDetailsService exception")
    void shouldHandleUserDetailsException() throws Exception {
        logger.debug("Testing handling of UserDetailsService exception");
        
        // Setup
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(jwtService.extractUsername(VALID_TOKEN)).thenReturn(TEST_USERNAME);
        when(userDetailsService.loadUserByUsername(TEST_USERNAME))
            .thenThrow(new RuntimeException("User not found"));

        // Execute
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Verify
        verify(filterChain).doFilter(request, response);
        verify(jwtService).extractUsername(VALID_TOKEN);
        verify(userDetailsService).loadUserByUsername(TEST_USERNAME);
        verify(jwtService, never()).isTokenValid(anyString(), any(UserDetails.class));
        
        assertNull(SecurityContextHolder.getContext().getAuthentication(),
            "No authentication should be set in SecurityContext when exception occurs");
    }
} 
