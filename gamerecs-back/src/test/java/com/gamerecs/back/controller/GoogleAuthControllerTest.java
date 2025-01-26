package com.gamerecs.back.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.gamerecs.back.service.OAuth2UserService;
import com.gamerecs.back.service.GoogleOAuth2Service;
import com.gamerecs.back.service.JwtService;
import com.gamerecs.back.config.OAuth2AuthenticationSuccessHandler;
import com.gamerecs.back.config.TestSecurityConfig;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

@WebMvcTest(GoogleAuthController.class)
@Import(TestSecurityConfig.class)
class GoogleAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private OAuth2UserService oAuth2UserService;

    @MockBean
    private GoogleOAuth2Service googleOAuth2Service;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @MockBean
    private ClientRegistrationRepository clientRegistrationRepository;

    private static final String REDIRECT_URI = "http://localhost:4200/auth/google/callback";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .build();
    }

    @Test
    @DisplayName("Should handle successful Google callback with authorization code")
    void whenGoogleCallback_thenRedirectToFrontend() throws Exception {
        // Given
        String testCode = "test_authorization_code";
        String testState = "test_state";
        String testAccessToken = "test_access_token";
        String testEmail = "test@example.com";
        String testJwtToken = "test.jwt.token";

        OAuth2User mockOAuth2User = mock(OAuth2User.class);
        when(mockOAuth2User.getAttribute("email")).thenReturn(testEmail);

        when(googleOAuth2Service.exchangeAuthorizationCode(testCode))
            .thenReturn(testAccessToken);
        when(googleOAuth2Service.getUserInfo(testAccessToken))
            .thenReturn(mockOAuth2User);
        when(jwtService.generateToken(testEmail))
            .thenReturn(testJwtToken);

        // When & Then
        mockMvc.perform(get("/api/auth/google/callback")
                .param("code", testCode)
                .param("state", testState))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URI + "?token=" + testJwtToken));
    }

    @Test
    @DisplayName("Should handle Google callback with error")
    void whenGoogleCallbackWithError_thenRedirectToFrontendWithError() throws Exception {
        // Given
        String error = "access_denied";

        // When & Then
        mockMvc.perform(get("/api/auth/google/callback")
                .param("error", error))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URI + "?error=" + error));
    }

    @Test
    @DisplayName("Should handle Google callback with missing authorization code")
    void whenGoogleCallbackWithoutCode_thenRedirectToFrontendWithError() throws Exception {
        // Given
        String testState = "test_state";

        // When & Then
        mockMvc.perform(get("/api/auth/google/callback")
                .param("state", testState))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URI + "?error=no_code"));
    }

    @Test
    @DisplayName("Should handle failed token exchange")
    void whenTokenExchangeFails_thenRedirectToFrontendWithError() throws Exception {
        // Given
        String testCode = "invalid_code";
        String testState = "test_state";

        when(googleOAuth2Service.exchangeAuthorizationCode(testCode))
            .thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/auth/google/callback")
                .param("code", testCode)
                .param("state", testState))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URI + "?error=token_exchange_failed"));
    }

    @Test
    @DisplayName("Should handle failed user info fetch")
    void whenUserInfoFetchFails_thenRedirectToFrontendWithError() throws Exception {
        // Given
        String testCode = "test_code";
        String testState = "test_state";
        String testAccessToken = "test_access_token";

        when(googleOAuth2Service.exchangeAuthorizationCode(testCode))
            .thenReturn(testAccessToken);
        when(googleOAuth2Service.getUserInfo(testAccessToken))
            .thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/auth/google/callback")
                .param("code", testCode)
                .param("state", testState))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URI + "?error=user_info_failed"));
    }

    @Test
    @DisplayName("Should handle missing email in user info")
    void whenEmailMissing_thenRedirectToFrontendWithError() throws Exception {
        // Given
        String testCode = "test_code";
        String testState = "test_state";
        String testAccessToken = "test_access_token";

        OAuth2User mockOAuth2User = mock(OAuth2User.class);
        when(mockOAuth2User.getAttribute("email")).thenReturn(null);

        when(googleOAuth2Service.exchangeAuthorizationCode(testCode))
            .thenReturn(testAccessToken);
        when(googleOAuth2Service.getUserInfo(testAccessToken))
            .thenReturn(mockOAuth2User);

        // When & Then
        mockMvc.perform(get("/api/auth/google/callback")
                .param("code", testCode)
                .param("state", testState))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URI + "?error=no_email"));
    }
} 
