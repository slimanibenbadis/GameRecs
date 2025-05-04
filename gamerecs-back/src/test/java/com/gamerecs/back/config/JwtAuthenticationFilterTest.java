package com.gamerecs.back.config;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import com.gamerecs.back.service.JwtService;
import com.gamerecs.back.util.BaseUnitTest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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

    @Mock
    private CsrfTokenRepository csrfTokenRepository;

    @Mock
    private Environment environment;

    private CsrfToken csrfToken;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String TEST_USERNAME = "testuser";
    private static final String VALID_CSRF_TOKEN = "valid-csrf-token";
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        logger.debug("Setting up test environment");
        
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService, userDetailsService, csrfTokenRepository, environment);
        userDetails = new User(TEST_USERNAME, "password", Collections.emptyList());
        csrfToken = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", VALID_CSRF_TOKEN);
        
        // Clear security context before each test
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should process valid JWT token successfully for GET request")
    void shouldProcessValidTokenForGet() throws Exception {
        logger.debug("Testing processing of valid JWT token for GET request");
        
        // Setup
        when(request.getMethod()).thenReturn(HttpMethod.GET.name());
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
    @DisplayName("Should process valid JWT and CSRF token successfully for POST request")
    void shouldProcessValidTokenAndCsrfForPost() throws Exception {
        logger.debug("Testing processing of valid JWT and CSRF token for POST request");
        
        // Setup
        when(request.getMethod()).thenReturn(HttpMethod.POST.name());
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(request.getHeader("X-XSRF-TOKEN")).thenReturn(VALID_CSRF_TOKEN);
        when(csrfTokenRepository.loadToken(request)).thenReturn(csrfToken);
        when(jwtService.extractUsername(VALID_TOKEN)).thenReturn(TEST_USERNAME);
        when(userDetailsService.loadUserByUsername(TEST_USERNAME)).thenReturn(userDetails);
        when(jwtService.isTokenValid(VALID_TOKEN, userDetails)).thenReturn(true);

        // Execute
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Verify
        verify(csrfTokenRepository).loadToken(request);
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
    @DisplayName("Should reject POST request with missing CSRF token in header")
    void shouldRejectPostWithMissingCsrfHeader() throws Exception {
        logger.debug("Testing rejection of POST request with missing CSRF header");
        
        // Setup
        when(request.getMethod()).thenReturn(HttpMethod.POST.name());
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(csrfTokenRepository.loadToken(request)).thenReturn(csrfToken);
        when(request.getHeader("X-XSRF-TOKEN")).thenReturn(null);
        when(jwtService.extractUsername(VALID_TOKEN)).thenReturn(TEST_USERNAME);
        when(userDetailsService.loadUserByUsername(TEST_USERNAME)).thenReturn(userDetails);
        when(jwtService.isTokenValid(VALID_TOKEN, userDetails)).thenReturn(true);

        // Execute
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Verify
        verify(csrfTokenRepository).loadToken(request);
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token.");
        verify(filterChain, never()).doFilter(request, response);
        
        assertNull(SecurityContextHolder.getContext().getAuthentication(),
            "Authentication should NOT be set in SecurityContext");
    }

    @Test
    @DisplayName("Should reject POST request with mismatched CSRF token")
    void shouldRejectPostWithMismatchedCsrfToken() throws Exception {
        logger.debug("Testing rejection of POST request with mismatched CSRF token");
        
        // Setup
        when(request.getMethod()).thenReturn(HttpMethod.POST.name());
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(csrfTokenRepository.loadToken(request)).thenReturn(csrfToken);
        when(request.getHeader("X-XSRF-TOKEN")).thenReturn("invalid-csrf-token");
        when(jwtService.extractUsername(VALID_TOKEN)).thenReturn(TEST_USERNAME);
        when(userDetailsService.loadUserByUsername(TEST_USERNAME)).thenReturn(userDetails);
        when(jwtService.isTokenValid(VALID_TOKEN, userDetails)).thenReturn(true);

        // Execute
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Verify
        verify(csrfTokenRepository).loadToken(request);
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token.");
        verify(filterChain, never()).doFilter(request, response);
        
        assertNull(SecurityContextHolder.getContext().getAuthentication(),
            "Authentication should NOT be set in SecurityContext");
    }

    @Test
    @DisplayName("Should reject POST request with missing CSRF token in repository")
    void shouldRejectPostWithMissingCsrfRepositoryToken() throws Exception {
        logger.debug("Testing rejection of POST request with missing CSRF token in repository");
        
        // Setup
        when(request.getMethod()).thenReturn(HttpMethod.POST.name());
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(csrfTokenRepository.loadToken(request)).thenReturn(null);
        when(request.getHeader("X-XSRF-TOKEN")).thenReturn(VALID_CSRF_TOKEN);
        when(jwtService.extractUsername(VALID_TOKEN)).thenReturn(TEST_USERNAME);
        when(userDetailsService.loadUserByUsername(TEST_USERNAME)).thenReturn(userDetails);
        when(jwtService.isTokenValid(VALID_TOKEN, userDetails)).thenReturn(true);

        // Execute
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Verify
        verify(csrfTokenRepository).loadToken(request);
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "Missing CSRF token");
        verify(filterChain, never()).doFilter(request, response);
        
        assertNull(SecurityContextHolder.getContext().getAuthentication(),
            "Authentication should NOT be set in SecurityContext");
    }

    @Test
    @DisplayName("Should skip CSRF check for GET request")
    void shouldSkipCsrfCheckForGet() throws Exception {
        logger.debug("Testing CSRF check is skipped for GET request");
        
        // Setup
        when(request.getMethod()).thenReturn(HttpMethod.GET.name());
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(jwtService.extractUsername(VALID_TOKEN)).thenReturn(TEST_USERNAME);
        when(userDetailsService.loadUserByUsername(TEST_USERNAME)).thenReturn(userDetails);
        when(jwtService.isTokenValid(VALID_TOKEN, userDetails)).thenReturn(true);

        // Execute
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Verify
        verify(csrfTokenRepository, never()).loadToken(any());
        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication(),
            "Authentication should be set in SecurityContext");
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
        when(request.getMethod()).thenReturn(HttpMethod.GET.name());
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(jwtService.extractUsername(VALID_TOKEN)).thenReturn(TEST_USERNAME);
        when(userDetailsService.loadUserByUsername(TEST_USERNAME))
            .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found"));

        // Execute
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Verify
        verify(filterChain).doFilter(request, response);
        verify(jwtService).extractUsername(VALID_TOKEN);
        verify(userDetailsService).loadUserByUsername(TEST_USERNAME);
        verify(jwtService, never()).isTokenValid(anyString(), any(UserDetails.class));
        
        assertNull(SecurityContextHolder.getContext().getAuthentication(),
            "No authentication should be set in SecurityContext when exception occurs");
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    @DisplayName("Should skip CSRF check for POST request when 'dev' profile is active")
    void shouldSkipCsrfCheckForPostWhenDevProfileActive() throws Exception {
        logger.debug("Testing CSRF check is skipped for POST request with dev profile");

        // Setup
        when(request.getMethod()).thenReturn(HttpMethod.POST.name());
        // Mock environment to simulate 'dev' profile
        when(environment.acceptsProfiles(Profiles.of("dev"))).thenReturn(true);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        // No CSRF header or token repository setup needed, as the check should be skipped
        when(jwtService.extractUsername(VALID_TOKEN)).thenReturn(TEST_USERNAME);
        when(userDetailsService.loadUserByUsername(TEST_USERNAME)).thenReturn(userDetails);
        when(jwtService.isTokenValid(VALID_TOKEN, userDetails)).thenReturn(true);

        // Execute
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Verify
        verify(csrfTokenRepository, never()).loadToken(any()); // Ensure CSRF repo wasn't accessed
        verify(response, never()).sendError(anyInt(), anyString()); // Ensure no error was sent
        verify(filterChain).doFilter(request, response); // Ensure filter chain continued
        assertNotNull(SecurityContextHolder.getContext().getAuthentication(),
            "Authentication should be set in SecurityContext");
    }
} 
