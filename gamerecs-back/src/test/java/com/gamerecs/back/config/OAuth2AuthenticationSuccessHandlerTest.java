package com.gamerecs.back.config;

import com.gamerecs.back.service.JwtService;
import com.gamerecs.back.service.OAuth2UserService;
import com.gamerecs.back.model.User;
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
import static org.mockito.Mockito.lenient;

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
    private static final String TEST_USERNAME = "test";
    private static final String TEST_TOKEN = "test.jwt.token";
    private static final String TEST_REDIRECT_URI = "http://localhost:4200/oauth2/redirect";

    @BeforeEach
    void setUp() {
        logger.debug("Setting up test environment");
        
        successHandler = new OAuth2AuthenticationSuccessHandler(jwtService, oAuth2UserService);
        ReflectionTestUtils.setField(successHandler, "redirectUri", TEST_REDIRECT_URI);
        ReflectionTestUtils.setField(successHandler, "redirectStrategy", redirectStrategy);
        
        // Common setup for all tests
        lenient().when(response.isCommitted()).thenReturn(false);
        lenient().when(authentication.getPrincipal()).thenReturn(oauth2User);
    }

    @Test
    @DisplayName("Should handle OAuth2 authentication success correctly")
    void shouldHandleAuthenticationSuccess() throws Exception {
        logger.debug("Testing OAuth2 authentication success handling");
        
        User mockUser = User.builder()
            .email(TEST_EMAIL)
            .username(TEST_USERNAME)
            .build();
        when(oAuth2UserService.processOAuth2User(oauth2User)).thenReturn(mockUser);
        when(jwtService.generateToken(TEST_USERNAME)).thenReturn(TEST_TOKEN);

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(jwtService).generateToken(TEST_USERNAME);
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
    @DisplayName("Should handle OAuth2 user processing failure")
    void shouldHandleMissingEmail() throws Exception {
        logger.debug("Testing handling of OAuth2 user processing failure");
        
        when(oAuth2UserService.processOAuth2User(oauth2User)).thenReturn(null);

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendError(
            HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "Failed to process user data"
        );
        verify(jwtService, never()).generateToken(anyString());
        verify(redirectStrategy, never()).sendRedirect(any(), any(), anyString());
    }

    @Test
    @DisplayName("Should handle OAuth2User processing error")
    void shouldHandleOAuth2UserProcessingError() throws Exception {
        logger.debug("Testing handling of OAuth2User processing error");
        
        doThrow(new RuntimeException("Processing failed"))
            .when(oAuth2UserService)
            .processOAuth2User(oauth2User);

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendError(
            HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "Authentication failed"
        );
        verify(jwtService, never()).generateToken(anyString());
        verify(redirectStrategy, never()).sendRedirect(any(), any(), anyString());
    }

    @Test
    @DisplayName("Should handle JWT generation error")
    void shouldHandleJwtGenerationError() throws Exception {
        logger.debug("Testing handling of JWT generation error");
        
        User mockUser = User.builder()
            .email(TEST_EMAIL)
            .username(TEST_USERNAME)
            .build();
        when(oAuth2UserService.processOAuth2User(oauth2User)).thenReturn(mockUser);
        when(jwtService.generateToken(TEST_USERNAME))
            .thenThrow(new IllegalStateException("JWT generation failed"));

        successHandler.onAuthenticationSuccess(request, response, authentication);

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
        
        User mockUser = User.builder()
            .email(TEST_EMAIL)
            .username(TEST_USERNAME)
            .build();
        when(oAuth2UserService.processOAuth2User(oauth2User)).thenReturn(mockUser);
        when(jwtService.generateToken(TEST_USERNAME)).thenReturn(TEST_TOKEN);
        doThrow(new RuntimeException("Unexpected error"))
            .when(redirectStrategy)
            .sendRedirect(any(), any(), anyString());

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendError(
            HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "Authentication failed"
        );
    }

    @Test
    @DisplayName("Should handle null authentication principal")
    void shouldHandleNullAuthenticationPrincipal() throws Exception {
        logger.debug("Testing handling of null authentication principal");
        
        when(authentication.getPrincipal()).thenReturn(null);

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendError(
            HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "Authentication failed"
        );
        verify(jwtService, never()).generateToken(anyString());
        verify(redirectStrategy, never()).sendRedirect(any(), any(), anyString());
    }
} 
