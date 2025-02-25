package com.gamerecs.back.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.TimeUnit;

/**
 * Base test configuration class that provides common beans and utilities for testing
 */
@TestConfiguration
@EnableCaching
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
    
    /**
     * Configures a Caffeine cache for testing
     */
    @Bean
    @Primary
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(100)
            .recordStats();
    }

    /**
     * Configures the cache manager for testing
     */
    @Bean
    @Primary
    public CacheManager cacheManager(Caffeine<Object, Object> caffeine) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(caffeine);
        cacheManager.setCacheNames(java.util.List.of(CacheConfig.IGDB_GAME_SEARCH_CACHE));
        return cacheManager;
    }

    // Add more test-specific beans as needed
} 
