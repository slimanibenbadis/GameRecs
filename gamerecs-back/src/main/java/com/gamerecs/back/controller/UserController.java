package com.gamerecs.back.controller;

import com.gamerecs.back.dto.UserRegistrationDto;
import com.gamerecs.back.model.User;
import com.gamerecs.back.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
     * @return ResponseEntity containing the registered user or error details
     */
    @PostMapping("/register")
    public ResponseEntity<?> handleUserRegistration(@Valid @RequestBody UserRegistrationDto registrationDto) {
        logger.debug("Received registration request for user: {}", registrationDto.getUsername());
        
        try {
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
        } catch (IllegalArgumentException e) {
            logger.warn("Registration failed: {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Unexpected error during registration", e);
            return new ResponseEntity<>("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
} 
