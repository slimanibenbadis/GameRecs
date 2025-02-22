package com.gamerecs.back.controller;

import com.gamerecs.back.service.GoogleOAuth2Service;
import com.gamerecs.back.service.JwtService;
import com.gamerecs.back.service.OAuth2UserService;
import com.gamerecs.back.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for handling Google OAuth2.0 authentication callbacks.
 */
@RestController
@RequestMapping("/api/auth/google")
public class GoogleAuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(GoogleAuthController.class);
    private final GoogleOAuth2Service googleOAuth2Service;
    private final JwtService jwtService;
    private final OAuth2UserService oAuth2UserService;

    @Value("${app.oauth2.redirectUri}")
    private String redirectUri;

    @Value("${app.oauth2.expectedState:#{null}}")
    private String expectedState;

    public GoogleAuthController(GoogleOAuth2Service googleOAuth2Service, 
                              JwtService jwtService,
                              OAuth2UserService oAuth2UserService) {
        this.googleOAuth2Service = googleOAuth2Service;
        this.jwtService = jwtService;
        this.oAuth2UserService = oAuth2UserService;
    }

    /**
     * Handles the OAuth2.0 callback from Google.
     * 
     * @param code The authorization code from Google
     * @param state The state parameter for CSRF protection
     * @param error Any error that occurred during the OAuth process
     * @return A RedirectView to the frontend with appropriate parameters
     */
    @GetMapping("/callback")
    public RedirectView handleCallback(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(name = "error", required = false) String error) {
        
        // Handle OAuth2 error
        if (error != null) {
            logger.error("Google OAuth authentication failed - potential invalid credentials or configuration error");
            return new RedirectView(redirectUri + "?error=" + error);
        }

        // Validate authorization code
        if (code == null) {
            logger.error("No authorization code received from Google");
            return new RedirectView(redirectUri + "?error=no_code");
        }

        // Validate state parameter only if expectedState is configured
        if (expectedState != null && (state == null || !state.equals(expectedState))) {
            logger.error("Invalid state parameter received");
            return new RedirectView(redirectUri + "?error=invalid_state");
        }

        logger.debug("Received Google OAuth callback");
        
        try {
            // Exchange authorization code for access token
            String accessToken;
            try {
                accessToken = googleOAuth2Service.exchangeAuthorizationCode(code);
            } catch (RuntimeException e) {
                logger.error("Error exchanging authorization code: {}", e.getMessage());
                if (e.getMessage().contains("Rate limit exceeded")) {
                    return new RedirectView(redirectUri + "?error=rate_limit_exceeded");
                }
                throw e;
            }
            
            if (accessToken == null) {
                logger.error("Failed to exchange authorization code for access token");
                return new RedirectView(redirectUri + "?error=token_exchange_failed");
            }

            // Get user info from Google
            OAuth2User oauth2User;
            try {
                oauth2User = googleOAuth2Service.getUserInfo(accessToken);
            } catch (RuntimeException e) {
                logger.error("Error fetching user info from Google: {}", e.getMessage());
                if (e.getMessage().contains("Rate limit exceeded")) {
                    return new RedirectView(redirectUri + "?error=rate_limit_exceeded");
                }
                return new RedirectView(redirectUri + "?error=google_api_error");
            }

            if (oauth2User == null) {
                logger.error("Failed to get user info from Google");
                return new RedirectView(redirectUri + "?error=user_info_failed");
            }

            // Process OAuth2 user
            User user = oAuth2UserService.processOAuth2User(oauth2User);
            if (user == null) {
                logger.error("Failed to process OAuth2 user");
                return new RedirectView(redirectUri + "?error=user_processing_failed");
            }

            // Generate JWT token
            String token;
            try {
                token = jwtService.generateToken(user.getUsername());
            } catch (Exception e) {
                logger.error("Failed to generate JWT token: {}", e.getMessage());
                return new RedirectView(redirectUri + "?error=token_generation_failed");
            }
            logger.debug("Generated JWT token for user: {}", user.getUsername());

            // Redirect to frontend with token
            String targetUrl = redirectUri + "?token=" + token;
            logger.debug("Redirecting to: {}", targetUrl);
            return new RedirectView(targetUrl);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid redirect URI: {}", e.getMessage());
            return new RedirectView(redirectUri + "?error=invalid_redirect_uri");
        } catch (Exception e) {
            logger.error("Unexpected error during OAuth callback: {}", e.getMessage());
            return new RedirectView(redirectUri + "?error=unexpected_error");
        }
    }
} 
