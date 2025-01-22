package com.gamerecs.back.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.client.RestTemplate;
import com.gamerecs.back.util.BaseUnitTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class GoogleOAuth2ServiceTest extends BaseUnitTest {

    @Mock
    private RestTemplate restTemplate;

    private GoogleOAuth2Service googleOAuth2Service;
    private static final String TEST_CLIENT_ID = "test-client-id";
    private static final String TEST_CLIENT_SECRET = "test-client-secret";
    private static final String TEST_REDIRECT_URI = "http://localhost:8080/callback";
    private static final String TEST_ACCESS_TOKEN = "test-access-token";
    private static final String TEST_AUTH_CODE = "test-auth-code";

    @BeforeEach
    void setUp() {
        googleOAuth2Service = new GoogleOAuth2Service(
            restTemplate,
            TEST_CLIENT_ID,
            TEST_CLIENT_SECRET,
            TEST_REDIRECT_URI
        );
    }

    @Test
    @DisplayName("Should successfully exchange authorization code for access token")
    void shouldExchangeAuthorizationCode() {
        // Given
        String successResponse = """
            {
                "access_token": "%s",
                "expires_in": 3600,
                "scope": "email profile",
                "token_type": "Bearer"
            }
            """.formatted(TEST_ACCESS_TOKEN);

        when(restTemplate.exchange(
            eq("https://oauth2.googleapis.com/token"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(new ResponseEntity<>(successResponse, HttpStatus.OK));

        // When
        String result = googleOAuth2Service.exchangeAuthorizationCode(TEST_AUTH_CODE);

        // Then
        assertNotNull(result);
        assertEquals(TEST_ACCESS_TOKEN, result);
    }

    @Test
    @DisplayName("Should return null when token exchange fails")
    void shouldReturnNullOnTokenExchangeFailure() {
        // Given
        when(restTemplate.exchange(
            eq("https://oauth2.googleapis.com/token"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(new ResponseEntity<>(HttpStatus.BAD_REQUEST));

        // When
        String result = googleOAuth2Service.exchangeAuthorizationCode(TEST_AUTH_CODE);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should successfully fetch user info")
    void shouldFetchUserInfo() {
        // Given
        String userInfoResponse = """
            {
                "sub": "12345",
                "email": "test@example.com",
                "name": "Test User",
                "picture": "http://example.com/pic.jpg"
            }
            """;

        when(restTemplate.exchange(
            eq("https://www.googleapis.com/oauth2/v3/userinfo"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(new ResponseEntity<>(userInfoResponse, HttpStatus.OK));

        // When
        OAuth2User result = googleOAuth2Service.getUserInfo(TEST_ACCESS_TOKEN);

        // Then
        assertNotNull(result);
        assertEquals("test@example.com", result.getAttribute("email"));
        assertEquals("Test User", result.getAttribute("name"));
        assertEquals("12345", result.getAttribute("sub"));
        assertEquals("http://example.com/pic.jpg", result.getAttribute("picture"));
    }

    @Test
    @DisplayName("Should return null when user info fetch fails")
    void shouldReturnNullOnUserInfoFailure() {
        // Given
        when(restTemplate.exchange(
            eq("https://www.googleapis.com/oauth2/v3/userinfo"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(new ResponseEntity<>(HttpStatus.BAD_REQUEST));

        // When
        OAuth2User result = googleOAuth2Service.getUserInfo(TEST_ACCESS_TOKEN);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should return null when user info response is invalid")
    void shouldReturnNullOnInvalidUserInfoResponse() {
        // Given
        String invalidResponse = "invalid json";

        when(restTemplate.exchange(
            eq("https://www.googleapis.com/oauth2/v3/userinfo"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(new ResponseEntity<>(invalidResponse, HttpStatus.OK));

        // When
        OAuth2User result = googleOAuth2Service.getUserInfo(TEST_ACCESS_TOKEN);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should handle missing fields in user info response")
    void shouldHandleMissingFields() {
        // Given
        String incompleteResponse = """
            {
                "sub": "12345",
                "email": "test@example.com"
            }
            """;

        when(restTemplate.exchange(
            eq("https://www.googleapis.com/oauth2/v3/userinfo"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(new ResponseEntity<>(incompleteResponse, HttpStatus.OK));

        // When
        OAuth2User result = googleOAuth2Service.getUserInfo(TEST_ACCESS_TOKEN);

        // Then
        assertNotNull(result);
        assertEquals("test@example.com", result.getAttribute("email"));
        assertEquals("12345", result.getAttribute("sub"));
        assertEquals("", result.getAttribute("name"));
        assertEquals("", result.getAttribute("picture"));
    }

    @Test
    @DisplayName("Should successfully fetch user details")
    void shouldFetchUserDetails() {
        // Given
        String userInfoResponse = """
            {
                "email": "test@example.com",
                "name": "Test User",
                "picture": "http://example.com/pic.jpg"
            }
            """;

        when(restTemplate.exchange(
            eq("https://www.googleapis.com/oauth2/v3/userinfo"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(new ResponseEntity<>(userInfoResponse, HttpStatus.OK));

        // When
        GoogleOAuth2Service.GoogleUserInfo result = googleOAuth2Service.fetchUserDetails(TEST_ACCESS_TOKEN);

        // Then
        assertNotNull(result);
        assertEquals("test@example.com", result.email());
        assertEquals("Test User", result.name());
        assertEquals("http://example.com/pic.jpg", result.picture());
    }

    @Test
    @DisplayName("Should throw RuntimeException when user details fetch fails")
    void shouldThrowExceptionWhenUserDetailsFetchFails() {
        // Given
        when(restTemplate.exchange(
            eq("https://www.googleapis.com/oauth2/v3/userinfo"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)
        )).thenThrow(new RuntimeException("API Error"));

        // When & Then
        RuntimeException thrown = assertThrows(
            RuntimeException.class,
            () -> googleOAuth2Service.fetchUserDetails(TEST_ACCESS_TOKEN)
        );
        assertTrue(thrown.getMessage().contains("Failed to fetch user details from Google"));
    }

    @Test
    @DisplayName("Should handle malformed token exchange response")
    void shouldHandleMalformedTokenExchangeResponse() {
        // Given
        String malformedResponse = "invalid json";

        when(restTemplate.exchange(
            eq("https://oauth2.googleapis.com/token"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(new ResponseEntity<>(malformedResponse, HttpStatus.OK));

        // When
        String result = googleOAuth2Service.exchangeAuthorizationCode(TEST_AUTH_CODE);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should handle null response body in token exchange")
    void shouldHandleNullResponseBodyInTokenExchange() {
        // Given
        when(restTemplate.exchange(
            eq("https://oauth2.googleapis.com/token"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        // When
        String result = googleOAuth2Service.exchangeAuthorizationCode(TEST_AUTH_CODE);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should handle missing access token in token exchange response")
    void shouldHandleMissingAccessTokenInResponse() {
        // Given
        String responseWithoutToken = """
            {
                "expires_in": 3600,
                "scope": "email profile",
                "token_type": "Bearer"
            }
            """;

        when(restTemplate.exchange(
            eq("https://oauth2.googleapis.com/token"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(new ResponseEntity<>(responseWithoutToken, HttpStatus.OK));

        // When
        String result = googleOAuth2Service.exchangeAuthorizationCode(TEST_AUTH_CODE);

        // Then
        assertNull(result);
    }
} 
