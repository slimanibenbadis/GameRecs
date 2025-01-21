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
            processOAuth2User(oauth2User);
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

    protected OAuth2User processOAuth2User(OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String pictureUrl = (String) attributes.get("picture");

        logger.debug("Processing OAuth2 user with email: {}", email);

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            updateExistingUser(user, name, pictureUrl);
        } else {
            user = registerNewUser(email, name, pictureUrl);
        }

        return oauth2User;
    }

    private User registerNewUser(String email, String name, String pictureUrl) {
        logger.debug("Registering new OAuth2 user with email: {}", email);
        
        User user = User.builder()
                .email(email)
                .username(generateUsername(email))
                .profilePictureUrl(pictureUrl)
                .emailVerified(true) // OAuth2 users are pre-verified
                .build();

        return userRepository.save(user);
    }

    private void updateExistingUser(User user, String name, String pictureUrl) {
        logger.debug("Updating existing OAuth2 user: {}", user.getEmail());
        
        user.setProfilePictureUrl(pictureUrl);
        userRepository.save(user);
    }

    private String generateUsername(String email) {
        return email.substring(0, email.indexOf('@'));
    }
} 
