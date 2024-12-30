package com.gamerecs.back.service;

import com.gamerecs.back.model.User;
import com.gamerecs.back.repository.UserRepository;
import com.gamerecs.back.util.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UserServiceTest extends BaseUnitTest {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceTest.class);

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        logger.debug("Setting up test user");
        
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("password123")
                .build();
    }

    @Test
    @DisplayName("Should register user successfully")
    void shouldRegisterUser() {
        logger.debug("Testing successful user registration");
        
        String rawPassword = "password123";
        String hashedPassword = "hashedPassword";
        
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(hashedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertEquals(hashedPassword, savedUser.getPasswordHash(), "Password should be hashed");
            return savedUser;
        });

        User registeredUser = userService.registerUser(testUser);

        assertNotNull(registeredUser, "Registered user should not be null");
        assertEquals(testUser.getUsername(), registeredUser.getUsername(), "Username should match");
        assertEquals(testUser.getEmail(), registeredUser.getEmail(), "Email should match");

        verify(userRepository).existsByEmail(testUser.getEmail());
        verify(userRepository).existsByUsername(testUser.getUsername());
        verify(passwordEncoder).encode(rawPassword);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when email exists")
    void shouldThrowExceptionWhenEmailExists() {
        logger.debug("Testing registration with existing email");
        
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(testUser);
        });

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository).existsByEmail(testUser.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when username exists")
    void shouldThrowExceptionWhenUsernameExists() {
        logger.debug("Testing registration with existing username");
        
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(testUser);
        });

        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository).existsByEmail(testUser.getEmail());
        verify(userRepository).existsByUsername(testUser.getUsername());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should hash password during registration")
    void shouldHashPasswordDuringRegistration() {
        logger.debug("Testing password hashing during registration");
        
        String rawPassword = "password123";
        String hashedPassword = "hashedPassword123";
        testUser.setPasswordHash(rawPassword);

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(hashedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertEquals(hashedPassword, savedUser.getPasswordHash(), "Password should be hashed");
            return savedUser;
        });

        userService.registerUser(testUser);

        verify(passwordEncoder).encode(rawPassword);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should preserve user fields during registration")
    void shouldPreserveUserFieldsDuringRegistration() {
        logger.debug("Testing preservation of user fields during registration");
        
        testUser.setProfilePictureUrl("http://example.com/pic.jpg");
        testUser.setBio("Test bio");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertEquals(testUser.getProfilePictureUrl(), savedUser.getProfilePictureUrl(), 
                    "Profile picture URL should be preserved");
            assertEquals(testUser.getBio(), savedUser.getBio(), "Bio should be preserved");
            return savedUser;
        });

        userService.registerUser(testUser);

        verify(userRepository).save(any(User.class));
    }
} 
