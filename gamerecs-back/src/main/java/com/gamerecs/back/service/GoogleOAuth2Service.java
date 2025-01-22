package com.gamerecs.back.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class GoogleOAuth2Service {
    
    private static final Logger logger = LoggerFactory.getLogger(GoogleOAuth2Service.class);
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
    
    private final RestTemplate restTemplate;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final ObjectMapper objectMapper;

    public GoogleOAuth2Service(
            RestTemplate restTemplate,
            @Value("${spring.security.oauth2.client.registration.google.client-id}") String clientId,
            @Value("${spring.security.oauth2.client.registration.google.client-secret}") String clientSecret,
            @Value("${spring.security.oauth2.client.registration.google.redirect-uri}") String redirectUri) {
        this.restTemplate = restTemplate;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Exchanges an authorization code for an access token using Google's OAuth2 token endpoint.
     *
     * @param code The authorization code to exchange
     * @return The access token if successful, null otherwise
     */
    public String exchangeAuthorizationCode(String code) {
        logger.debug("Exchanging authorization code for access token");
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("code", code);
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("redirect_uri", redirectUri);
            params.add("grant_type", "authorization_code");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                TOKEN_ENDPOINT,
                HttpMethod.POST,
                request,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode accessTokenNode = root.path("access_token");
                if (!accessTokenNode.isMissingNode() && !accessTokenNode.isNull() && !accessTokenNode.asText().isEmpty()) {
                    String accessToken = accessTokenNode.asText();
                    logger.debug("Successfully obtained access token");
                    return accessToken;
                }
                logger.error("Access token missing or empty in response: {}", response.getBody());
                return null;
            }

            logger.error("Failed to exchange authorization code: {}", response.getBody());
            return null;
            
        } catch (Exception e) {
            logger.error("Error exchanging authorization code for access token", e);
            return null;
        }
    }

    /**
     * Fetches user information from Google's userinfo endpoint using the access token.
     *
     * @param accessToken The access token to use for authentication
     * @return OAuth2User containing user information if successful, null otherwise
     */
    public OAuth2User getUserInfo(String accessToken) {
        logger.debug("Fetching user info from Google");
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<?> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                GOOGLE_USERINFO_URL,
                HttpMethod.GET,
                request,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode userInfo = objectMapper.readTree(response.getBody());
                Map<String, Object> attributes = new HashMap<>();
                
                // Extract user information
                attributes.put("sub", userInfo.path("sub").asText());
                attributes.put("email", userInfo.path("email").asText());
                attributes.put("name", userInfo.path("name").asText());
                attributes.put("picture", userInfo.path("picture").asText());
                
                logger.debug("Successfully fetched user info for email: {}", attributes.get("email"));
                return new DefaultOAuth2User(
                    Collections.emptySet(),
                    attributes,
                    "email"
                );
            }

            logger.error("Failed to fetch user info: {}", response.getBody());
            return null;

        } catch (Exception e) {
            logger.error("Error fetching user info from Google", e);
            return null;
        }
    }

    public record GoogleUserInfo(String email, String name, String picture) {}

    public GoogleUserInfo fetchUserDetails(String accessToken) {
        logger.debug("Fetching user details from Google with access token: {}", accessToken.substring(0, 10) + "...");
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            var response = restTemplate.exchange(
                GOOGLE_USERINFO_URL,
                HttpMethod.GET,
                requestEntity,
                String.class
            );

            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            
            var userInfo = new GoogleUserInfo(
                jsonNode.get("email").asText(),
                jsonNode.get("name").asText(),
                jsonNode.get("picture").asText()
            );
            
            logger.debug("Successfully fetched user details for email: {}", userInfo.email());
            return userInfo;
            
        } catch (Exception e) {
            logger.error("Failed to fetch user details from Google", e);
            throw new RuntimeException("Failed to fetch user details from Google: " + e.getMessage());
        }
    }
} 
