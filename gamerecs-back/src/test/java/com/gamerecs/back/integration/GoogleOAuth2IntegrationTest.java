package com.gamerecs.back.integration;

import com.gamerecs.back.model.User;
import com.gamerecs.back.repository.UserRepository;
import com.gamerecs.back.service.GoogleOAuth2Service;
import com.gamerecs.back.service.JwtService;
import com.gamerecs.back.service.OAuth2UserService;
import com.gamerecs.back.util.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GoogleOAuth2IntegrationTest extends BaseIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(GoogleOAuth2IntegrationTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private GoogleOAuth2Service googleOAuth2Service;

    @MockBean
    private OAuth2UserService oAuth2UserService;

    @Autowired
    private JwtService jwtService;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_USERNAME = "test";
    private static final String TEST_NAME = "Test User";
    private static final String TEST_PICTURE = "http://example.com/pic.jpg";
    private static final String TEST_GOOGLE_ID = "123456789";
    private static final String TEST_AUTH_CODE = "test_auth_code";
    private static final String TEST_ACCESS_TOKEN = "test_access_token";
    private static final String TEST_STATE = "test_state";

    @BeforeEach
    void setUp() {
        logger.debug("Setting up test environment");
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should complete full OAuth2 flow for new user")
    void shouldCompleteOAuth2FlowForNewUser() throws Exception {
        logger.debug("Testing complete OAuth2 flow for new user");

        // Mock Google OAuth2 service responses
        when(googleOAuth2Service.exchangeAuthorizationCode(TEST_AUTH_CODE))
            .thenReturn(TEST_ACCESS_TOKEN);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", TEST_EMAIL);
        attributes.put("name", TEST_NAME);
        attributes.put("picture", TEST_PICTURE);
        attributes.put("sub", TEST_GOOGLE_ID);

        OAuth2User oauth2User = new DefaultOAuth2User(
            Collections.emptyList(),
            attributes,
            "email"
        );

        when(googleOAuth2Service.getUserInfo(TEST_ACCESS_TOKEN))
            .thenReturn(oauth2User);

        // Mock OAuth2UserService to process the user
        User newUser = User.builder()
            .email(TEST_EMAIL)
            .username(TEST_USERNAME)
            .googleId(TEST_GOOGLE_ID)
            .profilePictureUrl(TEST_PICTURE)
            .emailVerified(true)
            .build();
        when(oAuth2UserService.processOAuth2User(oauth2User))
            .thenReturn(newUser);

        // Execute OAuth2 callback
        MvcResult result = mockMvc.perform(get("/api/auth/google/callback")
                .param("code", TEST_AUTH_CODE)
                .param("state", TEST_STATE))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(header().exists("Location"))
                .andReturn();

        // Verify redirect URL contains JWT token
        String redirectUrl = result.getResponse().getHeader("Location");
        assertTrue(redirectUrl.contains("token="));

        // Save the user to the database since we mocked the OAuth2UserService
        userRepository.save(newUser);

        // Verify user was created in database
        Optional<User> createdUser = userRepository.findByEmail(TEST_EMAIL);
        assertTrue(createdUser.isPresent());
        assertEquals(TEST_GOOGLE_ID, createdUser.get().getGoogleId());
        assertEquals(TEST_USERNAME, createdUser.get().getUsername());
        assertEquals(TEST_PICTURE, createdUser.get().getProfilePictureUrl());
        assertTrue(createdUser.get().isEmailVerified());
    }

    @Test
    @DisplayName("Should complete full OAuth2 flow for existing user")
    void shouldCompleteOAuth2FlowForExistingUser() throws Exception {
        logger.debug("Testing complete OAuth2 flow for existing user");

        // Create existing user with a custom profile picture
        String existingPictureUrl = "old_picture.jpg";
        User existingUser = User.builder()
            .email(TEST_EMAIL)
            .username(TEST_USERNAME)
            .googleId(TEST_GOOGLE_ID)
            .profilePictureUrl(existingPictureUrl)
            .emailVerified(true)
            .build();
        existingUser = userRepository.save(existingUser);

        // Mock Google OAuth2 service responses
        when(googleOAuth2Service.exchangeAuthorizationCode(TEST_AUTH_CODE))
            .thenReturn(TEST_ACCESS_TOKEN);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", TEST_EMAIL);
        attributes.put("name", TEST_NAME);
        attributes.put("picture", TEST_PICTURE); // Different picture from Google
        attributes.put("sub", TEST_GOOGLE_ID);

        OAuth2User oauth2User = new DefaultOAuth2User(
            Collections.emptyList(),
            attributes,
            "email"
        );

        when(googleOAuth2Service.getUserInfo(TEST_ACCESS_TOKEN))
            .thenReturn(oauth2User);

        // Mock OAuth2UserService to process the user - should preserve existing picture
        User updatedUser = User.builder()
            .userId(existingUser.getUserId())
            .email(TEST_EMAIL)
            .username(TEST_USERNAME)
            .googleId(TEST_GOOGLE_ID)
            .profilePictureUrl(existingPictureUrl) // Should keep existing picture
            .emailVerified(true)
            .build();
        when(oAuth2UserService.processOAuth2User(oauth2User))
            .thenReturn(updatedUser);

        // Execute OAuth2 callback
        MvcResult result = mockMvc.perform(get("/api/auth/google/callback")
                .param("code", TEST_AUTH_CODE)
                .param("state", TEST_STATE))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(header().exists("Location"))
                .andReturn();

        // Verify redirect URL contains JWT token
        String redirectUrl = result.getResponse().getHeader("Location");
        assertTrue(redirectUrl.contains("token="));

        // Save the updated user to the database since we mocked the OAuth2UserService
        userRepository.save(updatedUser);

        // Verify user was updated in database and profile picture was preserved
        Optional<User> updatedUserInDb = userRepository.findByEmail(TEST_EMAIL);
        assertTrue(updatedUserInDb.isPresent());
        assertEquals(TEST_GOOGLE_ID, updatedUserInDb.get().getGoogleId());
        assertEquals(TEST_USERNAME, updatedUserInDb.get().getUsername());
        assertEquals(existingPictureUrl, updatedUserInDb.get().getProfilePictureUrl(), 
            "Profile picture should not be updated from Google");
        assertTrue(updatedUserInDb.get().isEmailVerified());
    }

    @Test
    @DisplayName("Should handle OAuth2 flow with invalid authorization code")
    void shouldHandleInvalidAuthorizationCode() throws Exception {
        logger.debug("Testing OAuth2 flow with invalid authorization code");

        // Mock Google OAuth2 service to return null for invalid code
        when(googleOAuth2Service.exchangeAuthorizationCode(TEST_AUTH_CODE))
            .thenReturn(null);

        // Execute OAuth2 callback
        mockMvc.perform(get("/api/auth/google/callback")
                .param("code", TEST_AUTH_CODE)
                .param("state", TEST_STATE))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(redirectUri + "?error=token_exchange_failed"));
    }

    @Test
    @DisplayName("Should handle OAuth2 flow with invalid state parameter")
    void shouldHandleInvalidState() throws Exception {
        logger.debug("Testing OAuth2 flow with invalid state parameter");

        // Execute OAuth2 callback with invalid state
        mockMvc.perform(get("/api/auth/google/callback")
                .param("code", TEST_AUTH_CODE)
                .param("state", "invalid_state"))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(redirectUri + "?error=invalid_state"));
    }

    @Test
    @DisplayName("Should handle OAuth2 flow with user processing failure")
    void shouldHandleMissingEmail() throws Exception {
        logger.debug("Testing OAuth2 flow with user processing failure");

        // Mock Google OAuth2 service responses
        when(googleOAuth2Service.exchangeAuthorizationCode(TEST_AUTH_CODE))
            .thenReturn(TEST_ACCESS_TOKEN);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", TEST_EMAIL);
        attributes.put("name", TEST_NAME);
        attributes.put("picture", TEST_PICTURE);
        attributes.put("sub", TEST_GOOGLE_ID);

        OAuth2User oauth2User = new DefaultOAuth2User(
            Collections.emptyList(),
            attributes,
            "email"
        );

        when(googleOAuth2Service.getUserInfo(TEST_ACCESS_TOKEN))
            .thenReturn(oauth2User);
            
        when(oAuth2UserService.processOAuth2User(oauth2User))
            .thenReturn(null);

        // Execute OAuth2 callback
        mockMvc.perform(get("/api/auth/google/callback")
                .param("code", TEST_AUTH_CODE)
                .param("state", TEST_STATE))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(redirectUri + "?error=user_processing_failed"));
    }

    @Test
    @DisplayName("Should handle OAuth2 flow with failed user info fetch")
    void shouldHandleFailedUserInfoFetch() throws Exception {
        logger.debug("Testing OAuth2 flow with failed user info fetch");

        // Mock Google OAuth2 service responses
        when(googleOAuth2Service.exchangeAuthorizationCode(TEST_AUTH_CODE))
            .thenReturn(TEST_ACCESS_TOKEN);

        when(googleOAuth2Service.getUserInfo(TEST_ACCESS_TOKEN))
            .thenReturn(null);

        // Execute OAuth2 callback
        mockMvc.perform(get("/api/auth/google/callback")
                .param("code", TEST_AUTH_CODE)
                .param("state", TEST_STATE))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(redirectUri + "?error=user_info_failed"));
    }
} 
