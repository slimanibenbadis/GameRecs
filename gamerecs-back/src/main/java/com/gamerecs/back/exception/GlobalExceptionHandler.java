package com.gamerecs.back.exception;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Global exception handler for the application.
 * Handles validation and other common exceptions.
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handles validation exceptions and returns field-specific error messages.
     *
     * @param ex the validation exception
     * @return ResponseEntity containing validation error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
            logger.debug("Validation error for field '{}': {}", fieldName, errorMessage);
        });
        
        logger.warn("Validation failed: {}", errors);
        ApiError apiError = new ApiError(
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed",
            errors
        );
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handles missing required request parameters.
     *
     * @param ex the missing parameter exception
     * @return ResponseEntity containing error details
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParams(MissingServletRequestParameterException ex) {
        logger.warn("Missing required parameter: {}", ex.getParameterName());
        Map<String, String> errors = new HashMap<>();
        errors.put(ex.getParameterName(), "Parameter is required");
        
        ApiError apiError = new ApiError(
            HttpStatus.BAD_REQUEST.value(),
            "Missing required parameter",
            errors
        );
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handles account disabled exceptions (e.g., unverified email).
     *
     * @param ex the disabled account exception
     * @return ResponseEntity containing error message
     */
    @ExceptionHandler(org.springframework.security.authentication.DisabledException.class)
    public ResponseEntity<ApiError> handleDisabledException(org.springframework.security.authentication.DisabledException ex) {
        logger.warn("Account disabled: {}", ex.getMessage());
        ApiError apiError = new ApiError(
            HttpStatus.UNAUTHORIZED.value(),
            "Please verify your email before logging in"
        );
        return new ResponseEntity<>(apiError, HttpStatus.UNAUTHORIZED);
    }
    
    /**
     * Handles REST client exceptions that occur during API calls.
     *
     * @param ex the REST client exception
     * @return ResponseEntity containing error details
     */
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ApiError> handleRestClientException(RestClientException ex) {
        logger.error("REST client error during external API call: {}", ex.getMessage(), ex);
        ApiError apiError = new ApiError(
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            "Unable to communicate with external service",
            Map.of("detail", ex.getMessage())
        );
        return new ResponseEntity<>(apiError, HttpStatus.SERVICE_UNAVAILABLE);
    }
    
    /**
     * Handles resource access exceptions that occur during API calls (e.g., network issues).
     *
     * @param ex the resource access exception
     * @return ResponseEntity containing error details
     */
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ApiError> handleResourceAccessException(ResourceAccessException ex) {
        logger.error("Network or resource error during external API call: {}", ex.getMessage(), ex);
        ApiError apiError = new ApiError(
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            "Network error while communicating with external service",
            Map.of("detail", ex.getMessage())
        );
        return new ResponseEntity<>(apiError, HttpStatus.SERVICE_UNAVAILABLE);
    }
    
    /**
     * Handles ResponseStatusException thrown by services or controllers.
     *
     * @param ex the response status exception
     * @return ResponseEntity containing error details with the appropriate status code
     */
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(org.springframework.web.server.ResponseStatusException ex) {
        logger.warn("Response status exception: {} - {}", ex.getStatusCode(), ex.getMessage());
        ApiError apiError = new ApiError(
            ex.getStatusCode().value(),
            ex.getReason() != null ? ex.getReason() : "Error processing request"
        );
        return new ResponseEntity<>(apiError, ex.getStatusCode());
    }
    
    /**
     * Handles EntityNotFoundException and returns 404 with a standard ApiError JSON.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleEntityNotFoundException(EntityNotFoundException ex, HttpServletRequest request) {
        logger.error("Entity not found at path {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        ApiError apiError = new ApiError(HttpStatus.NOT_FOUND.value(), "Game not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
    }
    
    /**
     * Handles IllegalArgumentException and returns 409 for username/email conflicts, 400 otherwise, always as ApiError.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        logger.error("Illegal argument at path {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        String message = ex.getMessage();
        if ("Username already exists".equals(message)) {
            ApiError apiError = new ApiError(HttpStatus.CONFLICT.value(), "Registration failed", Map.of("username", "This username is already taken"));
            return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
        } else if ("Email already exists".equals(message)) {
            ApiError apiError = new ApiError(HttpStatus.CONFLICT.value(), "Registration failed", Map.of("email", "This email is already registered"));
            return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
        } else {
            ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST.value(), message != null ? message : "Bad request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
        }
    }
    
    /**
     * Handles all other unhandled exceptions and returns 500 with ApiError.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex, HttpServletRequest request) {
        logger.error("Internal server error at path {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        ApiError apiError = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiError);
    }

    /**
     * Handles authentication failures (invalid credentials).
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentialsException(BadCredentialsException ex, HttpServletRequest request) {
        logger.error("Authentication failed at path {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        ApiError apiError = new ApiError(HttpStatus.UNAUTHORIZED.value(), "Invalid credentials");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiError);
    }

    /**
     * Handles user not found during authentication.
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiError> handleUsernameNotFoundException(UsernameNotFoundException ex, HttpServletRequest request) {
        logger.error("User not found at path {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST.value(), "User not found");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    /**
     * Handles generic Spring Security authentication exceptions.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        logger.error("Authentication exception at path {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        if (msg.contains("not found")) {
            ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST.value(), "User not found");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
        } else {
            ApiError apiError = new ApiError(HttpStatus.UNAUTHORIZED.value(), "Invalid credentials");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiError);
        }
    }

    /**
     * Handles IllegalStateException and returns 400 with the exception message.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalStateException(IllegalStateException ex, HttpServletRequest request) {
        logger.error("Illegal state at path {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST.value(), ex.getMessage() != null ? ex.getMessage() : "Bad request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }
} 
