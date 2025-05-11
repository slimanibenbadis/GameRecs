package com.gamerecs.back.exception;

import com.gamerecs.back.exception.ApiError;
import com.gamerecs.back.exception.GlobalExceptionHandler;
import com.gamerecs.back.exception.TooManyRequestsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private BindingResult mockBindingResult;
    
    @Mock
    private MethodParameter mockMethodParameter;

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        // Removed: when(mockRequest.getRequestURI()).thenReturn("/test-path");
    }

    @Test
    void handleValidationExceptions() {
        FieldError fieldError = new FieldError("objectName", "fieldName", "defaultMessage");
        when(mockBindingResult.getAllErrors()).thenReturn(List.of(fieldError));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(mockMethodParameter, mockBindingResult);

        ResponseEntity<ApiError> response = globalExceptionHandler.handleValidationExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Validation failed", response.getBody().getMessage());
        assertTrue(response.getBody().getErrors().containsKey("fieldName"));
        assertEquals("defaultMessage", response.getBody().getErrors().get("fieldName"));
    }

    @Test
    void handleMissingParams() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("paramName", "String");
        ResponseEntity<ApiError> response = globalExceptionHandler.handleMissingParams(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Missing required parameter", response.getBody().getMessage());
        assertTrue(response.getBody().getErrors().containsKey("paramName"));
        assertEquals("Parameter is required", response.getBody().getErrors().get("paramName"));
    }

    @Test
    void handleDisabledException() {
        DisabledException ex = new DisabledException("User is disabled");
        ResponseEntity<ApiError> response = globalExceptionHandler.handleDisabledException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Please verify your email before logging in", response.getBody().getMessage());
    }

    @Test
    void handleRestClientException() {
        RestClientException ex = new RestClientException("External service error");
        ResponseEntity<ApiError> response = globalExceptionHandler.handleRestClientException(ex);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Unable to communicate with external service", response.getBody().getMessage());
        assertTrue(response.getBody().getErrors().containsKey("detail"));
        assertEquals("External service error", response.getBody().getErrors().get("detail"));
    }

    @Test
    void handleResourceAccessException() {
        ResourceAccessException ex = new ResourceAccessException("Network error");
        ResponseEntity<ApiError> response = globalExceptionHandler.handleResourceAccessException(ex);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Network error while communicating with external service", response.getBody().getMessage());
        assertTrue(response.getBody().getErrors().containsKey("detail"));
        assertEquals("Network error", response.getBody().getErrors().get("detail"));
    }

    @Test
    void handleResponseStatusException_withReason() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        ResponseEntity<ApiError> response = globalExceptionHandler.handleResponseStatusException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Resource not found", response.getBody().getMessage());
    }
    
    @Test
    void handleResponseStatusException_withoutReason() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        ResponseEntity<ApiError> response = globalExceptionHandler.handleResponseStatusException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Error processing request", response.getBody().getMessage());
    }

    @Test
    void handleEntityNotFoundException() {
        EntityNotFoundException ex = new EntityNotFoundException("Game not found");
        when(mockRequest.getRequestURI()).thenReturn("/test-path");
        ResponseEntity<ApiError> response = globalExceptionHandler.handleEntityNotFoundException(ex, mockRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Game not found", response.getBody().getMessage());
    }

    @Test
    void handleIllegalArgumentException_usernameExists() {
        IllegalArgumentException ex = new IllegalArgumentException("Username already exists");
        when(mockRequest.getRequestURI()).thenReturn("/test-path");
        ResponseEntity<ApiError> response = globalExceptionHandler.handleIllegalArgumentException(ex, mockRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Registration failed", response.getBody().getMessage());
        assertTrue(response.getBody().getErrors().containsKey("username"));
        assertEquals("This username is already taken", response.getBody().getErrors().get("username"));
    }

    @Test
    void handleIllegalArgumentException_emailExists() {
        IllegalArgumentException ex = new IllegalArgumentException("Email already exists");
        when(mockRequest.getRequestURI()).thenReturn("/test-path");
        ResponseEntity<ApiError> response = globalExceptionHandler.handleIllegalArgumentException(ex, mockRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Registration failed", response.getBody().getMessage());
        assertTrue(response.getBody().getErrors().containsKey("email"));
        assertEquals("This email is already registered", response.getBody().getErrors().get("email"));
    }

    @Test
    void handleIllegalArgumentException_other() {
        IllegalArgumentException ex = new IllegalArgumentException("Some other error");
        when(mockRequest.getRequestURI()).thenReturn("/test-path");
        ResponseEntity<ApiError> response = globalExceptionHandler.handleIllegalArgumentException(ex, mockRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Some other error", response.getBody().getMessage());
    }
    
    @Test
    void handleIllegalArgumentException_nullMessage() {
        IllegalArgumentException ex = new IllegalArgumentException(); // No message
        when(mockRequest.getRequestURI()).thenReturn("/test-path");
        ResponseEntity<ApiError> response = globalExceptionHandler.handleIllegalArgumentException(ex, mockRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Bad request", response.getBody().getMessage()); // Default message
    }

    @Test
    void handleGenericException() {
        Exception ex = new Exception("Generic error");
        when(mockRequest.getRequestURI()).thenReturn("/test-path");
        ResponseEntity<ApiError> response = globalExceptionHandler.handleGenericException(ex, mockRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Internal server error", response.getBody().getMessage());
    }

    @Test
    void handleBadCredentialsException() {
        BadCredentialsException ex = new BadCredentialsException("Invalid credentials");
        when(mockRequest.getRequestURI()).thenReturn("/test-path");
        ResponseEntity<ApiError> response = globalExceptionHandler.handleBadCredentialsException(ex, mockRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid credentials", response.getBody().getMessage());
    }

    @Test
    void handleUsernameNotFoundException() {
        UsernameNotFoundException ex = new UsernameNotFoundException("User not found");
        when(mockRequest.getRequestURI()).thenReturn("/test-path");
        ResponseEntity<ApiError> response = globalExceptionHandler.handleUsernameNotFoundException(ex, mockRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("User not found", response.getBody().getMessage());
    }

    @Test
    void handleAuthenticationException_notFound() {
        AuthenticationException ex = new AuthenticationException("User foo not found") {};
        when(mockRequest.getRequestURI()).thenReturn("/test-path");
        ResponseEntity<ApiError> response = globalExceptionHandler.handleAuthenticationException(ex, mockRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("User not found", response.getBody().getMessage());
    }

    @Test
    void handleAuthenticationException_other() {
        AuthenticationException ex = new AuthenticationException("Other auth error") {};
        when(mockRequest.getRequestURI()).thenReturn("/test-path");
        ResponseEntity<ApiError> response = globalExceptionHandler.handleAuthenticationException(ex, mockRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid credentials", response.getBody().getMessage());
    }
    
    @Test
    void handleAuthenticationException_nullMessage() {
        AuthenticationException ex = new AuthenticationException(null) {}; // No message
        when(mockRequest.getRequestURI()).thenReturn("/test-path");
        ResponseEntity<ApiError> response = globalExceptionHandler.handleAuthenticationException(ex, mockRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid credentials", response.getBody().getMessage());
    }

    @Test
    void handleIllegalStateException_withMessage() {
        IllegalStateException ex = new IllegalStateException("Illegal state occurred");
        when(mockRequest.getRequestURI()).thenReturn("/test-path");
        ResponseEntity<ApiError> response = globalExceptionHandler.handleIllegalStateException(ex, mockRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Illegal state occurred", response.getBody().getMessage());
    }
    
    @Test
    void handleIllegalStateException_nullMessage() {
        IllegalStateException ex = new IllegalStateException(); // No message
        when(mockRequest.getRequestURI()).thenReturn("/test-path");
        ResponseEntity<ApiError> response = globalExceptionHandler.handleIllegalStateException(ex, mockRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Bad request", response.getBody().getMessage());
    }

    @Test
    void handleTooManyRequestsException() {
        TooManyRequestsException ex = new TooManyRequestsException("Rate limit exceeded");
        when(mockRequest.getRequestURI()).thenReturn("/test-path");
        ResponseEntity<ApiError> response = globalExceptionHandler.handleTooManyRequestsException(ex, mockRequest);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Rate limit exceeded", response.getBody().getMessage());
    }
} 
