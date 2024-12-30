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
                .andExpect(jsonPath("$.email").exists());

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
                .andExpect(jsonPath("$.username").exists());

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
                .andExpect(jsonPath("$.password").exists());

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
                .andExpect(content().string("Email already exists"));

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
                .andExpect(content().string("Username already exists"));

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
                .andExpect(content().string("An unexpected error occurred"));

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
} 
