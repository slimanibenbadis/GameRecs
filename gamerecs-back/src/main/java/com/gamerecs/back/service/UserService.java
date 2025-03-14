package com.gamerecs.back.service;

import com.gamerecs.back.dto.ProfileResponseDto;
import com.gamerecs.back.dto.UpdateProfileRequestDto;
import com.gamerecs.back.model.GameLibrary;
import com.gamerecs.back.model.User;
import com.gamerecs.back.model.VerificationToken;
import com.gamerecs.back.repository.GameLibraryRepository;
import com.gamerecs.back.repository.UserRepository;
import com.gamerecs.back.repository.VerificationTokenRepository;
import com.gamerecs.back.util.UsernameNormalizer;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service class for handling user-related business logic.
 */
@Service
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final VerificationTokenRepository verificationTokenRepository;
    private final GameLibraryRepository gameLibraryRepository;

    @Value("${app.verification.token.expiration-hours:24}")
    private int tokenExpirationHours;
    
    @Autowired
    public UserService(UserRepository userRepository, 
                      PasswordEncoder passwordEncoder,
                      EmailService emailService,
                      VerificationTokenRepository verificationTokenRepository,
                      GameLibraryRepository gameLibraryRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.verificationTokenRepository = verificationTokenRepository;
        this.gameLibraryRepository = gameLibraryRepository;
    }
    
    /**
     * Registers a new user in the system and sends a verification email.
     * Also creates a game library for the user.
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
        
        // Normalize username to lowercase
        String normalizedUsername = user.getUsername().toLowerCase();
        user.setUsername(normalizedUsername);
        logger.debug("Normalized username to lowercase: {}", normalizedUsername);
        
        // Check if username is already taken (now case-insensitive)
        if (userRepository.existsByUsername(normalizedUsername)) {
            logger.warn("Registration failed: Username already exists: {}", normalizedUsername);
            throw new IllegalArgumentException("Username already exists");
        }
        
        // Hash the password
        String hashedPassword = passwordEncoder.encode(user.getPasswordHash());
        user.setPasswordHash(hashedPassword);
        
        // Set email as unverified
        user.setEmailVerified(false);
        
        // Save the user
        User savedUser = userRepository.save(user);
        logger.info("Successfully registered new user with ID: {}", savedUser.getUserId());
        
        // Create a GameLibrary for the saved user
        GameLibrary library = new GameLibrary();
        library.setUser(savedUser);
        library = gameLibraryRepository.save(library);
        logger.info("Created game library for user ID: {}", savedUser.getUserId());
        
        // Link the library to the user
        savedUser.setGameLibrary(library);
        // No need to save the user again, as we're in a transactional context
        // and all changes to the managed entity will be persisted
        
        // Generate and send verification email
        try {
            sendVerificationEmail(savedUser);
            logger.info("Verification email sent successfully to user: {}", savedUser.getEmail());
        } catch (MessagingException e) {
            logger.error("Failed to send verification email to user: {}", savedUser.getEmail(), e);
            // Don't throw the exception as the user is already registered
        }
        
        return savedUser;
    }

    /**
     * Generates a verification token and sends a verification email to the user.
     *
     * @param user the user to send verification email to
     * @throws MessagingException if there is an error sending the email
     */
    private void sendVerificationEmail(User user) throws MessagingException {
        logger.debug("Generating verification token for user: {}", user.getEmail());
        
        // Delete any existing tokens for this user
        verificationTokenRepository.deleteByUser_UserId(user.getUserId());
        
        // Generate new token
        String token = emailService.generateVerificationToken();
        
        // Create verification token
        VerificationToken verificationToken = VerificationToken.builder()
                .user(user)
                .token(token)
                .expiryDate(LocalDateTime.now().plusHours(tokenExpirationHours))
                .build();
        
        // Save token
        verificationTokenRepository.save(verificationToken);
        
        // Send verification email
        emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), token);
    }

    /**
     * Verifies a user's email using a verification token.
     *
     * @param token the verification token
     * @return true if verification was successful
     * @throws IllegalArgumentException if token is invalid or expired
     */
    @Transactional
    public boolean verifyEmail(String token) {
        logger.debug("Attempting to verify email with token");
        
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    logger.warn("Email verification failed: Token not found");
                    return new IllegalArgumentException("Invalid verification token");
                });
        
        if (verificationToken.isExpired()) {
            logger.warn("Email verification failed: Token expired");
            verificationTokenRepository.delete(verificationToken);
            throw new IllegalArgumentException("Verification token has expired");
        }
        
        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        
        // Clean up used token
        verificationTokenRepository.delete(verificationToken);
        
        logger.info("Successfully verified email for user: {}", user.getEmail());
        return true;
    }

    /**
     * Retrieves a user's profile by their ID.
     *
     * @param userId the ID of the user
     * @return the user's profile data
     * @throws IllegalArgumentException if the user is not found
     */
    public ProfileResponseDto getUserProfile(Long userId) {
        logger.debug("Retrieving profile for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("Profile retrieval failed: User not found with ID: {}", userId);
                    return new IllegalArgumentException("User not found");
                });
        
        return mapToProfileResponse(user);
    }

    /**
     * Updates a user's profile information.
     *
     * @param userId the ID of the user to update
     * @param updateRequest the new profile information
     * @return the updated profile data
     * @throws IllegalArgumentException if the user is not found or if the username is already taken
     */
    @Transactional
    public ProfileResponseDto updateUserProfile(Long userId, UpdateProfileRequestDto updateRequest) {
        logger.debug("Updating profile for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("Profile update failed: User not found with ID: {}", userId);
                    return new IllegalArgumentException("User not found");
                });

        // Get and normalize the new username
        String newNormalizedUsername = UsernameNormalizer.normalize(updateRequest.getUsername());
        String currentNormalizedUsername = user.getUsername(); // Already normalized by User entity

        // Check if username is actually changing
        if (!currentNormalizedUsername.equals(newNormalizedUsername)) {
            logger.debug("Username change requested from '{}' to '{}'", currentNormalizedUsername, newNormalizedUsername);
            
            // Check if new normalized username is already taken by another user
            if (userRepository.existsByUsername(newNormalizedUsername)) {
                logger.warn("Profile update failed: Username already exists: {}", newNormalizedUsername);
                throw new IllegalArgumentException("Username already exists");
            }
            
            // Set the new normalized username
            user.setUsername(newNormalizedUsername);
        }

        // Update other profile information
        user.setProfilePictureUrl(updateRequest.getProfilePictureUrl());
        user.setBio(updateRequest.getBio());

        User updatedUser = userRepository.save(user);
        logger.info("Successfully updated profile for user: {}", updatedUser.getUsername());

        return mapToProfileResponse(updatedUser);
    }

    /**
     * Maps a User entity to a ProfileResponseDto.
     *
     * @param user the user entity
     * @return the profile response DTO
     */
    private ProfileResponseDto mapToProfileResponse(User user) {
        return ProfileResponseDto.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .profilePictureUrl(user.getProfilePictureUrl())
                .bio(user.getBio())
                .emailVerified(user.isEmailVerified())
                .build();
    }
} 
