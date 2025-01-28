package com.gamerecs.back.service;

import com.gamerecs.back.model.User;
import com.gamerecs.back.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class OAuth2UserService extends DefaultOAuth2UserService {
    private static final Logger logger = LoggerFactory.getLogger(OAuth2UserService.class);

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Autowired
    public OAuth2UserService(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        logger.debug("Loading OAuth2 user details");
        try {
            OAuth2User oauth2User = getDelegate().loadUser(userRequest);
            logger.debug("OAuth2 user attributes: {}", oauth2User.getAttributes());
            User user = processOAuth2User(oauth2User);
            logger.debug("User processed successfully. UserId: {}, Username: {}, Email: {}", 
                user.getUserId(), user.getUsername(), user.getEmail());
            return oauth2User;
        } catch (OAuth2AuthenticationException ex) {
            logger.error("OAuth2 authentication error", ex);
            throw ex;
        } catch (Exception ex) {
            logger.error("Error processing OAuth2 user", ex);
            throw new OAuth2AuthenticationException(new OAuth2Error("processing_error", ex.getMessage(), null));
        }
    }

    // Protected method to allow overriding in tests
    protected DefaultOAuth2UserService getDelegate() {
        return new DefaultOAuth2UserService();
    }

    @Transactional
    public User processOAuth2User(OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String pictureUrl = (String) attributes.get("picture");
        String googleId = (String) attributes.get("sub"); // Google's unique identifier

        if (email == null || googleId == null) {
            logger.error("Required OAuth2 user attributes missing. Email: {}, GoogleId: {}", email, googleId);
            throw new OAuth2AuthenticationException(
                new OAuth2Error("invalid_user_info", "Required user information is missing", null));
        }

        logger.debug("Processing OAuth2 user with email: {} and Google ID: {}", email, googleId);

        // First try to find user by Google ID
        Optional<User> userOptional = userRepository.findByGoogleId(googleId);
        logger.debug("User lookup by Google ID result: {}", userOptional.isPresent());
        
        // If not found by Google ID, try by email
        if (userOptional.isEmpty()) {
            logger.debug("User not found by Google ID, checking email");
            userOptional = userRepository.findByEmail(email);
            logger.debug("User lookup by email result: {}", userOptional.isPresent());
        }

        final User user;
        if (userOptional.isPresent()) {
            logger.debug("Existing user found, updating details");
            User existingUser = userOptional.get();
            user = updateExistingUser(existingUser, name, pictureUrl, googleId);
            // Email verification is handled in updateExistingUser
        } else {
            logger.debug("No existing user found, creating new user");
            user = registerNewUser(email, name, pictureUrl, googleId);
            logger.debug("New user created with ID: {}", user.getUserId());
        }

        // Verify the user was saved
        User savedUser = userRepository.findById(user.getUserId())
            .orElseThrow(() -> {
                logger.error("User not found after save operation. UserId: {}", user.getUserId());
                return new IllegalStateException("User not found after save operation");
            });
        logger.debug("User verified in database. UserId: {}, Username: {}", savedUser.getUserId(), savedUser.getUsername());

        return user;
    }

    private User registerNewUser(String email, String name, String pictureUrl, String googleId) {
        logger.debug("Registering new OAuth2 user with email: {} and Google ID: {}", email, googleId);
        
        String username = generateUsername(email);
        // Check if username exists and append numbers if needed
        int suffix = 1;
        String baseUsername = username;
        while (userRepository.findByUsername(username).isPresent()) {
            username = baseUsername + suffix++;
        }
        
        User user = User.builder()
                .email(email)
                .username(username)
                .profilePictureUrl(pictureUrl)
                .googleId(googleId)
                .emailVerified(true) // OAuth2 users are pre-verified
                .lastLogin(LocalDateTime.now())
                .build();

        logger.debug("Attempting to save new OAuth2 user with username: {}", username);
        User savedUser = userRepository.save(user);
        logger.debug("User saved successfully. UserId: {}", savedUser.getUserId());
        return savedUser;
    }

    private User updateExistingUser(User user, String name, String pictureUrl, String googleId) {
        logger.debug("Updating existing OAuth2 user: {}", user.getEmail());
        
        // Update Google ID if not set
        if (user.getGoogleId() == null) {
            user.setGoogleId(googleId);
        }
        
        // Always update profile picture and name from Google
        if (pictureUrl != null) {
            user.setProfilePictureUrl(pictureUrl);
        }
        
        // Update last login time
        user.setLastLogin(LocalDateTime.now());

        // Ensure email is verified for OAuth2 users
        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
        }
        
        logger.debug("Saving updated OAuth2 user: {}", user.getEmail());
        User savedUser = userRepository.save(user);
        logger.debug("User updated successfully. UserId: {}", savedUser.getUserId());
        return savedUser;
    }

    private String generateUsername(String email) {
        return email.substring(0, email.indexOf('@'));
    }
} 
