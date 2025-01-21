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
} 
