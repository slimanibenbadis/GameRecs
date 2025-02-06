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
import com.gamerecs.back.model.User;
import com.gamerecs.back.config.OAuth2AuthenticationSuccessHandler;
import com.gamerecs.back.config.TestSecurityConfig;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Autowired
    private GoogleAuthController googleAuthController;

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
    private static final String EXPECTED_STATE = "test_state";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_USERNAME = "test";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .build();
        
        // Set the expected state value in the controller
        ReflectionTestUtils.setField(googleAuthController, "expectedState", EXPECTED_STATE);
        ReflectionTestUtils.setField(googleAuthController, "redirectUri", REDIRECT_URI);
    }

    @Test
    @DisplayName("Should handle successful Google callback with authorization code")
    void whenGoogleCallback_thenRedirectToFrontend() throws Exception {
        // Given
        String testCode = "test_authorization_code";
        String testAccessToken = "test_access_token";
        String testJwtToken = "test.jwt.token";

        OAuth2User mockOAuth2User = mock(OAuth2User.class);
        when(mockOAuth2User.getAttribute("email")).thenReturn(TEST_EMAIL);

        User mockUser = User.builder()
            .email(TEST_EMAIL)
            .username(TEST_USERNAME)
            .build();

        when(googleOAuth2Service.exchangeAuthorizationCode(testCode))
            .thenReturn(testAccessToken);
        when(googleOAuth2Service.getUserInfo(testAccessToken))
            .thenReturn(mockOAuth2User);
        when(oAuth2UserService.processOAuth2User(mockOAuth2User))
            .thenReturn(mockUser);
        when(jwtService.generateToken(TEST_USERNAME))
            .thenReturn(testJwtToken);

        // When & Then
        mockMvc.perform(get("/api/auth/google/callback")
                .param("code", testCode)
                .param("state", EXPECTED_STATE))
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
        // When & Then
        mockMvc.perform(get("/api/auth/google/callback")
                .param("state", EXPECTED_STATE))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URI + "?error=no_code"));
    }

    @Test
    @DisplayName("Should handle failed token exchange")
    void whenTokenExchangeFails_thenRedirectToFrontendWithError() throws Exception {
        // Given
        String testCode = "invalid_code";

        when(googleOAuth2Service.exchangeAuthorizationCode(testCode))
            .thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/auth/google/callback")
                .param("code", testCode)
                .param("state", EXPECTED_STATE))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URI + "?error=token_exchange_failed"));
    }

    @Test
    @DisplayName("Should handle failed user info fetch")
    void whenUserInfoFetchFails_thenRedirectToFrontendWithError() throws Exception {
        // Given
        String testCode = "test_code";
        String testAccessToken = "test_access_token";

        when(googleOAuth2Service.exchangeAuthorizationCode(testCode))
            .thenReturn(testAccessToken);
        when(googleOAuth2Service.getUserInfo(testAccessToken))
            .thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/auth/google/callback")
                .param("code", testCode)
                .param("state", EXPECTED_STATE))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URI + "?error=user_info_failed"));
    }

    @Test
    @DisplayName("Should handle OAuth2 user processing failure")
    void whenEmailMissing_thenRedirectToFrontendWithError() throws Exception {
        // Given
        String testCode = "test_code";
        String testAccessToken = "test_access_token";

        OAuth2User mockOAuth2User = mock(OAuth2User.class);
        when(mockOAuth2User.getAttribute("email")).thenReturn(TEST_EMAIL);

        when(googleOAuth2Service.exchangeAuthorizationCode(testCode))
            .thenReturn(testAccessToken);
        when(googleOAuth2Service.getUserInfo(testAccessToken))
            .thenReturn(mockOAuth2User);
        when(oAuth2UserService.processOAuth2User(mockOAuth2User))
            .thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/auth/google/callback")
                .param("code", testCode)
                .param("state", EXPECTED_STATE))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URI + "?error=user_processing_failed"));
    }

    @Test
    @DisplayName("Should handle JWT generation failure")
    void whenJwtGenerationFails_thenRedirectToFrontendWithError() throws Exception {
        // Given
        String testCode = "test_code";
        String testAccessToken = "test_access_token";

        OAuth2User mockOAuth2User = mock(OAuth2User.class);
        when(mockOAuth2User.getAttribute("email")).thenReturn(TEST_EMAIL);

        User mockUser = User.builder()
            .email(TEST_EMAIL)
            .username(TEST_USERNAME)
            .build();

        when(googleOAuth2Service.exchangeAuthorizationCode(testCode))
            .thenReturn(testAccessToken);
        when(googleOAuth2Service.getUserInfo(testAccessToken))
            .thenReturn(mockOAuth2User);
        when(oAuth2UserService.processOAuth2User(mockOAuth2User))
            .thenReturn(mockUser);
        when(jwtService.generateToken(TEST_USERNAME))
            .thenThrow(new IllegalStateException("JWT generation failed"));

        // When & Then
        mockMvc.perform(get("/api/auth/google/callback")
                .param("code", testCode)
                .param("state", EXPECTED_STATE))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URI + "?error=token_generation_failed"));
    }

    @Test
    @DisplayName("Should handle malformed Google API response")
    void whenGoogleApiResponseIsMalformed_thenRedirectToFrontendWithError() throws Exception {
        // Given
        String testCode = "test_code";
        String testAccessToken = "test_access_token";

        when(googleOAuth2Service.exchangeAuthorizationCode(testCode))
            .thenReturn(testAccessToken);
        when(googleOAuth2Service.getUserInfo(testAccessToken))
            .thenThrow(new RuntimeException("Malformed response from Google API"));

        // When & Then
        mockMvc.perform(get("/api/auth/google/callback")
                .param("code", testCode)
                .param("state", EXPECTED_STATE))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URI + "?error=google_api_error"));
    }

    @Test
    @DisplayName("Should handle rate limiting from Google API")
    void whenGoogleApiRateLimited_thenRedirectToFrontendWithError() throws Exception {
        // Given
        String testCode = "test_code";

        when(googleOAuth2Service.exchangeAuthorizationCode(testCode))
            .thenThrow(new RuntimeException("Rate limit exceeded"));

        // When & Then
        mockMvc.perform(get("/api/auth/google/callback")
                .param("code", testCode)
                .param("state", EXPECTED_STATE))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URI + "?error=rate_limit_exceeded"));
    }

    @Test
    @DisplayName("Should handle invalid state parameter")
    void whenStateParameterInvalid_thenRedirectToFrontendWithError() throws Exception {
        // Given
        String testCode = "test_code";
        String invalidState = "invalid_state";

        // When & Then
        mockMvc.perform(get("/api/auth/google/callback")
                .param("code", testCode)
                .param("state", invalidState))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(REDIRECT_URI + "?error=invalid_state"));
    }
} 
