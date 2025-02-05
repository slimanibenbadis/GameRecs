package com.gamerecs.back.service;

import com.gamerecs.back.model.User;
import com.gamerecs.back.model.VerificationToken;
import com.gamerecs.back.repository.UserRepository;
import com.gamerecs.back.repository.VerificationTokenRepository;
import com.gamerecs.back.util.BaseUnitTest;
import com.gamerecs.back.dto.ProfileResponseDto;
import com.gamerecs.back.dto.UpdateProfileRequestDto;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest extends BaseUnitTest {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceTest.class);

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        logger.debug("Setting up test user");
        
        testUser = User.builder()
                .userId(1L)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("password123")
                .build();
    }

    @Test
    @DisplayName("Should register user successfully")
    void shouldRegisterUser() throws MessagingException {
        logger.debug("Testing successful user registration");
        
        String rawPassword = "password123";
        String hashedPassword = "hashedPassword";
        String verificationToken = "test-token";
        
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(hashedPassword);
        when(emailService.generateVerificationToken()).thenReturn(verificationToken);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertEquals(hashedPassword, savedUser.getPasswordHash(), "Password should be hashed");
            return savedUser;
        });

        User registeredUser = userService.registerUser(testUser);

        assertNotNull(registeredUser, "Registered user should not be null");
        assertEquals(testUser.getUsername(), registeredUser.getUsername(), "Username should match");
        assertEquals(testUser.getEmail(), registeredUser.getEmail(), "Email should match");
        assertFalse(registeredUser.isEmailVerified(), "Email should not be verified initially");

        verify(userRepository).existsByEmail(testUser.getEmail());
        verify(userRepository).existsByUsername(testUser.getUsername());
        verify(passwordEncoder).encode(rawPassword);
        verify(userRepository).save(any(User.class));
        verify(verificationTokenRepository).deleteByUser_UserId(testUser.getUserId());
        verify(emailService).generateVerificationToken();
        verify(emailService).sendVerificationEmail(eq(testUser.getEmail()), eq(testUser.getUsername()), eq(verificationToken));
    }

    @Test
    @DisplayName("Should throw exception when email exists")
    void shouldThrowExceptionWhenEmailExists() throws MessagingException {
        logger.debug("Testing registration with existing email");
        
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(testUser);
        });

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository).existsByEmail(testUser.getEmail());
        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw exception when username exists")
    void shouldThrowExceptionWhenUsernameExists() throws MessagingException {
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
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should hash password during registration")
    void shouldHashPasswordDuringRegistration() throws MessagingException {
        logger.debug("Testing password hashing during registration");
        
        String rawPassword = "password123";
        String hashedPassword = "hashedPassword123";
        String verificationToken = "test-token";
        testUser.setPasswordHash(rawPassword);

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(hashedPassword);
        when(emailService.generateVerificationToken()).thenReturn(verificationToken);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertEquals(hashedPassword, savedUser.getPasswordHash(), "Password should be hashed");
            return savedUser;
        });

        userService.registerUser(testUser);

        verify(passwordEncoder).encode(rawPassword);
        verify(userRepository).save(any(User.class));
        verify(verificationTokenRepository).deleteByUser_UserId(testUser.getUserId());
        verify(emailService).sendVerificationEmail(eq(testUser.getEmail()), eq(testUser.getUsername()), eq(verificationToken));
    }

    @Test
    @DisplayName("Should preserve user fields during registration")
    void shouldPreserveUserFieldsDuringRegistration() throws MessagingException {
        logger.debug("Testing preservation of user fields during registration");
        
        testUser.setProfilePictureUrl("http://example.com/pic.jpg");
        testUser.setBio("Test bio");
        String verificationToken = "test-token";

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(emailService.generateVerificationToken()).thenReturn(verificationToken);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertEquals(testUser.getProfilePictureUrl(), savedUser.getProfilePictureUrl(), 
                    "Profile picture URL should be preserved");
            assertEquals(testUser.getBio(), savedUser.getBio(), "Bio should be preserved");
            return savedUser;
        });

        userService.registerUser(testUser);

        verify(userRepository).save(any(User.class));
        verify(verificationTokenRepository).deleteByUser_UserId(testUser.getUserId());
        verify(emailService).sendVerificationEmail(eq(testUser.getEmail()), eq(testUser.getUsername()), eq(verificationToken));
    }

    @Test
    @DisplayName("Should verify email successfully")
    void shouldVerifyEmailSuccessfully() {
        logger.debug("Testing successful email verification");
        
        String token = "valid-token";
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(testUser);
        verificationToken.setExpiryDate(LocalDateTime.now().plusDays(1));

        when(verificationTokenRepository.findByToken(token)).thenReturn(Optional.of(verificationToken));

        boolean result = userService.verifyEmail(token);

        assertTrue(result, "Email verification should be successful");
        assertTrue(testUser.isEmailVerified(), "User email should be marked as verified");
        verify(userRepository).save(testUser);
        verify(verificationTokenRepository).delete(verificationToken);
    }

    @Test
    @DisplayName("Should throw exception for invalid token")
    void shouldThrowExceptionForInvalidToken() {
        logger.debug("Testing email verification with invalid token");
        
        String invalidToken = "invalid-token";
        when(verificationTokenRepository.findByToken(invalidToken)).thenReturn(Optional.empty());

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.verifyEmail(invalidToken);
        });

        assertEquals("Invalid verification token", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception for expired token")
    void shouldThrowExceptionForExpiredToken() {
        logger.debug("Testing email verification with expired token");
        
        String token = "expired-token";
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(testUser);
        verificationToken.setExpiryDate(LocalDateTime.now().minusDays(1));

        when(verificationTokenRepository.findByToken(token)).thenReturn(Optional.of(verificationToken));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.verifyEmail(token);
        });

        assertEquals("Verification token has expired", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
        verify(verificationTokenRepository).delete(verificationToken);
    }

    @Test
    void registerUser_WhenEmailServiceFails_ShouldStillRegisterUser() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(emailService.generateVerificationToken()).thenReturn("test-token");
        // Mock the verification token repository delete method
        doNothing().when(verificationTokenRepository).deleteByUser_UserId(anyLong());
        // Mock the verification token save
        when(verificationTokenRepository.save(any(VerificationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        // Mock email service to throw exception exactly as the real service does
        doAnswer(invocation -> {
            throw new MessagingException("Failed to send email");
        }).when(emailService).sendVerificationEmail(
            eq("test@example.com"),
            eq("testuser"),
            eq("test-token")
        );

        // Act
        User result = userService.registerUser(testUser);

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getUserId(), result.getUserId());
        assertEquals(testUser.getUsername(), result.getUsername());
        assertEquals(testUser.getEmail(), result.getEmail());
        assertFalse(result.isEmailVerified());

        // Verify interactions
        verify(userRepository).save(any(User.class));
        verify(verificationTokenRepository).deleteByUser_UserId(testUser.getUserId());
        verify(verificationTokenRepository).save(any(VerificationToken.class));
        verify(emailService).sendVerificationEmail(
            eq("test@example.com"),
            eq("testuser"),
            eq("test-token")
        );
    }

    @Test
    void registerUser_WhenEmailServiceSucceeds_ShouldRegisterUserAndSendEmail() throws MessagingException {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(emailService.generateVerificationToken()).thenReturn("test-token");

        // Act
        User result = userService.registerUser(testUser);

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getUserId(), result.getUserId());
        assertEquals(testUser.getUsername(), result.getUsername());
        assertEquals(testUser.getEmail(), result.getEmail());
        assertFalse(result.isEmailVerified());

        // Verify interactions
        verify(userRepository).save(any(User.class));
        verify(emailService).sendVerificationEmail(
                eq(testUser.getEmail()),
                eq(testUser.getUsername()),
                anyString()
        );
        verify(verificationTokenRepository).save(any(VerificationToken.class));
    }

    @Test
    void verifyEmail_WithValidToken_ShouldVerifyUserEmail() {
        // Arrange
        VerificationToken token = new VerificationToken();
        token.setToken("valid-token");
        token.setUser(testUser);
        token.setExpiryDate(java.time.LocalDateTime.now().plusHours(24));

        when(verificationTokenRepository.findByToken("valid-token"))
                .thenReturn(java.util.Optional.of(token));

        // Act
        boolean result = userService.verifyEmail("valid-token");

        // Assert
        assertTrue(result);
        assertTrue(testUser.isEmailVerified());
        verify(userRepository).save(testUser);
        verify(verificationTokenRepository).delete(token);
    }

    @Test
    void verifyEmail_WithExpiredToken_ShouldThrowException() {
        // Arrange
        VerificationToken token = new VerificationToken();
        token.setToken("expired-token");
        token.setUser(testUser);
        token.setExpiryDate(java.time.LocalDateTime.now().minusHours(1));

        when(verificationTokenRepository.findByToken("expired-token"))
                .thenReturn(java.util.Optional.of(token));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.verifyEmail("expired-token")
        );
        assertEquals("Verification token has expired", exception.getMessage());
        verify(verificationTokenRepository).delete(token);
    }

    @Test
    void verifyEmail_WithInvalidToken_ShouldThrowException() {
        // Arrange
        when(verificationTokenRepository.findByToken("invalid-token"))
                .thenReturn(java.util.Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.verifyEmail("invalid-token")
        );
        assertEquals("Invalid verification token", exception.getMessage());
    }

    @Test
    @DisplayName("Should successfully retrieve user profile")
    void getUserProfile_WhenUserExists_ShouldReturnProfile() {
        // Arrange
        Long userId = 1L;
        User user = User.builder()
                .userId(userId)
                .username("testuser")
                .email("test@example.com")
                .profilePictureUrl("http://example.com/pic.jpg")
                .bio("Test bio")
                .emailVerified(true)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        ProfileResponseDto profile = userService.getUserProfile(userId);

        // Assert
        assertNotNull(profile, "Profile should not be null");
        assertEquals(user.getUsername(), profile.getUsername(), "Username should match");
        assertEquals(user.getEmail(), profile.getEmail(), "Email should match");
        assertEquals(user.getProfilePictureUrl(), profile.getProfilePictureUrl(), "Profile picture URL should match");
        assertEquals(user.getBio(), profile.getBio(), "Bio should match");
        assertEquals(user.isEmailVerified(), profile.isEmailVerified(), "Email verification status should match");
        
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void getUserProfile_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        Long nonExistentUserId = 999L;
        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.getUserProfile(nonExistentUserId);
        });

        assertEquals("User not found", exception.getMessage(), "Exception message should match");
        verify(userRepository).findById(nonExistentUserId);
    }

    @Test
    @DisplayName("Should handle null fields in user profile")
    void getUserProfile_WhenUserHasNullFields_ShouldReturnProfileWithNullFields() {
        // Arrange
        Long userId = 1L;
        User user = User.builder()
                .userId(userId)
                .username("testuser")
                .email("test@example.com")
                // Deliberately leaving optional fields null
                .profilePictureUrl(null)
                .bio(null)
                .emailVerified(false)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        ProfileResponseDto profile = userService.getUserProfile(userId);

        // Assert
        assertNotNull(profile, "Profile should not be null");
        assertEquals(user.getUsername(), profile.getUsername(), "Username should match");
        assertEquals(user.getEmail(), profile.getEmail(), "Email should match");
        assertNull(profile.getProfilePictureUrl(), "Profile picture URL should be null");
        assertNull(profile.getBio(), "Bio should be null");
        assertFalse(profile.isEmailVerified(), "Email should not be verified");
        
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Should successfully update user profile")
    void updateUserProfile_Success() {
        // Arrange
        Long userId = 1L;
        UpdateProfileRequestDto updateRequest = new UpdateProfileRequestDto();
        updateRequest.setUsername("newUsername");
        updateRequest.setProfilePictureUrl("http://example.com/new-pic.jpg");
        updateRequest.setBio("Updated bio");

        User existingUser = User.builder()
                .userId(userId)
                .username("oldUsername")
                .email("test@example.com")
                .profilePictureUrl("http://example.com/old-pic.jpg")
                .bio("Old bio")
                .build();

        User updatedUser = User.builder()
                .userId(userId)
                .username(updateRequest.getUsername())
                .email("test@example.com")
                .profilePictureUrl(updateRequest.getProfilePictureUrl())
                .bio(updateRequest.getBio())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByUsername(updateRequest.getUsername())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // Act
        ProfileResponseDto result = userService.updateUserProfile(userId, updateRequest);

        // Assert
        assertNotNull(result, "Profile response should not be null");
        assertEquals(updateRequest.getUsername(), result.getUsername(), "Username should be updated");
        assertEquals(updateRequest.getProfilePictureUrl(), result.getProfilePictureUrl(), "Profile picture URL should be updated");
        assertEquals(updateRequest.getBio(), result.getBio(), "Bio should be updated");

        verify(userRepository).findById(userId);
        verify(userRepository).existsByUsername(updateRequest.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found during profile update")
    void updateUserProfile_UserNotFound_ThrowsException() {
        // Arrange
        Long nonExistentUserId = 999L;
        UpdateProfileRequestDto updateRequest = new UpdateProfileRequestDto();
        updateRequest.setUsername("newUsername");

        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.updateUserProfile(nonExistentUserId, updateRequest)
        );

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(nonExistentUserId);
        verify(userRepository, never()).existsByUsername(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when new username already exists")
    void updateUserProfile_UsernameExists_ThrowsException() {
        // Arrange
        Long userId = 1L;
        UpdateProfileRequestDto updateRequest = new UpdateProfileRequestDto();
        updateRequest.setUsername("existingUsername");
        updateRequest.setProfilePictureUrl("http://example.com/new-pic.jpg");
        updateRequest.setBio("Updated bio");

        User existingUser = User.builder()
                .userId(userId)
                .username("oldUsername")
                .email("test@example.com")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByUsername(updateRequest.getUsername())).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.updateUserProfile(userId, updateRequest)
        );

        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository).findById(userId);
        verify(userRepository).existsByUsername(updateRequest.getUsername());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should not check username existence when username unchanged")
    void updateUserProfile_UnchangedUsername_SkipsExistenceCheck() {
        // Arrange
        Long userId = 1L;
        String existingUsername = "existingUsername";
        UpdateProfileRequestDto updateRequest = new UpdateProfileRequestDto();
        updateRequest.setUsername(existingUsername);
        updateRequest.setProfilePictureUrl("http://example.com/new-pic.jpg");
        updateRequest.setBio("Updated bio");

        User existingUser = User.builder()
                .userId(userId)
                .username(existingUsername)
                .email("test@example.com")
                .build();

        User updatedUser = User.builder()
                .userId(userId)
                .username(existingUsername)
                .email("test@example.com")
                .profilePictureUrl(updateRequest.getProfilePictureUrl())
                .bio(updateRequest.getBio())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // Act
        ProfileResponseDto result = userService.updateUserProfile(userId, updateRequest);

        // Assert
        assertNotNull(result, "Profile response should not be null");
        assertEquals(existingUsername, result.getUsername(), "Username should remain unchanged");
        assertEquals(updateRequest.getProfilePictureUrl(), result.getProfilePictureUrl(), "Profile picture URL should be updated");
        assertEquals(updateRequest.getBio(), result.getBio(), "Bio should be updated");

        verify(userRepository).findById(userId);
        verify(userRepository, never()).existsByUsername(any());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should handle null optional fields in update request")
    void updateUserProfile_NullOptionalFields_Success() {
        // Arrange
        Long userId = 1L;
        UpdateProfileRequestDto updateRequest = new UpdateProfileRequestDto();
        updateRequest.setUsername("newUsername");
        updateRequest.setProfilePictureUrl(null);
        updateRequest.setBio(null);

        User existingUser = User.builder()
                .userId(userId)
                .username("oldUsername")
                .email("test@example.com")
                .profilePictureUrl("http://example.com/old-pic.jpg")
                .bio("Old bio")
                .build();

        User updatedUser = User.builder()
                .userId(userId)
                .username(updateRequest.getUsername())
                .email("test@example.com")
                .profilePictureUrl(null)
                .bio(null)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByUsername(updateRequest.getUsername())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // Act
        ProfileResponseDto result = userService.updateUserProfile(userId, updateRequest);

        // Assert
        assertNotNull(result, "Profile response should not be null");
        assertEquals(updateRequest.getUsername(), result.getUsername(), "Username should be updated");
        assertNull(result.getProfilePictureUrl(), "Profile picture URL should be null");
        assertNull(result.getBio(), "Bio should be null");

        verify(userRepository).findById(userId);
        verify(userRepository).existsByUsername(updateRequest.getUsername());
        verify(userRepository).save(any(User.class));
    }
} 
