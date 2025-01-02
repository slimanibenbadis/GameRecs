package com.gamerecs.back.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standardized API error response format.
 */
@Data
public class ApiError {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    private int status;
    private String message;
    private Map<String, String> errors;

    public ApiError() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiError(int status, String message) {
        this();
        this.status = status;
        this.message = message;
    }

    public ApiError(int status, String message, Map<String, String> errors) {
        this(status, message);
        this.errors = errors;
    }
} 
