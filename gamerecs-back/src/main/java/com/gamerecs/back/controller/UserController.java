package com.gamerecs.back.controller;

import com.gamerecs.back.dto.ProfileResponseDto;
import com.gamerecs.back.dto.UserRegistrationDto;
import com.gamerecs.back.model.User;
import com.gamerecs.back.security.CustomUserDetails;
import com.gamerecs.back.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for handling user-related endpoints.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    private final UserService userService;
    
    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * Handles user registration requests.
     *
     * @param registrationDto the user registration data
     * @return ResponseEntity containing the registered user
     */
    @PostMapping("/register")
    public ResponseEntity<User> handleUserRegistration(@Valid @RequestBody UserRegistrationDto registrationDto) {
        logger.debug("Received registration request for user: {}", registrationDto.getUsername());
        
        User user = User.builder()
                .username(registrationDto.getUsername())
                .email(registrationDto.getEmail())
                .passwordHash(registrationDto.getPassword()) // Will be hashed by UserService
                .profilePictureUrl(registrationDto.getProfilePictureUrl())
                .bio(registrationDto.getBio())
                .build();
        
        User registeredUser = userService.registerUser(user);
        logger.info("Successfully registered user: {}", registeredUser.getUsername());
        
        return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
    }

    /**
     * Handles email verification requests.
     *
     * @param token the verification token
     * @return ResponseEntity with verification status
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam(required = true) String token) {
        logger.debug("Received email verification request");
        
        boolean verified = userService.verifyEmail(token);
        
        if (verified) {
            logger.info("Email verification successful");
            return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
        } else {
            logger.warn("Email verification failed");
            return ResponseEntity.badRequest().body(Map.of("message", "Email verification failed"));
        }
    }

    /**
     * Retrieves the profile of the currently authenticated user.
     *
     * @return ResponseEntity containing the user's profile data
     */
    @GetMapping("/profile")
    public ResponseEntity<ProfileResponseDto> getCurrentUserProfile() {
        logger.debug("Retrieving profile for current user");
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        
        ProfileResponseDto profile = userService.getUserProfile(userDetails.getUserId());
        logger.info("Successfully retrieved profile for user: {}", userDetails.getUsername());
        
        return ResponseEntity.ok(profile);
    }
} 
