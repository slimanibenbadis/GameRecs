package com.gamerecs.back.service;

import com.gamerecs.back.model.User;
import com.gamerecs.back.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for handling user-related business logic.
 */
@Service
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    /**
     * Registers a new user in the system.
     *
     * @param user the user to register
     * @return the registered user with generated ID
     * @throws IllegalArgumentException if a user with the same email or username already exists
     */
    @Transactional
    public User registerUser(User user) {
        logger.debug("Attempting to register new user with email: {}", user.getEmail());
        
        // Check if email is already taken
        if (userRepository.existsByEmail(user.getEmail())) {
            logger.warn("Registration failed: Email already exists: {}", user.getEmail());
            throw new IllegalArgumentException("Email already exists");
        }
        
        // Check if username is already taken
        if (userRepository.existsByUsername(user.getUsername())) {
            logger.warn("Registration failed: Username already exists: {}", user.getUsername());
            throw new IllegalArgumentException("Username already exists");
        }
        
        // Hash the password
        String hashedPassword = passwordEncoder.encode(user.getPasswordHash());
        user.setPasswordHash(hashedPassword);
        
        // Save the user
        User savedUser = userRepository.save(user);
        logger.info("Successfully registered new user with ID: {}", savedUser.getUserId());
        
        return savedUser;
    }
} 
