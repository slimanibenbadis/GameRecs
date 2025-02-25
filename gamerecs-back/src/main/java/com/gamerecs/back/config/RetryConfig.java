package com.gamerecs.back.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.client.RestClientException;

/**
 * Configuration for Spring Retry functionality
 * Provides retry capabilities for transient failures in API calls
 */
@Configuration
@EnableRetry
public class RetryConfig {

    /**
     * Maximum number of retry attempts for failed API calls
     */
    private static final int MAX_RETRY_ATTEMPTS = 3;
    
    /**
     * Initial backoff period in milliseconds
     */
    private static final long INITIAL_BACKOFF_INTERVAL = 1000L;
    
    /**
     * Multiplier for backoff period between retries
     */
    private static final double BACKOFF_MULTIPLIER = 2.0;
    
    /**
     * Creates a RetryTemplate bean with exponential backoff policy
     * 
     * @return configured RetryTemplate
     */
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Configure exponential backoff
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(INITIAL_BACKOFF_INTERVAL);
        backOffPolicy.setMultiplier(BACKOFF_MULTIPLIER);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        // Configure retry policy - retry on RestClientException
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(RestClientException.class, true);
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                MAX_RETRY_ATTEMPTS, 
                retryableExceptions,
                true
        );
        retryTemplate.setRetryPolicy(retryPolicy);
        
        return retryTemplate;
    }
} 
