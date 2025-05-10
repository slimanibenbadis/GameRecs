package com.gamerecs.back.security.crypto;

public class EncryptionOperationException extends RuntimeException {

    public EncryptionOperationException(String message) {
        super(message);
    }

    public EncryptionOperationException(String message, Throwable cause) {
        super(message, cause);
    }
} 
