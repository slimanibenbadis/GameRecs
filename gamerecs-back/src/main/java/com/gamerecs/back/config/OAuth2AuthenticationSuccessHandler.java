package com.gamerecs.back.config;

import com.gamerecs.back.service.JwtService;
import com.gamerecs.back.service.OAuth2UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    @Value("${app.oauth2.redirectUri}")
    private String redirectUri;

    private final JwtService jwtService;
    private final OAuth2UserService oAuth2UserService;

    @Autowired
    public OAuth2AuthenticationSuccessHandler(JwtService jwtService, OAuth2UserService oAuth2UserService) {
        this.jwtService = jwtService;
        this.oAuth2UserService = oAuth2UserService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, 
            Authentication authentication) throws IOException, ServletException {
        logger.debug("Handling OAuth2 authentication success");

        if (response.isCommitted()) {
            logger.warn("Response has already been committed");
            return;
        }

        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            logger.debug("Processing OAuth2 user with attributes: {}", oAuth2User.getAttributes());
            
            // Process the OAuth2 user first
            oAuth2UserService.processOAuth2User(oAuth2User);
            
            String email = oAuth2User.getAttribute("email");
            if (email == null) {
                logger.error("No email found in OAuth2 user attributes");
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not found in OAuth2 user data");
                return;
            }

            String token = jwtService.generateToken(email);
            String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("token", token)
                    .build().toUriString();

            logger.debug("Redirecting to: {}", targetUrl);
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } catch (IllegalStateException e) {
            logger.error("Failed to generate JWT token: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to generate authentication token");
        } catch (Exception e) {
            logger.error("Unexpected error during OAuth2 success handling: {}", e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication failed");
        }
    }
} 
