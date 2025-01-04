package com.gamerecs.back.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamerecs.back.dto.UserRegistrationDto;
import com.gamerecs.back.model.User;
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
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    @DisplayName("Should return 400 when email already exists")
    void shouldReturn400WhenEmailExists() throws Exception {
        logger.debug("Testing user registration with existing email");
        
        when(userService.registerUser(any(User.class)))
                .thenThrow(new IllegalArgumentException("Email already exists"));

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Email already exists"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors").isEmpty());

        verify(userService).registerUser(any(User.class));
    }

    @Test
    @DisplayName("Should return 400 when username already exists")
    void shouldReturn400WhenUsernameExists() throws Exception {
        logger.debug("Testing user registration with existing username");
        
        when(userService.registerUser(any(User.class)))
                .thenThrow(new IllegalArgumentException("Username already exists"));

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Username already exists"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors").isEmpty());

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
} 
