package com.gamerecs.back.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamerecs.back.dto.ProfileResponseDto;
import com.gamerecs.back.dto.UserRegistrationDto;
import com.gamerecs.back.dto.UpdateProfileRequestDto;
import com.gamerecs.back.model.User;
import com.gamerecs.back.security.CustomUserDetails;
import com.gamerecs.back.service.UserService;
import com.gamerecs.back.util.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

class UserControllerTest extends BaseIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(UserControllerTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private UserRegistrationDto validRegistrationDto;
    private User mockUser;

    @BeforeEach
    void setUp() {
        logger.debug("Setting up test data");
        
        validRegistrationDto = new UserRegistrationDto();
        validRegistrationDto.setUsername("testuser");
        validRegistrationDto.setEmail("test@example.com");
        validRegistrationDto.setPassword("password123");
        validRegistrationDto.setProfilePictureUrl("http://example.com/pic.jpg");
        validRegistrationDto.setBio("Test bio");

        mockUser = User.builder()
                .userId(1L)
                .username(validRegistrationDto.getUsername())
                .email(validRegistrationDto.getEmail())
                .passwordHash("hashedPassword")
                .profilePictureUrl(validRegistrationDto.getProfilePictureUrl())
                .bio(validRegistrationDto.getBio())
                .build();
    }

    @Test
    @DisplayName("Should register user with valid data")
    void shouldRegisterUserWithValidData() throws Exception {
        logger.debug("Testing user registration with valid data");
        
        when(userService.registerUser(any(User.class))).thenReturn(mockUser);

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(mockUser.getUserId()))
                .andExpect(jsonPath("$.username").value(mockUser.getUsername()))
                .andExpect(jsonPath("$.email").value(mockUser.getEmail()));

        verify(userService).registerUser(any(User.class));
    }

    @Test
    @DisplayName("Should return 400 for invalid email format")
    void shouldReturn400ForInvalidEmailFormat() throws Exception {
        logger.debug("Testing user registration with invalid email format");
        
        validRegistrationDto.setEmail("invalid-email");

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors.email").exists());

        verify(userService, never()).registerUser(any(User.class));
    }

    @Test
    @DisplayName("Should return 400 for blank username")
    void shouldReturn400ForBlankUsername() throws Exception {
        logger.debug("Testing user registration with blank username");
        
        validRegistrationDto.setUsername("");

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors.username").exists());

        verify(userService, never()).registerUser(any(User.class));
    }

    @Test
    @DisplayName("Should return 400 for password too short")
    void shouldReturn400ForPasswordTooShort() throws Exception {
        logger.debug("Testing user registration with short password");
        
        validRegistrationDto.setPassword("short");

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors.password").exists());

        verify(userService, never()).registerUser(any(User.class));
    }

    @Test
    @DisplayName("Should return 409 when email already exists")
    void shouldReturn400WhenEmailExists() throws Exception {
        logger.debug("Testing user registration with existing email");
        
        when(userService.registerUser(any(User.class)))
                .thenThrow(new IllegalArgumentException("Email already exists"));

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Registration failed"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors.email").value("This email is already registered"));

        verify(userService).registerUser(any(User.class));
    }

    @Test
    @DisplayName("Should return 409 when username already exists")
    void shouldReturn400WhenUsernameExists() throws Exception {
        logger.debug("Testing user registration with existing username");
        
        when(userService.registerUser(any(User.class)))
                .thenThrow(new IllegalArgumentException("Username already exists"));

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Registration failed"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors.username").value("This username is already taken"));

        verify(userService).registerUser(any(User.class));
    }

    @Test
    @DisplayName("Should return 409 when username exists with different case")
    void shouldReturn400WhenUsernameExistsWithDifferentCase() throws Exception {
        logger.debug("Testing user registration with case-variant of existing username");
        
        // Create a registration DTO with uppercase variant of username
        UserRegistrationDto uppercaseVariantDto = new UserRegistrationDto();
        uppercaseVariantDto.setUsername("TESTUSER"); // Uppercase variant of "testuser"
        uppercaseVariantDto.setEmail("different@example.com");
        uppercaseVariantDto.setPassword("password123");
        
        when(userService.registerUser(any(User.class)))
                .thenThrow(new IllegalArgumentException("Username already exists"));

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(uppercaseVariantDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Registration failed"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors.username").value("This username is already taken"));

        verify(userService).registerUser(any(User.class));
    }

    @Test
    @DisplayName("Should return 409 when username exists with mixed case")
    void shouldReturn400WhenUsernameExistsWithMixedCase() throws Exception {
        logger.debug("Testing user registration with mixed-case variant of existing username");
        
        // Create a registration DTO with mixed case variant of username
        UserRegistrationDto mixedCaseVariantDto = new UserRegistrationDto();
        mixedCaseVariantDto.setUsername("TestUser"); // Mixed case variant of "testuser"
        mixedCaseVariantDto.setEmail("different@example.com");
        mixedCaseVariantDto.setPassword("password123");
        
        when(userService.registerUser(any(User.class)))
                .thenThrow(new IllegalArgumentException("Username already exists"));

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mixedCaseVariantDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Registration failed"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors.username").value("This username is already taken"));

        verify(userService).registerUser(any(User.class));
    }

    @Test
    @DisplayName("Should handle unexpected errors with 500 status")
    void shouldHandleUnexpectedErrors() throws Exception {
        logger.debug("Testing user registration with unexpected error");
        
        when(userService.registerUser(any(User.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationDto)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors").isEmpty());

        verify(userService).registerUser(any(User.class));
    }

    @Test
    @DisplayName("Should accept registration with optional fields null")
    void shouldAcceptRegistrationWithOptionalFieldsNull() throws Exception {
        logger.debug("Testing user registration with null optional fields");
        
        validRegistrationDto.setProfilePictureUrl(null);
        validRegistrationDto.setBio(null);
        
        User userWithoutOptionals = User.builder()
                .userId(1L)
                .username(validRegistrationDto.getUsername())
                .email(validRegistrationDto.getEmail())
                .passwordHash("hashedPassword")
                .build();
        
        when(userService.registerUser(any(User.class))).thenReturn(userWithoutOptionals);

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userWithoutOptionals.getUserId()))
                .andExpect(jsonPath("$.username").value(userWithoutOptionals.getUsername()))
                .andExpect(jsonPath("$.email").value(userWithoutOptionals.getEmail()))
                .andExpect(jsonPath("$.profilePictureUrl").doesNotExist())
                .andExpect(jsonPath("$.bio").doesNotExist());

        verify(userService).registerUser(any(User.class));
    }

    @Test
    @DisplayName("Should verify email successfully with valid token")
    void shouldVerifyEmailSuccessfully() throws Exception {
        logger.debug("Testing email verification with valid token");
        
        String validToken = "valid-token";
        when(userService.verifyEmail(validToken)).thenReturn(true);

        mockMvc.perform(get("/api/users/verify")
                .param("token", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully"));

        verify(userService).verifyEmail(validToken);
    }

    @Test
    @DisplayName("Should return 400 for invalid or expired token")
    void shouldReturn400ForInvalidToken() throws Exception {
        logger.debug("Testing email verification with invalid token");
        
        String invalidToken = "invalid-token";
        when(userService.verifyEmail(invalidToken)).thenReturn(false);

        mockMvc.perform(get("/api/users/verify")
                .param("token", invalidToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email verification failed"));

        verify(userService).verifyEmail(invalidToken);
    }

    @Test
    @DisplayName("Should handle missing token parameter")
    void shouldHandleMissingToken() throws Exception {
        logger.debug("Testing email verification with missing token");

        mockMvc.perform(get("/api/users/verify"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Missing required parameter"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors.token").value("Parameter is required"));
    }

    @Test
    @DisplayName("Should handle unexpected errors during verification")
    void shouldHandleUnexpectedVerificationErrors() throws Exception {
        logger.debug("Testing email verification with unexpected error");
        
        String token = "error-token";
        when(userService.verifyEmail(token))
                .thenThrow(new RuntimeException("Unexpected verification error"));

        mockMvc.perform(get("/api/users/verify")
                .param("token", token))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors").isEmpty());

        verify(userService).verifyEmail(token);
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should successfully retrieve current user profile")
    void getCurrentUserProfile_Success() throws Exception {
        logger.debug("Testing successful retrieval of current user profile");
        
        // Arrange
        Long userId = 1L;
        ProfileResponseDto expectedProfile = ProfileResponseDto.builder()
                .username("testuser")
                .email("test@example.com")
                .profilePictureUrl("http://example.com/pic.jpg")
                .bio("Test bio")
                .emailVerified(true)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(
            mockUser.getUsername(),
            mockUser.getPasswordHash(),
            true,
            mockUser.getUserId()
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null);

        when(userService.getUserProfile(userId)).thenReturn(expectedProfile);

        // Act & Assert
        mockMvc.perform(get("/api/users/profile")
                .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value(expectedProfile.getUsername()))
                .andExpect(jsonPath("$.email").value(expectedProfile.getEmail()))
                .andExpect(jsonPath("$.profilePictureUrl").value(expectedProfile.getProfilePictureUrl()))
                .andExpect(jsonPath("$.bio").value(expectedProfile.getBio()))
                .andExpect(jsonPath("$.emailVerified").value(expectedProfile.isEmailVerified()));

        verify(userService).getUserProfile(userId);
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should handle profile retrieval with missing optional fields")
    void getCurrentUserProfile_WithMissingOptionalFields() throws Exception {
        logger.debug("Testing profile retrieval with missing optional fields");
        
        // Arrange
        Long userId = 1L;
        ProfileResponseDto profileWithNulls = ProfileResponseDto.builder()
                .username("testuser")
                .email("test@example.com")
                .profilePictureUrl(null)
                .bio(null)
                .emailVerified(true)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(
            mockUser.getUsername(),
            mockUser.getPasswordHash(),
            true,
            mockUser.getUserId()
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null);

        when(userService.getUserProfile(userId)).thenReturn(profileWithNulls);

        // Act & Assert
        mockMvc.perform(get("/api/users/profile")
                .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value(profileWithNulls.getUsername()))
                .andExpect(jsonPath("$.email").value(profileWithNulls.getEmail()))
                .andExpect(jsonPath("$.profilePictureUrl").doesNotExist())
                .andExpect(jsonPath("$.bio").doesNotExist())
                .andExpect(jsonPath("$.emailVerified").value(profileWithNulls.isEmailVerified()));

        verify(userService).getUserProfile(userId);
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should handle service exception during profile retrieval")
    void getCurrentUserProfile_ServiceException() throws Exception {
        logger.debug("Testing profile retrieval with service exception");
        
        // Arrange
        Long userId = 1L;
        CustomUserDetails userDetails = new CustomUserDetails(
            mockUser.getUsername(),
            mockUser.getPasswordHash(),
            true,
            mockUser.getUserId()
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null);

        when(userService.getUserProfile(userId))
                .thenThrow(new IllegalArgumentException("User not found"));

        // Act & Assert
        mockMvc.perform(get("/api/users/profile")
                .with(authentication(auth)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("User not found"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService).getUserProfile(userId);
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should handle internal server error during profile retrieval")
    void getCurrentUserProfile_InternalServerError() throws Exception {
        logger.debug("Testing profile retrieval with internal server error");
        
        // Arrange
        Long userId = 1L;
        CustomUserDetails userDetails = new CustomUserDetails(
            mockUser.getUsername(),
            mockUser.getPasswordHash(),
            true,
            mockUser.getUserId()
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null);

        when(userService.getUserProfile(userId))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act & Assert
        mockMvc.perform(get("/api/users/profile")
                .with(authentication(auth)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService).getUserProfile(userId);
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should successfully update user profile")
    void updateUserProfile_Success() throws Exception {
        // Prepare test data
        UpdateProfileRequestDto updateRequest = UpdateProfileRequestDto.builder()
                .username("newUsername")
                .profilePictureUrl("http://new-picture.com/pic.jpg")
                .bio("Updated bio")
                .build();

        ProfileResponseDto expectedResponse = ProfileResponseDto.builder()
                .username("newUsername")
                .email("test@example.com")
                .profilePictureUrl("http://new-picture.com/pic.jpg")
                .bio("Updated bio")
                .emailVerified(true)
                .build();

        // Mock authentication
        CustomUserDetails userDetails = new CustomUserDetails(
            mockUser.getUsername(),
            mockUser.getPasswordHash(),
            true,
            mockUser.getUserId()
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null);

        // Mock service response
        when(userService.updateUserProfile(eq(mockUser.getUserId()), any(UpdateProfileRequestDto.class)))
                .thenReturn(expectedResponse);

        // Perform request and verify
        mockMvc.perform(put("/api/users/profile")
                .with(authentication(auth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(expectedResponse.getUsername()))
                .andExpect(jsonPath("$.email").value(expectedResponse.getEmail()))
                .andExpect(jsonPath("$.profilePictureUrl").value(expectedResponse.getProfilePictureUrl()))
                .andExpect(jsonPath("$.bio").value(expectedResponse.getBio()))
                .andExpect(jsonPath("$.emailVerified").value(expectedResponse.isEmailVerified()));

        verify(userService).updateUserProfile(eq(mockUser.getUserId()), any(UpdateProfileRequestDto.class));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should return 400 when username is invalid in profile update")
    void updateUserProfile_InvalidUsername() throws Exception {
        // Prepare test data with invalid username
        UpdateProfileRequestDto updateRequest = UpdateProfileRequestDto.builder()
                .username("a") // Too short username
                .profilePictureUrl("http://new-picture.com/pic.jpg")
                .bio("Updated bio")
                .build();

        // Mock authentication
        CustomUserDetails userDetails = new CustomUserDetails(
            mockUser.getUsername(),
            mockUser.getPasswordHash(),
            true,
            mockUser.getUserId()
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null);

        // Perform request and verify
        mockMvc.perform(put("/api/users/profile")
                .with(authentication(auth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.username").exists());

        verify(userService, never()).updateUserProfile(any(), any());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should return 400 when bio exceeds maximum length")
    void updateUserProfile_BioTooLong() throws Exception {
        // Prepare test data with too long bio
        String tooLongBio = "a".repeat(501); // Exceeds 500 character limit
        UpdateProfileRequestDto updateRequest = UpdateProfileRequestDto.builder()
                .username("validUsername")
                .bio(tooLongBio)
                .build();

        // Mock authentication
        CustomUserDetails userDetails = new CustomUserDetails(
            mockUser.getUsername(),
            mockUser.getPasswordHash(),
            true,
            mockUser.getUserId()
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null);

        // Perform request and verify
        mockMvc.perform(put("/api/users/profile")
                .with(authentication(auth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.bio").exists());

        verify(userService, never()).updateUserProfile(any(), any());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should return 401 when user is not authenticated for profile update")
    void updateUserProfile_Unauthorized() throws Exception {
        UpdateProfileRequestDto updateRequest = UpdateProfileRequestDto.builder()
                .username("newUsername")
                .bio("Updated bio")
                .build();

        mockMvc.perform(put("/api/users/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).updateUserProfile(any(), any());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should handle service exception during profile update")
    void updateUserProfile_ServiceException() throws Exception {
        // Prepare test data
        UpdateProfileRequestDto updateRequest = UpdateProfileRequestDto.builder()
                .username("newUsername")
                .bio("Updated bio")
                .build();

        // Mock authentication
        CustomUserDetails userDetails = new CustomUserDetails(
            mockUser.getUsername(),
            mockUser.getPasswordHash(),
            true,
            mockUser.getUserId()
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null);

        // Mock service throwing exception
        when(userService.updateUserProfile(eq(mockUser.getUserId()), any(UpdateProfileRequestDto.class)))
                .thenThrow(new IllegalArgumentException("Username already taken"));

        // Perform request and verify
        mockMvc.perform(put("/api/users/profile")
                .with(authentication(auth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Username already taken"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService).updateUserProfile(eq(mockUser.getUserId()), any(UpdateProfileRequestDto.class));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should handle internal server error during profile update")
    void updateUserProfile_InternalServerError() throws Exception {
        // Prepare test data
        UpdateProfileRequestDto updateRequest = UpdateProfileRequestDto.builder()
                .username("newUsername")
                .bio("Updated bio")
                .build();

        // Mock authentication
        CustomUserDetails userDetails = new CustomUserDetails(
            mockUser.getUsername(),
            mockUser.getPasswordHash(),
            true,
            mockUser.getUserId()
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null);

        // Mock service throwing runtime exception
        when(userService.updateUserProfile(eq(mockUser.getUserId()), any(UpdateProfileRequestDto.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Perform request and verify
        mockMvc.perform(put("/api/users/profile")
                .with(authentication(auth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService).updateUserProfile(eq(mockUser.getUserId()), any(UpdateProfileRequestDto.class));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should successfully update profile with minimal data")
    void updateUserProfile_MinimalData() throws Exception {
        // Prepare test data with only required field
        UpdateProfileRequestDto updateRequest = UpdateProfileRequestDto.builder()
                .username("newUsername")
                .build();

        ProfileResponseDto expectedResponse = ProfileResponseDto.builder()
                .username("newUsername")
                .email("test@example.com")
                .emailVerified(true)
                .build();

        // Mock authentication
        CustomUserDetails userDetails = new CustomUserDetails(
            mockUser.getUsername(),
            mockUser.getPasswordHash(),
            true,
            mockUser.getUserId()
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null);

        // Mock service response
        when(userService.updateUserProfile(eq(mockUser.getUserId()), any(UpdateProfileRequestDto.class)))
                .thenReturn(expectedResponse);

        // Perform request and verify
        mockMvc.perform(put("/api/users/profile")
                .with(authentication(auth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(expectedResponse.getUsername()))
                .andExpect(jsonPath("$.email").value(expectedResponse.getEmail()))
                .andExpect(jsonPath("$.profilePictureUrl").doesNotExist())
                .andExpect(jsonPath("$.bio").doesNotExist())
                .andExpect(jsonPath("$.emailVerified").value(expectedResponse.isEmailVerified()));

        verify(userService).updateUserProfile(eq(mockUser.getUserId()), any(UpdateProfileRequestDto.class));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should return 400 when multiple fields are invalid in profile update")
    void updateUserProfile_MultipleInvalidFields() throws Exception {
        // Prepare test data with multiple invalid fields
        UpdateProfileRequestDto updateRequest = UpdateProfileRequestDto.builder()
                .username("a") // Too short username
                .profilePictureUrl("not-a-valid-url") // Invalid URL format
                .bio("a".repeat(501)) // Bio exceeds max length
                .build();

        // Mock authentication
        CustomUserDetails userDetails = new CustomUserDetails(
            mockUser.getUsername(),
            mockUser.getPasswordHash(),
            true,
            mockUser.getUserId()
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null);

        // Perform request and verify
        mockMvc.perform(put("/api/users/profile")
                .with(authentication(auth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.username").value("Username must be between 3 and 50 characters"))
                .andExpect(jsonPath("$.errors.profilePictureUrl").value("Profile picture URL must be a valid URL"))
                .andExpect(jsonPath("$.errors.bio").value("Bio cannot exceed 500 characters"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).updateUserProfile(any(), any());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should return 400 when username contains invalid characters")
    void updateUserProfile_InvalidUsernameCharacters() throws Exception {
        // Prepare test data with invalid username characters
        UpdateProfileRequestDto updateRequest = UpdateProfileRequestDto.builder()
                .username("invalid@username#$%") // Contains invalid special characters
                .build();

        // Mock authentication
        CustomUserDetails userDetails = new CustomUserDetails(
            mockUser.getUsername(),
            mockUser.getPasswordHash(),
            true,
            mockUser.getUserId()
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null);

        // Perform request and verify
        mockMvc.perform(put("/api/users/profile")
                .with(authentication(auth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.username").value("Username can only contain letters, numbers, underscores and hyphens"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).updateUserProfile(any(), any());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should return 400 when username is blank")
    void updateUserProfile_BlankUsername() throws Exception {
        // Prepare test data with blank username
        UpdateProfileRequestDto updateRequest = UpdateProfileRequestDto.builder()
                .username("   ") // Blank username
                .build();

        // Mock authentication
        CustomUserDetails userDetails = new CustomUserDetails(
            mockUser.getUsername(),
            mockUser.getPasswordHash(),
            true,
            mockUser.getUserId()
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null);

        // Perform request and verify
        mockMvc.perform(put("/api/users/profile")
                .with(authentication(auth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.username").value("Username cannot be empty"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).updateUserProfile(any(), any());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Should return 400 when profile picture URL is malformed")
    void updateUserProfile_MalformedProfilePictureUrl() throws Exception {
        // Prepare test data with malformed URL
        UpdateProfileRequestDto updateRequest = UpdateProfileRequestDto.builder()
                .username("validUsername")
                .profilePictureUrl("malformed:url:format") // Malformed URL
                .build();

        // Mock authentication
        CustomUserDetails userDetails = new CustomUserDetails(
            mockUser.getUsername(),
            mockUser.getPasswordHash(),
            true,
            mockUser.getUserId()
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null);

        // Perform request and verify
        mockMvc.perform(put("/api/users/profile")
                .with(authentication(auth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.profilePictureUrl").value("Profile picture URL must be a valid URL"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).updateUserProfile(any(), any());
    }
} 
