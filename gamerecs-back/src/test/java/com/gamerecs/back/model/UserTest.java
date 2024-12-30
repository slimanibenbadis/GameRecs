package com.gamerecs.back.model;

import com.gamerecs.back.util.BaseUnitTest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Set;
import jakarta.validation.ConstraintViolation;

import static org.junit.jupiter.api.Assertions.*;

class UserTest extends BaseUnitTest {
    private static final Logger logger = LoggerFactory.getLogger(UserTest.class);
    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should create valid user with all required fields")
    void shouldCreateValidUser() {
        logger.debug("Testing user creation with valid fields");
        
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("password123")
                .build();

        var violations = validator.validate(user);
        assertTrue(violations.isEmpty(), "User should be valid");
    }

    @Test
    @DisplayName("Should fail validation with blank username")
    void shouldFailValidationWithBlankUsername() {
        logger.debug("Testing user creation with blank username");
        
        User user = User.builder()
                .username("")
                .email("test@example.com")
                .passwordHash("password123")
                .build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        
        // Log all violations for debugging
        logger.debug("Found {} violations:", violations.size());
        for (ConstraintViolation<User> violation : violations) {
            logger.debug("Violation on path '{}': {}", 
                    violation.getPropertyPath(), 
                    violation.getMessage());
            logger.debug("Constraint: {}", 
                    violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName());
        }
        
        assertFalse(violations.isEmpty(), "User should be invalid");
        assertEquals(2, violations.size(), "Should have two violations: @NotBlank and @Size(min=3)");
        
        boolean hasNotBlankViolation = false;
        boolean hasSizeViolation = false;
        
        for (ConstraintViolation<User> violation : violations) {
            String path = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            String constraintType = violation.getConstraintDescriptor()
                    .getAnnotation().annotationType().getSimpleName();
            
            if (path.equals("username")) {
                logger.debug("Found username violation - Constraint: {}, Message: {}", 
                        constraintType, message);
                if (constraintType.equals("NotBlank")) {
                    hasNotBlankViolation = true;
                } else if (constraintType.equals("Size")) {
                    hasSizeViolation = true;
                }
            }
        }
        
        assertTrue(hasNotBlankViolation, "Should have @NotBlank violation");
        assertTrue(hasSizeViolation, "Should have @Size violation");
    }

    @Test
    @DisplayName("Should fail validation with invalid email")
    void shouldFailValidationWithInvalidEmail() {
        logger.debug("Testing user creation with invalid email");
        
        User user = User.builder()
                .username("testuser")
                .email("invalid-email")
                .passwordHash("password123")
                .build();

        var violations = validator.validate(user);
        assertFalse(violations.isEmpty(), "User should be invalid");
        assertEquals(1, violations.size(), "Should have one violation");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("email")),
                "Should have email violation");
    }

    @Test
    @DisplayName("Should fail validation with blank password hash")
    void shouldFailValidationWithBlankPasswordHash() {
        logger.debug("Testing user creation with blank password hash");
        
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("")
                .build();

        var violations = validator.validate(user);
        assertFalse(violations.isEmpty(), "User should be invalid");
        assertEquals(1, violations.size(), "Should have one violation");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("passwordHash")),
                "Should have passwordHash violation");
    }

    @Test
    @DisplayName("Should create user with optional fields")
    void shouldCreateUserWithOptionalFields() {
        logger.debug("Testing user creation with optional fields");
        
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("password123")
                .profilePictureUrl("http://example.com/pic.jpg")
                .bio("Test bio")
                .joinDate(now)
                .lastLogin(now)
                .build();

        var violations = validator.validate(user);
        assertTrue(violations.isEmpty(), "User should be valid");
        assertNotNull(user.getProfilePictureUrl(), "Profile picture URL should be set");
        assertNotNull(user.getBio(), "Bio should be set");
        assertNotNull(user.getJoinDate(), "Join date should be set");
        assertNotNull(user.getLastLogin(), "Last login should be set");
    }

    @Test
    @DisplayName("Should fail validation with username too short")
    void shouldFailValidationWithUsernameTooShort() {
        logger.debug("Testing user creation with username too short");
        
        User user = User.builder()
                .username("ab")  // Less than 3 characters
                .email("test@example.com")
                .passwordHash("password123")
                .build();

        var violations = validator.validate(user);
        assertFalse(violations.isEmpty(), "User should be invalid");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("username")),
                "Should have username violation");
    }

    @Test
    @DisplayName("Should fail validation with username too long")
    void shouldFailValidationWithUsernameTooLong() {
        logger.debug("Testing user creation with username too long");
        
        String longUsername = "a".repeat(51);  // 51 characters
        User user = User.builder()
                .username(longUsername)
                .email("test@example.com")
                .passwordHash("password123")
                .build();

        var violations = validator.validate(user);
        assertFalse(violations.isEmpty(), "User should be invalid");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("username")),
                "Should have username violation");
    }
} 
