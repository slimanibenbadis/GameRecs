package com.gamerecs.back.config;

import com.gamerecs.back.service.JwtService;
import com.gamerecs.back.service.OAuth2UserService;
import com.gamerecs.back.util.BaseUnitTest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OAuth2AuthenticationSuccessHandlerTest extends BaseUnitTest {
    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandlerTest.class);

    @Mock
    private JwtService jwtService;

    @Mock
    private OAuth2UserService oAuth2UserService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oauth2User;

    @Mock
    private DefaultRedirectStrategy redirectStrategy;

    private OAuth2AuthenticationSuccessHandler successHandler;
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_TOKEN = "test.jwt.token";
    private static final String TEST_REDIRECT_URI = "http://localhost:4200/oauth2/redirect";

    @BeforeEach
    void setUp() {
        logger.debug("Setting up test environment");
        
        successHandler = new OAuth2AuthenticationSuccessHandler(jwtService, oAuth2UserService);
        ReflectionTestUtils.setField(successHandler, "redirectUri", TEST_REDIRECT_URI);
        ReflectionTestUtils.setField(successHandler, "redirectStrategy", redirectStrategy);
    }

    @Test
    @DisplayName("Should handle OAuth2 authentication success correctly")
    void shouldHandleAuthenticationSuccess() throws Exception {
        logger.debug("Testing OAuth2 authentication success handling");
        
        // Setup specific test stubbings
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("email")).thenReturn(TEST_EMAIL);
        when(jwtService.generateToken(TEST_EMAIL)).thenReturn(TEST_TOKEN);
        when(response.isCommitted()).thenReturn(false);

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(jwtService).generateToken(TEST_EMAIL);
        verify(redirectStrategy).sendRedirect(
            eq(request),
            eq(response),
            contains(TEST_REDIRECT_URI + "?token=" + TEST_TOKEN)
        );
    }

    @Test
    @DisplayName("Should not redirect if response is already committed")
    void shouldNotRedirectIfResponseCommitted() throws Exception {
        logger.debug("Testing behavior when response is already committed");
        
        when(response.isCommitted()).thenReturn(true);

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(redirectStrategy, never()).sendRedirect(any(), any(), anyString());
        verify(jwtService, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("Should handle missing email attribute")
    void shouldHandleMissingEmail() throws Exception {
        logger.debug("Testing handling of missing email attribute");
        
        // Setup
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("email")).thenReturn(null);
        when(response.isCommitted()).thenReturn(false);

        // Execute
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // Verify
        verify(response).sendError(
            HttpServletResponse.SC_BAD_REQUEST,
            "Email not found in OAuth2 user data"
        );
        verify(jwtService, never()).generateToken(anyString());
        verify(redirectStrategy, never()).sendRedirect(any(), any(), anyString());
    }

    @Test
    @DisplayName("Should handle empty email attribute")
    void shouldHandleEmptyEmail() throws Exception {
        logger.debug("Testing handling of empty email attribute");
        
        // Setup
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("email")).thenReturn("");
        when(response.isCommitted()).thenReturn(false);
        when(jwtService.generateToken(""))
            .thenThrow(new IllegalStateException("Invalid email"));

        // Execute
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // Verify
        verify(response).sendError(
            HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "Failed to generate authentication token"
        );
        verify(redirectStrategy, never()).sendRedirect(any(), any(), anyString());
    }

    @Test
    @DisplayName("Should handle OAuth2User processing error")
    void shouldHandleOAuth2UserProcessingError() throws Exception {
        logger.debug("Testing handling of OAuth2User processing error");
        
        // Setup
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(response.isCommitted()).thenReturn(false);
        doThrow(new IllegalStateException("Processing failed"))
            .when(oAuth2UserService)
            .processOAuth2User(oauth2User);

        // Execute
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // Verify
        verify(response).sendError(
            HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "Failed to generate authentication token"
        );
        verify(jwtService, never()).generateToken(anyString());
        verify(redirectStrategy, never()).sendRedirect(any(), any(), anyString());
    }

    @Test
    @DisplayName("Should handle JWT generation error")
    void shouldHandleJwtGenerationError() throws Exception {
        logger.debug("Testing handling of JWT generation error");
        
        // Setup
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("email")).thenReturn(TEST_EMAIL);
        when(response.isCommitted()).thenReturn(false);
        when(jwtService.generateToken(TEST_EMAIL))
            .thenThrow(new IllegalStateException("JWT generation failed"));

        // Execute
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // Verify
        verify(response).sendError(
            HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "Failed to generate authentication token"
        );
        verify(redirectStrategy, never()).sendRedirect(any(), any(), anyString());
    }

    @Test
    @DisplayName("Should handle unexpected runtime exception")
    void shouldHandleUnexpectedRuntimeException() throws Exception {
        logger.debug("Testing handling of unexpected runtime exception");
        
        // Setup
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("email")).thenReturn(TEST_EMAIL);
        when(response.isCommitted()).thenReturn(false);
        doThrow(new RuntimeException("Unexpected error"))
            .when(redirectStrategy)
            .sendRedirect(any(), any(), anyString());

        // Execute
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // Verify
        verify(response).sendError(
            HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "Authentication failed"
        );
    }

    @Test
    @DisplayName("Should propagate IOException during error response")
    void shouldHandleIOExceptionDuringErrorResponse() throws Exception {
        logger.debug("Testing handling of IOException during error response");
        
        // Setup
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("email")).thenReturn(TEST_EMAIL);
        when(response.isCommitted()).thenReturn(false);
        RuntimeException originalException = new RuntimeException("Original error");
        IOException responseException = new IOException("Response error");
        
        doThrow(originalException)
            .when(redirectStrategy)
            .sendRedirect(any(), any(), anyString());
        doThrow(responseException)
            .when(response)
            .sendError(anyInt(), anyString());

        // Execute and verify
        IOException thrown = assertThrows(IOException.class, () -> 
            successHandler.onAuthenticationSuccess(request, response, authentication)
        );
        
        assertEquals("Response error", thrown.getMessage());
        verify(response).sendError(
            HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "Authentication failed"
        );
    }

    @Test
    @DisplayName("Should handle null authentication principal")
    void shouldHandleNullAuthenticationPrincipal() throws Exception {
        logger.debug("Testing handling of null authentication principal");
        
        // Setup
        when(authentication.getPrincipal()).thenReturn(null);
        when(response.isCommitted()).thenReturn(false);

        // Execute
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // Verify
        verify(response).sendError(
            HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "Authentication failed"
        );
    }
} 
