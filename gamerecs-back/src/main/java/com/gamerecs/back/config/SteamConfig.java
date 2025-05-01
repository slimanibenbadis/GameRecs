package com.gamerecs.back.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;

@Configuration
public class SteamConfig {

    private static final Logger logger = LoggerFactory.getLogger(SteamConfig.class);

    @Value("${steam.api.key:}") // Default to empty string if not set
    private String apiKey;

    @PostConstruct
    public void checkKeyConfiguration() {
        if (!StringUtils.hasText(apiKey) || "YOUR_DEV_STEAM_KEY".equals(apiKey)) {
            logger.warn("Steam API Key is not configured or is using a placeholder value. Please set the STEAM_API_KEY environment variable.");
        } else {
            logger.info("Steam API Key loaded.");
            // Avoid logging the actual key: logger.debug("Steam API Key: {}", apiKey);
        }
    }

    @Bean("steamApiKey")
    public String steamApiKey() {
        // Return empty string if not configured, allows application to start
        // but services using it should handle the lack of a key.
        return apiKey != null ? apiKey : "";
    }
} 
