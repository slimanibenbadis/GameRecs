package com.gamerecs.back.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IGDBConfig {
    
    @Value("${igdb.client-id}")
    private String clientId;
    
    @Value("${igdb.access-token}")
    private String accessToken;
    
    @Bean("igdbClientId")
    public String igdbClientId() {
        return clientId;
    }
    
    @Bean("igdbAccessToken")
    public String igdbAccessToken() {
        return accessToken;
    }
} 
