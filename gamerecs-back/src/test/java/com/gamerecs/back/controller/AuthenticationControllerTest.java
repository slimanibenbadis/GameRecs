package com.gamerecs.back.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamerecs.back.dto.LoginRequestDto;
import com.gamerecs.back.model.User;
import com.gamerecs.back.repository.UserRepository;
import com.gamerecs.back.service.JwtService;
import com.gamerecs.back.util.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthenticationControllerTest extends BaseIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationControllerTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    private LoginRequestDto validLoginRequest;
    private User mockUser;
    private Authentication mockAuthentication;
    private UserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        logger.debug("Setting up test data");
        
        validLoginRequest = new LoginRequestDto();
        validLoginRequest.setUsername("testuser");
        validLoginRequest.setPassword("password123");

        mockUser = User.builder()
                .userId(1L)
                .username(validLoginRequest.getUsername())
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .emailVerified(true)
                .build();

        mockUserDetails = org.springframework.security.core.userdetails.User.builder()
                .username(mockUser.getUsername())
                .password(mockUser.getPasswordHash())
                .authorities(java.util.Collections.emptyList())
                .build();

        mockAuthentication = new UsernamePasswordAuthenticationToken(
                mockUserDetails, null, mockUserDetails.getAuthorities());
    }

    @Test
    @DisplayName("Should authenticate user with valid credentials")
    void shouldAuthenticateWithValidCredentials() throws Exception {
        logger.debug("Testing authentication with valid credentials");
        
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(mockAuthentication);
        when(userRepository.findByUsername(mockUser.getUsername()))
                .thenReturn(Optional.of(mockUser));
        when(jwtService.generateToken(any(UserDetails.class)))
                .thenReturn("valid.jwt.token");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value(mockUser.getUsername()))
                .andExpect(jsonPath("$.email").value(mockUser.getEmail()))
                .andExpect(jsonPath("$.emailVerified").value(mockUser.isEmailVerified()));

        verify(authenticationManager).authenticate(any(Authentication.class));
        verify(jwtService).generateToken(any(UserDetails.class));
    }

    @Test
    @DisplayName("Should return 401 for invalid credentials")
    void shouldReturn401ForInvalidCredentials() throws Exception {
        logger.debug("Testing authentication with invalid credentials");
        
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid credentials"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authenticationManager).authenticate(any(Authentication.class));
        verify(jwtService, never()).generateToken(any(UserDetails.class));
    }

    @Test
    @DisplayName("Should return 400 for invalid request format")
    void shouldReturn400ForInvalidRequestFormat() throws Exception {
        logger.debug("Testing authentication with invalid request format");
        
        LoginRequestDto invalidRequest = new LoginRequestDto();
        // Missing required fields

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors").exists());

        verify(authenticationManager, never()).authenticate(any(Authentication.class));
    }

    @Test
    @DisplayName("Should return 500 for unexpected errors")
    void shouldReturn500ForUnexpectedErrors() throws Exception {
        logger.debug("Testing authentication with unexpected error");
        
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authenticationManager).authenticate(any(Authentication.class));
        verify(jwtService, never()).generateToken(any(UserDetails.class));
    }

    @Test
    @DisplayName("Should return 400 when user not found after authentication")
    void shouldReturn400WhenUserNotFound() throws Exception {
        logger.debug("Testing authentication when user not found");
        
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(mockAuthentication);
        when(userRepository.findByUsername(mockUser.getUsername()))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("User not found after authentication"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authenticationManager).authenticate(any(Authentication.class));
        verify(jwtService, never()).generateToken(any(UserDetails.class));
    }

    @Test
    @DisplayName("Should return 401 for unverified email")
    void shouldReturn401ForUnverifiedEmail() throws Exception {
        logger.debug("Testing authentication with unverified email");

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new org.springframework.security.authentication.DisabledException("Account is disabled"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Account is disabled"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authenticationManager).authenticate(any(Authentication.class));
        verify(jwtService, never()).generateToken(any(UserDetails.class));
    }
} 
