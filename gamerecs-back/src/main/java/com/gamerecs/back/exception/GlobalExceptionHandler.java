package com.gamerecs.back.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

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
     * Handles user registration conflicts (duplicate username/email).
     *
     * @param ex the illegal argument exception
     * @return ResponseEntity containing error details
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleUserRegistrationConflict(IllegalArgumentException ex) {
        String message = ex.getMessage();
        HttpStatus status = HttpStatus.CONFLICT;
        Map<String, String> errors = new HashMap<>();

        if ("Username already exists".equals(message)) {
            logger.warn("Registration failed: Username already exists");
            errors.put("username", "This username is already taken");
        } else if ("Email already exists".equals(message)) {
            logger.warn("Registration failed: Email already exists");
            errors.put("email", "This email is already registered");
        } else {
            // For other IllegalArgumentException cases, delegate to the generic handler
            return handleGenericIllegalArgumentException(ex);
        }

        ApiError apiError = new ApiError(
            status.value(),
            "Registration failed",
            errors
        );
        return new ResponseEntity<>(apiError, status);
    }

    /**
     * Handles other illegal argument exceptions.
     *
     * @param ex the illegal argument exception
     * @return ResponseEntity containing error message
     */
    private ResponseEntity<ApiError> handleGenericIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("Invalid argument: {}", ex.getMessage());
        ApiError apiError = new ApiError(
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage()
        );
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles illegal state exceptions.
     *
     * @param ex the illegal state exception
     * @return ResponseEntity containing error message
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalStateException(IllegalStateException ex) {
        logger.warn("Invalid state: {}", ex.getMessage());
        ApiError apiError = new ApiError(
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage()
        );
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles authentication failures.
     *
     * @param ex the bad credentials exception
     * @return ResponseEntity containing error message
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentialsException(BadCredentialsException ex) {
        logger.warn("Authentication failed: {}", ex.getMessage());
        ApiError apiError = new ApiError(
            HttpStatus.UNAUTHORIZED.value(),
            "Invalid credentials"
        );
        return new ResponseEntity<>(apiError, HttpStatus.UNAUTHORIZED);
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
     * Handles all other unhandled exceptions.
     *
     * @param ex the exception
     * @return ResponseEntity containing generic error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex) {
        logger.error("Unexpected error occurred", ex);
        ApiError apiError = new ApiError(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An unexpected error occurred"
        );
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }
} 
