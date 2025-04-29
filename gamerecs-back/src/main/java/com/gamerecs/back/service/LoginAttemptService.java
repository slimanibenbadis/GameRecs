package com.gamerecs.back.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class LoginAttemptService {

    private static final Logger logger = LoggerFactory.getLogger(LoginAttemptService.class);

    @Value("${app.security.login.max-attempts:5}")
    private int MAX_ATTEMPTS;

    @Value("${app.security.login.lockout-duration-minutes:10}")
    private int LOCKOUT_DURATION_MINUTES;

    private final Cache<String, Bucket> cache;

    public LoginAttemptService() {
        // Cache buckets per IP address, expire after lockout duration + buffer
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(LOCKOUT_DURATION_MINUTES + 1, TimeUnit.MINUTES)
                .maximumSize(10000) // Limit cache size
                .build();
        logger.info("LoginAttemptService initialized with MAX_ATTEMPTS={}, LOCKOUT_DURATION_MINUTES={}", MAX_ATTEMPTS, LOCKOUT_DURATION_MINUTES);
    }

    public void loginSucceeded(String key) {
        // Successful login resets the bucket (removes from cache)
        cache.invalidate(key);
        logger.debug("Login attempt cache cleared for key: {}", key);
    }

    public void loginFailed(String key) {
        Bucket bucket = resolveBucket(key);
        // Consume a token for the failed attempt. This doesn't block yet,
        // but subsequent calls to tryConsume will fail if the limit is reached.
        bucket.tryConsume(1);
        logger.warn("Failed login attempt recorded for key: {}. Tokens remaining: {}", key, bucket.getAvailableTokens());
    }

    public boolean isBlocked(String key) {
        Bucket bucket = resolveBucket(key);
        // Check if consumption is possible. If not, the user is blocked.
        boolean blocked = !bucket.tryConsume(1);
        if (blocked) {
            logger.warn("Login attempt blocked for key: {} due to exceeding rate limit.", key);
        } else {
             // Important: If we successfully consumed a token here just for checking,
             // we need to add it back immediately as this method is only for checking,
             // not for consuming attempts during the actual login process.
             // The actual consumption happens in the AuthenticationManager/Provider.
             bucket.addTokens(1);
             logger.debug("Login attempt check passed for key: {}. Tokens available: {}", key, bucket.getAvailableTokens());
        }
        return blocked;
    }

    private Bucket resolveBucket(String key) {
        // Get existing bucket or create a new one if not present
        return cache.get(key, this::createNewBucket);
    }

    private Bucket createNewBucket(String key) {
        logger.debug("Creating new rate limiting bucket for key: {}", key);
        // Allow MAX_ATTEMPTS per LOCKOUT_DURATION_MINUTES
        Refill refill = Refill.intervally(MAX_ATTEMPTS, Duration.ofMinutes(LOCKOUT_DURATION_MINUTES));
        Bandwidth limit = Bandwidth.classic(MAX_ATTEMPTS, refill);
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
} 
