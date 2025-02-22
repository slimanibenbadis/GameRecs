package com.gamerecs.back.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base test configuration class that provides common beans and utilities for testing
 */
@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean("igdbClientId")
    @Primary
    public String igdbClientId() {
        return "test-client-id";
    }
    
    @Bean("igdbAccessToken")
    @Primary
    public String igdbAccessToken() {
        return "test-access-token";
    }

    // Add more test-specific beans as needed
} 
