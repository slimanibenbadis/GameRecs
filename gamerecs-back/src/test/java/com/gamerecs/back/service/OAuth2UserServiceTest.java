package com.gamerecs.back.service;

import com.gamerecs.back.model.User;
import com.gamerecs.back.repository.UserRepository;
import com.gamerecs.back.util.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OAuth2UserServiceTest extends BaseUnitTest {
    private static final Logger logger = LoggerFactory.getLogger(OAuth2UserServiceTest.class);

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    private OAuth2UserService oAuth2UserService;
    private OAuth2User defaultOAuth2User;
    private Map<String, Object> attributes;

    @BeforeEach
    void setUp() {
        logger.debug("Setting up test environment");
        
        // Set up attributes
        attributes = new HashMap<>();
        attributes.put("email", "test@example.com");
        attributes.put("name", "Test User");
        attributes.put("picture", "http://example.com/pic.jpg");
        attributes.put("sub", "google123"); // Google ID

        // Create default OAuth2User with standard attributes
        defaultOAuth2User = new DefaultOAuth2User(
            Collections.singleton(new SimpleGrantedAuthority("SCOPE_read")),
            attributes,
            "email"
        );

        // Create service that returns our defaultOAuth2User
        oAuth2UserService = new OAuth2UserService(userRepository, jwtService) {
            @Override
            public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
                try {
                    processOAuth2User(defaultOAuth2User);
                    return defaultOAuth2User;
                } catch (Exception ex) {
                    throw new OAuth2AuthenticationException(ex.getMessage());
                }
            }
        };
    }

    @Test
    @DisplayName("Should process new OAuth2 user successfully")
    void shouldProcessNewOAuth2User() {
        logger.debug("Testing processing of new OAuth2 user");
        
        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        OAuth2User result = oAuth2UserService.loadUser(mock(OAuth2UserRequest.class));

        assertNotNull(result);
        verify(userRepository).findByGoogleId("google123");
        verify(userRepository).findByEmail("test@example.com");
        verify(userRepository).save(any(User.class));
        
        // Verify the saved user has the correct attributes
        verify(userRepository).save(argThat(user -> 
            user.getEmail().equals("test@example.com") &&
            user.getUsername().equals("test") &&
            user.getProfilePictureUrl().equals("http://example.com/pic.jpg") &&
            user.getGoogleId().equals("google123") &&
            user.isEmailVerified() // OAuth2 users should be pre-verified
        ));
    }

    @Test
    @DisplayName("Should update existing OAuth2 user found by Google ID")
    void shouldUpdateExistingOAuth2UserByGoogleId() {
        logger.debug("Testing update of existing OAuth2 user by Google ID");
        
        User existingUser = User.builder()
                .email("test@example.com")
                .username("test")
                .googleId("google123")
                .build();

        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        OAuth2User result = oAuth2UserService.loadUser(mock(OAuth2UserRequest.class));

        assertNotNull(result);
        verify(userRepository).findByGoogleId("google123");
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository).save(any(User.class));
        
        // Verify the user was updated correctly
        verify(userRepository).save(argThat(user -> 
            user.getEmail().equals("test@example.com") &&
            user.getUsername().equals("test") &&
            user.getProfilePictureUrl().equals("http://example.com/pic.jpg") &&
            user.getGoogleId().equals("google123")
        ));
    }

    @Test
    @DisplayName("Should update existing OAuth2 user found by email and set Google ID")
    void shouldUpdateExistingOAuth2UserByEmailAndSetGoogleId() {
        logger.debug("Testing update of existing OAuth2 user by email and setting Google ID");
        
        User existingUser = User.builder()
                .email("test@example.com")
                .username("test")
                .build();

        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        OAuth2User result = oAuth2UserService.loadUser(mock(OAuth2UserRequest.class));

        assertNotNull(result);
        verify(userRepository).findByGoogleId("google123");
        verify(userRepository).findByEmail("test@example.com");
        verify(userRepository).save(any(User.class));
        
        // Verify the user was updated correctly with Google ID
        verify(userRepository).save(argThat(user -> 
            user.getEmail().equals("test@example.com") &&
            user.getUsername().equals("test") &&
            user.getProfilePictureUrl().equals("http://example.com/pic.jpg") &&
            user.getGoogleId().equals("google123")
        ));
    }

    @Test
    @DisplayName("Should handle OAuth2 user processing error")
    void shouldHandleOAuth2UserProcessingError() {
        logger.debug("Testing error handling in OAuth2 user processing");
        
        // Create a mock delegate service that throws an exception
        DefaultOAuth2UserService mockDelegate = mock(DefaultOAuth2UserService.class);
        when(mockDelegate.loadUser(any())).thenThrow(new RuntimeException("Processing error"));
        
        OAuth2UserService serviceWithMockDelegate = new OAuth2UserService(userRepository, jwtService) {
            @Override
            protected DefaultOAuth2UserService getDelegate() {
                return mockDelegate;
            }
        };

        OAuth2UserRequest mockRequest = mock(OAuth2UserRequest.class);
        
        OAuth2AuthenticationException thrown = assertThrows(
            OAuth2AuthenticationException.class,
            () -> serviceWithMockDelegate.loadUser(mockRequest)
        );
        
        assertEquals("Processing error", thrown.getMessage());
    }
} 
