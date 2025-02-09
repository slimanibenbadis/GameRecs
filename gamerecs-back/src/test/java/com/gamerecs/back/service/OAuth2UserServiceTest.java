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
    private OAuth2UserRequest userRequest;
    private DefaultOAuth2UserService mockDelegate;

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

        // Mock OAuth2UserRequest
        userRequest = mock(OAuth2UserRequest.class);

        // Create service with mocked delegate
        oAuth2UserService = spy(new OAuth2UserService(userRepository, jwtService));

        // Create and configure mock delegate service
        mockDelegate = mock(DefaultOAuth2UserService.class);
    }

    @Test
    @DisplayName("Should process new OAuth2 user successfully")
    void shouldProcessNewOAuth2User() {
        logger.debug("Testing processing of new OAuth2 user");
        
        // Set up delegate behavior
        when(mockDelegate.loadUser(any(OAuth2UserRequest.class))).thenReturn(defaultOAuth2User);
        doReturn(mockDelegate).when(oAuth2UserService).getDelegate();
        
        // Mock repository behavior
        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        
        // Mock save to return a user with ID
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(1L); // Set a mock ID
            return user;
        });
        
        // Mock findById for verification
        when(userRepository.findById(1L)).thenAnswer(invocation -> 
            Optional.of(User.builder()
                .userId(1L)
                .email("test@example.com")
                .username("test")
                .googleId("google123")
                .profilePictureUrl("http://example.com/pic.jpg")
                .emailVerified(true)
                .build())
        );

        OAuth2User result = oAuth2UserService.loadUser(userRequest);

        assertNotNull(result);
        assertEquals(defaultOAuth2User, result);
        verify(userRepository).findByGoogleId("google123");
        verify(userRepository).findByEmail("test@example.com");
        verify(userRepository).save(any(User.class));
        verify(userRepository).findById(1L);
        
        // Verify the saved user has the correct attributes
        verify(userRepository).save(argThat(user -> 
            user.getEmail().equals("test@example.com") &&
            user.getUsername().startsWith("test") &&
            user.getProfilePictureUrl().equals("http://example.com/pic.jpg") &&
            user.getGoogleId().equals("google123") &&
            user.isEmailVerified()
        ));
    }

    @Test
    @DisplayName("Should update existing OAuth2 user found by Google ID")
    void shouldUpdateExistingOAuth2UserByGoogleId() {
        logger.debug("Testing update of existing OAuth2 user by Google ID");
        
        // Set up delegate behavior
        when(mockDelegate.loadUser(any(OAuth2UserRequest.class))).thenReturn(defaultOAuth2User);
        doReturn(mockDelegate).when(oAuth2UserService).getDelegate();
        
        User existingUser = User.builder()
                .userId(1L)
                .email("test@example.com")
                .username("test")
                .googleId("google123")
                .build();

        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        OAuth2User result = oAuth2UserService.loadUser(userRequest);

        assertNotNull(result);
        assertEquals(defaultOAuth2User, result);
        verify(userRepository).findByGoogleId("google123");
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository).save(any(User.class));
        verify(userRepository).findById(1L);
        
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
        
        // Set up delegate behavior
        when(mockDelegate.loadUser(any(OAuth2UserRequest.class))).thenReturn(defaultOAuth2User);
        doReturn(mockDelegate).when(oAuth2UserService).getDelegate();
        
        User existingUser = User.builder()
                .userId(1L)
                .email("test@example.com")
                .username("test")
                .build();

        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        OAuth2User result = oAuth2UserService.loadUser(userRequest);

        assertNotNull(result);
        assertEquals(defaultOAuth2User, result);
        verify(userRepository).findByGoogleId("google123");
        verify(userRepository).findByEmail("test@example.com");
        verify(userRepository).save(any(User.class));
        verify(userRepository).findById(1L);
        
        // Verify the user was updated correctly with Google ID
        verify(userRepository).save(argThat(user -> 
            user.getEmail().equals("test@example.com") &&
            user.getUsername().equals("test") &&
            user.getProfilePictureUrl().equals("http://example.com/pic.jpg") &&
            user.getGoogleId().equals("google123")
        ));
    }

    @Test
    @DisplayName("Should handle missing required attributes")
    void shouldHandleMissingRequiredAttributes() {
        logger.debug("Testing handling of missing required attributes");
        
        // Create OAuth2User without email
        Map<String, Object> incompleteAttributes = new HashMap<>();
        incompleteAttributes.put("sub", "google123");
        
        OAuth2User incompleteOAuth2User = new DefaultOAuth2User(
            Collections.singleton(new SimpleGrantedAuthority("SCOPE_read")),
            incompleteAttributes,
            "sub"
        );
        
        // Set up delegate behavior to return incomplete OAuth2User
        when(mockDelegate.loadUser(any(OAuth2UserRequest.class))).thenReturn(incompleteOAuth2User);
        doReturn(mockDelegate).when(oAuth2UserService).getDelegate();

        assertThrows(OAuth2AuthenticationException.class, () -> 
            oAuth2UserService.loadUser(userRequest)
        );
    }

    @Test
    @DisplayName("Should handle OAuth2 user processing error")
    void shouldHandleOAuth2UserProcessingError() {
        logger.debug("Testing error handling in OAuth2 user processing");
        
        // Set up delegate behavior to throw exception
        when(mockDelegate.loadUser(any(OAuth2UserRequest.class)))
            .thenThrow(new OAuth2AuthenticationException("test error"));
        doReturn(mockDelegate).when(oAuth2UserService).getDelegate();

        assertThrows(OAuth2AuthenticationException.class, () -> 
            oAuth2UserService.loadUser(userRequest)
        );
    }

    @Test
    @DisplayName("Should handle generic runtime exception during user processing")
    void shouldHandleGenericRuntimeException() {
        logger.debug("Testing handling of generic runtime exception");
        
        // Set up delegate behavior to return valid OAuth2User
        when(mockDelegate.loadUser(any(OAuth2UserRequest.class))).thenReturn(defaultOAuth2User);
        doReturn(mockDelegate).when(oAuth2UserService).getDelegate();
        
        // Mock repository to throw RuntimeException
        when(userRepository.findByGoogleId(anyString()))
            .thenThrow(new RuntimeException("Database connection failed"));

        OAuth2AuthenticationException exception = assertThrows(
            OAuth2AuthenticationException.class,
            () -> oAuth2UserService.loadUser(userRequest)
        );

        assertEquals("processing_error", exception.getError().getErrorCode());
        assertTrue(exception.getError().getDescription().contains("Database connection failed"));
    }

    @Test
    @DisplayName("Should handle null OAuth2User from delegate")
    void shouldHandleNullOAuth2User() {
        logger.debug("Testing handling of null OAuth2User from delegate");
        
        // Set up delegate behavior to return null
        when(mockDelegate.loadUser(any(OAuth2UserRequest.class))).thenReturn(null);
        doReturn(mockDelegate).when(oAuth2UserService).getDelegate();

        OAuth2AuthenticationException exception = assertThrows(
            OAuth2AuthenticationException.class,
            () -> oAuth2UserService.loadUser(userRequest)
        );

        logger.debug("Actual error description: {}", exception.getError().getDescription());
        assertEquals("processing_error", exception.getError().getErrorCode());
        assertTrue(exception.getError().getDescription().contains("Cannot invoke \"org.springframework.security.oauth2.core.user.OAuth2User.getAttributes()\" because \"oauth2User\" is null"));
    }

    @Test
    @DisplayName("Should handle database save failure")
    void shouldHandleDatabaseSaveFailure() {
        logger.debug("Testing handling of database save failure");
        
        // Set up delegate behavior
        when(mockDelegate.loadUser(any(OAuth2UserRequest.class))).thenReturn(defaultOAuth2User);
        doReturn(mockDelegate).when(oAuth2UserService).getDelegate();
        
        // Mock repository behavior for new user
        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        
        // Mock save to throw exception
        when(userRepository.save(any(User.class)))
            .thenThrow(new RuntimeException("Failed to save user"));

        OAuth2AuthenticationException exception = assertThrows(
            OAuth2AuthenticationException.class,
            () -> oAuth2UserService.loadUser(userRequest)
        );

        assertEquals("processing_error", exception.getError().getErrorCode());
        assertTrue(exception.getError().getDescription().contains("Failed to save user"));
    }

    @Test
    @DisplayName("Should handle verification failure after save")
    void shouldHandleVerificationFailure() {
        logger.debug("Testing handling of verification failure after save");
        
        // Set up delegate behavior
        when(mockDelegate.loadUser(any(OAuth2UserRequest.class))).thenReturn(defaultOAuth2User);
        doReturn(mockDelegate).when(oAuth2UserService).getDelegate();
        
        // Mock repository behavior for new user
        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        
        // Mock save to return user but findById to fail
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(1L);
            return user;
        });
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        OAuth2AuthenticationException exception = assertThrows(
            OAuth2AuthenticationException.class,
            () -> oAuth2UserService.loadUser(userRequest)
        );

        assertEquals("processing_error", exception.getError().getErrorCode());
        assertTrue(exception.getError().getDescription().contains("User not found after save operation"));
    }

    @Test
    @DisplayName("Should create new instance of DefaultOAuth2UserService")
    void shouldCreateNewDelegateInstance() {
        logger.debug("Testing creation of new DefaultOAuth2UserService delegate instance");
        
        // When
        DefaultOAuth2UserService delegate = oAuth2UserService.getDelegate();

        // Then
        assertNotNull(delegate, "Delegate instance should not be null");
        assertTrue(delegate instanceof DefaultOAuth2UserService, "Delegate should be instance of DefaultOAuth2UserService");
        
        // Create another instance to verify each call creates a new object
        DefaultOAuth2UserService anotherDelegate = oAuth2UserService.getDelegate();
        assertNotSame(delegate, anotherDelegate, "Each call should create a new instance");
    }

    @Test
    @DisplayName("Should handle username collision and generate unique username")
    void shouldHandleUsernameCollisionAndGenerateUnique() {
        logger.debug("Testing username collision handling");
        
        // Set up delegate behavior
        when(mockDelegate.loadUser(any(OAuth2UserRequest.class))).thenReturn(defaultOAuth2User);
        doReturn(mockDelegate).when(oAuth2UserService).getDelegate();
        
        // Mock repository to simulate username collision
        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("test"))
            .thenReturn(Optional.of(User.builder().username("test").build()));
        when(userRepository.findByUsername("test1"))
            .thenReturn(Optional.of(User.builder().username("test1").build()));
        when(userRepository.findByUsername("test2"))
            .thenReturn(Optional.empty());
            
        // Mock save behavior
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(1L);
            return user;
        });
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().userId(1L).build()));

        OAuth2User result = oAuth2UserService.loadUser(userRequest);

        assertNotNull(result);
        verify(userRepository).save(argThat(user -> 
            user.getUsername().equals("test2")
        ));
    }

    @Test
    @DisplayName("Should handle empty profile picture URL")
    void shouldHandleEmptyProfilePictureUrl() {
        logger.debug("Testing handling of empty profile picture URL");
        
        // Remove picture from attributes
        attributes.remove("picture");
        defaultOAuth2User = new DefaultOAuth2User(
            Collections.singleton(new SimpleGrantedAuthority("SCOPE_read")),
            attributes,
            "email"
        );
        
        // Set up delegate behavior
        when(mockDelegate.loadUser(any(OAuth2UserRequest.class))).thenReturn(defaultOAuth2User);
        doReturn(mockDelegate).when(oAuth2UserService).getDelegate();
        
        // Mock repository behavior for new user
        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(any())).thenReturn(Optional.of(User.builder().build()));

        OAuth2User result = oAuth2UserService.loadUser(userRequest);

        assertNotNull(result);
        verify(userRepository).save(argThat(user -> 
            user.getProfilePictureUrl() == null
        ));
    }

    @Test
    @DisplayName("Should handle empty name attribute")
    void shouldHandleEmptyName() {
        logger.debug("Testing handling of empty name attribute");
        
        // Remove name from attributes
        attributes.remove("name");
        defaultOAuth2User = new DefaultOAuth2User(
            Collections.singleton(new SimpleGrantedAuthority("SCOPE_read")),
            attributes,
            "email"
        );
        
        // Set up delegate behavior
        when(mockDelegate.loadUser(any(OAuth2UserRequest.class))).thenReturn(defaultOAuth2User);
        doReturn(mockDelegate).when(oAuth2UserService).getDelegate();
        
        // Mock repository behavior
        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(any())).thenReturn(Optional.of(User.builder().build()));

        OAuth2User result = oAuth2UserService.loadUser(userRequest);

        assertNotNull(result);
        // Verify user is created successfully even without name
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should handle special characters in email for username generation")
    void shouldHandleSpecialCharactersInEmail() {
        logger.debug("Testing username generation with special characters in email");
        
        // Update attributes with email containing special characters
        attributes.put("email", "test.user+label@example.com");
        defaultOAuth2User = new DefaultOAuth2User(
            Collections.singleton(new SimpleGrantedAuthority("SCOPE_read")),
            attributes,
            "email"
        );
        
        // Set up delegate behavior
        when(mockDelegate.loadUser(any(OAuth2UserRequest.class))).thenReturn(defaultOAuth2User);
        doReturn(mockDelegate).when(oAuth2UserService).getDelegate();
        
        // Mock repository behavior
        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test.user+label@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(any())).thenReturn(Optional.of(User.builder().build()));

        OAuth2User result = oAuth2UserService.loadUser(userRequest);

        assertNotNull(result);
        verify(userRepository).save(argThat(user -> 
            user.getUsername().equals("test_user_label")
        ));
    }

    @Test
    @DisplayName("Should handle concurrent user creation")
    void shouldHandleConcurrentUserCreation() {
        logger.debug("Testing handling of concurrent user creation");
        
        // Set up delegate behavior
        when(mockDelegate.loadUser(any(OAuth2UserRequest.class))).thenReturn(defaultOAuth2User);
        doReturn(mockDelegate).when(oAuth2UserService).getDelegate();
        
        // Mock repository to simulate race condition
        when(userRepository.findByGoogleId("google123"))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(User.builder()
                .googleId("google123")
                .email("test@example.com")
                .build()));
                
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Concurrent modification"));
        
        assertThrows(OAuth2AuthenticationException.class, () -> 
            oAuth2UserService.loadUser(userRequest)
        );
        
        verify(userRepository).findByGoogleId("google123");
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("Should preserve existing profile picture for OAuth2 user")
    void shouldPreserveExistingProfilePicture() {
        logger.debug("Testing preservation of existing profile picture during OAuth2 update");
        
        // Set up delegate behavior
        when(mockDelegate.loadUser(any(OAuth2UserRequest.class))).thenReturn(defaultOAuth2User);
        doReturn(mockDelegate).when(oAuth2UserService).getDelegate();
        
        // Create existing user with a profile picture
        String existingPictureUrl = "http://example.com/existing-pic.jpg";
        User existingUser = User.builder()
                .userId(1L)
                .email("test@example.com")
                .username("test")
                .googleId("google123")
                .profilePictureUrl(existingPictureUrl)
                .build();

        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        OAuth2User result = oAuth2UserService.loadUser(userRequest);

        assertNotNull(result);
        assertEquals(defaultOAuth2User, result);
        verify(userRepository).findByGoogleId("google123");
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository).save(any(User.class));
        verify(userRepository).findById(1L);
        
        // Verify the profile picture was not updated
        verify(userRepository).save(argThat(user -> 
            user.getEmail().equals("test@example.com") &&
            user.getUsername().equals("test") &&
            user.getProfilePictureUrl().equals(existingPictureUrl) && // Should keep existing picture
            user.getGoogleId().equals("google123")
        ));
    }

    @Test
    @DisplayName("Should update empty profile picture for existing OAuth2 user")
    void shouldUpdateEmptyProfilePicture() {
        logger.debug("Testing update of empty profile picture during OAuth2 update");
        
        // Set up delegate behavior
        when(mockDelegate.loadUser(any(OAuth2UserRequest.class))).thenReturn(defaultOAuth2User);
        doReturn(mockDelegate).when(oAuth2UserService).getDelegate();
        
        // Create existing user with no profile picture
        User existingUser = User.builder()
                .userId(1L)
                .email("test@example.com")
                .username("test")
                .googleId("google123")
                .profilePictureUrl(null)
                .build();

        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        OAuth2User result = oAuth2UserService.loadUser(userRequest);

        assertNotNull(result);
        assertEquals(defaultOAuth2User, result);
        verify(userRepository).findByGoogleId("google123");
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository).save(any(User.class));
        verify(userRepository).findById(1L);
        
        // Verify the profile picture was updated since it was empty
        verify(userRepository).save(argThat(user -> 
            user.getEmail().equals("test@example.com") &&
            user.getUsername().equals("test") &&
            user.getProfilePictureUrl().equals("http://example.com/pic.jpg") && // Should be updated with Google picture
            user.getGoogleId().equals("google123")
        ));
    }
} 
