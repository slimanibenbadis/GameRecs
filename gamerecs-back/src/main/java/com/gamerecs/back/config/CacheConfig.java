package com.gamerecs.back.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuration class for caching using Caffeine
 * This configuration specifically targets the IGDB API integration
 * to reduce latency and minimize external API calls
 */
@Configuration
@EnableCaching
public class CacheConfig {
    private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);
    
    /**
     * Cache TTL in minutes - data from IGDB is relatively static, 
     * so a longer TTL is appropriate
     */
    private static final int CACHE_TTL_MINUTES = 60;
    
    /**
     * Maximum cache size to prevent memory issues
     */
    private static final int CACHE_MAX_SIZE = 1000;
    
    /**
     * Define cache names as constants for reuse and consistency
     */
    public static final String IGDB_GAME_SEARCH_CACHE = "igdbGameSearchCache";

    /**
     * Creates and configures the Caffeine cache builder
     *
     * @return configured Caffeine builder
     */
    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        logger.info("Initializing Caffeine cache with TTL: {} minutes, max size: {} entries", 
                    CACHE_TTL_MINUTES, CACHE_MAX_SIZE);
                    
        return Caffeine.newBuilder()
            .expireAfterWrite(CACHE_TTL_MINUTES, TimeUnit.MINUTES)
            .maximumSize(CACHE_MAX_SIZE)
            .recordStats();
    }

    /**
     * Creates the cache manager bean using the Caffeine configuration
     *
     * @param caffeine the configured Caffeine builder
     * @return the configured CacheManager
     */
    @Bean
    public CacheManager cacheManager(Caffeine<Object, Object> caffeine) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(IGDB_GAME_SEARCH_CACHE);
        cacheManager.setCaffeine(caffeine);
        logger.info("CaffeineCacheManager initialized with cache: {}", IGDB_GAME_SEARCH_CACHE);
        return cacheManager;
    }
} 
