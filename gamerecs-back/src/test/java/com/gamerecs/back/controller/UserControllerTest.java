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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;


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
                .andExpect(jsonPath("$.message").value("Internal server error"))
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
                .andExpect(jsonPath("$.message").value("Internal server error"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors").isEmpty());

        verify(userService).verifyEmail(token);
    }

    // --- Tests for getCurrentUserProfile ---

    @Test
    @DisplayName("Should get current user profile successfully")
    void shouldGetCurrentUserProfileSuccessfully() throws Exception {
        logger.debug("Testing getting current user profile successfully");

        CustomUserDetails userDetails = new CustomUserDetails(
                mockUser.getUsername(),
                mockUser.getPasswordHash(),
                true, // Assuming the user is enabled
                mockUser.getUserId()
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        ProfileResponseDto profileResponseDto = ProfileResponseDto.builder()
            .username(mockUser.getUsername())
            .email(mockUser.getEmail())
            .profilePictureUrl(mockUser.getProfilePictureUrl())
            .bio(mockUser.getBio())
            .emailVerified(true) // Assuming email is verified for this test
            .build();

        when(userService.getUserProfile(any(CustomUserDetails.class))).thenReturn(profileResponseDto);

        mockMvc.perform(get("/api/users/profile")
                .with(authentication(authentication))) // Use SecurityMockMvcRequestPostProcessors
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(profileResponseDto.getUsername()))
                .andExpect(jsonPath("$.email").value(profileResponseDto.getEmail()))
                .andExpect(jsonPath("$.profilePictureUrl").value(profileResponseDto.getProfilePictureUrl()))
                .andExpect(jsonPath("$.bio").value(profileResponseDto.getBio()))
                .andExpect(jsonPath("$.emailVerified").value(profileResponseDto.isEmailVerified()));

        verify(userService).getUserProfile(refEq(userDetails, "password")); // Compare fields except password
    }

    @Test
    @DisplayName("Should return 401 when getting profile without authentication")
    void shouldReturn401WhenGettingProfileUnauthenticated() throws Exception {
        logger.debug("Testing getting profile without authentication");

        // Clear any existing security context
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).getUserProfile(any());
    }
    
    @Test
    @DisplayName("Should return 403 when getting profile with invalid authentication principal")
    void shouldReturn401WhenGettingProfileWithInvalidPrincipal() throws Exception {
        logger.debug("Testing getting profile with invalid authentication principal");

        // Simulate an authentication object with a principal that is not CustomUserDetails
        Authentication invalidAuthentication = new UsernamePasswordAuthenticationToken("notUserDetails", null);

        mockMvc.perform(get("/api/users/profile")
                .with(authentication(invalidAuthentication))
                .with(csrf()))
                .andExpect(status().isForbidden());

        verify(userService, never()).getUserProfile(any());
    }


    // --- Tests for updateUserProfile ---

    @Test
    @DisplayName("Should update user profile successfully")
    void shouldUpdateUserProfileSuccessfully() throws Exception {
        logger.debug("Testing updating user profile successfully");

        CustomUserDetails userDetails = new CustomUserDetails(
                mockUser.getUsername(),
                mockUser.getPasswordHash(),
                true, // Assuming the user is enabled
                mockUser.getUserId()
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        UpdateProfileRequestDto updateRequest = UpdateProfileRequestDto.builder()
                .username(mockUser.getUsername()) // Include username if it's part of the request DTO
                .bio("New Bio")
                .profilePictureUrl("http://example.com/newpic.jpg")
                .build();

        ProfileResponseDto updatedProfileDto = ProfileResponseDto.builder()
            .username(mockUser.getUsername())
            .email(mockUser.getEmail())
            .profilePictureUrl(updateRequest.getProfilePictureUrl()) // Use updated URL from request
            .bio(updateRequest.getBio()) // Use updated bio from request
            .emailVerified(true)
            .build();

        when(userService.updateUserProfile(any(CustomUserDetails.class), any(UpdateProfileRequestDto.class)))
            .thenReturn(updatedProfileDto);

        mockMvc.perform(put("/api/users/profile")
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(updatedProfileDto.getUsername()))
                .andExpect(jsonPath("$.email").value(updatedProfileDto.getEmail()))
                .andExpect(jsonPath("$.profilePictureUrl").value(updatedProfileDto.getProfilePictureUrl()))
                .andExpect(jsonPath("$.bio").value(updatedProfileDto.getBio()))
                .andExpect(jsonPath("$.emailVerified").value(updatedProfileDto.isEmailVerified()));

        verify(userService).updateUserProfile(eq(userDetails), refEq(updateRequest));
    }

    @Test
    @DisplayName("Should return 401 when updating profile without authentication")
    void shouldReturn401WhenUpdatingProfileUnauthenticated() throws Exception {
        logger.debug("Testing updating profile without authentication");

        // Use builder for consistency
        UpdateProfileRequestDto updateRequest = UpdateProfileRequestDto.builder()
                .username("testuser") // Add username if needed by DTO
                .bio("New Bio")
                .profilePictureUrl("http://example.com/newpic.jpg")
                .build();

        // Clear any existing security context
        SecurityContextHolder.clearContext();

        mockMvc.perform(put("/api/users/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .with(csrf()))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).updateUserProfile(any(), any());
    }
    
    @Test
    @DisplayName("Should return 403 when updating profile with invalid authentication principal")
    void shouldReturn401WhenUpdatingProfileWithInvalidPrincipal() throws Exception {
        logger.debug("Testing updating profile with invalid authentication principal");

        // Use builder for consistency
        UpdateProfileRequestDto updateRequest = UpdateProfileRequestDto.builder()
                .username("testuser") // Add username if needed by DTO
                .bio("New Bio")
                .profilePictureUrl("http://example.com/newpic.jpg")
                .build();

        // Simulate an authentication object with a principal that is not CustomUserDetails
        Authentication invalidAuthentication = new UsernamePasswordAuthenticationToken("notUserDetails", null);

        mockMvc.perform(put("/api/users/profile")
                .with(authentication(invalidAuthentication))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .with(csrf()))
                .andExpect(status().isForbidden());

        verify(userService, never()).updateUserProfile(any(), any());
    }

    // Optional: Add test for validation failure in UpdateProfileRequestDto if applicable
    // e.g., if bio has length constraints
    @Test
    @DisplayName("Should return 400 when updating profile with invalid data")
    void shouldReturn400WhenUpdatingProfileWithInvalidData() throws Exception {
        logger.debug("Testing updating profile with invalid data");

        // Corrected CustomUserDetails instantiation
        CustomUserDetails userDetails = new CustomUserDetails(
                mockUser.getUsername(),
                mockUser.getPasswordHash(),
                true, // Assuming the user is enabled
                mockUser.getUserId()
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        // Create a string longer than 500 characters
        String longBio = "a".repeat(501);

        UpdateProfileRequestDto invalidUpdateRequest = UpdateProfileRequestDto.builder()
                .username(mockUser.getUsername())
                .bio(longBio) // Use the long string
                .profilePictureUrl("http://example.com/pic.jpg")
                .build();

        mockMvc.perform(put("/api/users/profile")
                .with(authentication(authentication))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidUpdateRequest))
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.bio").exists()); // Check for specific error

        verify(userService, never()).updateUserProfile(any(), any());
    }
} 
