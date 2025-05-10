package com.gamerecs.back.security.crypto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EncryptionServiceTest {

    private static final String VALID_KEY = "TestKeyForEncryptionService12345"; // Min 16 bytes for some encryptors, Spring's stronger handles derivation
    private static final String VALID_SALT = "abcdef1234567890abcdef1234567890"; // 32 hex characters
    private static final String INVALID_SALT_SHORT = "abcdef1234567890";
    private static final String INVALID_SALT_LONG = "abcdef1234567890abcdef12345678901234";
    private static final String INVALID_SALT_CHARS = "ghijklmnopqrstuvwxyz1234567890!!";

    @Test
    void encryptAndDecrypt_shouldReturnOriginalText_whenKeyAndSaltAreValid() {
        EncryptionService encryptionService = new EncryptionService(VALID_KEY, VALID_SALT);
        String originalText = "This is a secret message!";
        
        String encryptedText = encryptionService.encrypt(originalText);
        assertNotNull(encryptedText);
        assertNotEquals(originalText, encryptedText);
        
        String decryptedText = encryptionService.decrypt(encryptedText);
        assertEquals(originalText, decryptedText);
    }

    @Test
    void decrypt_shouldThrowEncryptionOperationException_whenCipherTextIsInvalid() {
        EncryptionService encryptionService = new EncryptionService(VALID_KEY, VALID_SALT);
        String invalidCipherText = "thisIsNotValidCipherTextLookingString==";
        
        Exception exception = assertThrows(EncryptionOperationException.class, () -> {
            encryptionService.decrypt(invalidCipherText);
        });
        assertTrue(exception.getMessage().contains("Decryption failed"));
    }

    @Test
    void decrypt_shouldThrowEncryptionOperationException_whenUsingDifferentKey() {
        EncryptionService encryptionService1 = new EncryptionService(VALID_KEY, VALID_SALT);
        EncryptionService encryptionService2 = new EncryptionService("AnotherKeyForEncryptionService!", VALID_SALT);
        String originalText = "Sensitive data";
        
        String encryptedText = encryptionService1.encrypt(originalText);
        
        Exception exception = assertThrows(EncryptionOperationException.class, () -> {
            encryptionService2.decrypt(encryptedText);
        });
        assertTrue(exception.getMessage().contains("Decryption failed"));
    }

    @Test
    void decrypt_shouldThrowEncryptionOperationException_whenUsingDifferentSalt() {
        EncryptionService encryptionService1 = new EncryptionService(VALID_KEY, VALID_SALT);
        EncryptionService encryptionService2 = new EncryptionService(VALID_KEY, "1234567890abcdef1234567890abcdef"); // Different valid salt
        String originalText = "Another piece of sensitive data";
        
        String encryptedText = encryptionService1.encrypt(originalText);
        
        Exception exception = assertThrows(EncryptionOperationException.class, () -> {
            encryptionService2.decrypt(encryptedText);
        });
        assertTrue(exception.getMessage().contains("Decryption failed"));
    }

    @Test
    void encrypt_shouldReturnNull_whenPlainTextIsNull() {
        EncryptionService encryptionService = new EncryptionService(VALID_KEY, VALID_SALT);
        assertNull(encryptionService.encrypt(null));
    }

    @Test
    void decrypt_shouldReturnNull_whenCipherTextIsNull() {
        EncryptionService encryptionService = new EncryptionService(VALID_KEY, VALID_SALT);
        assertNull(encryptionService.decrypt(null));
    }

    @Test
    void constructor_shouldThrowIllegalArgumentException_whenKeyIsNull() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new EncryptionService(null, VALID_SALT);
        });
        assertEquals("Encryption key cannot be null or empty.", exception.getMessage());
    }

    @Test
    void constructor_shouldThrowIllegalArgumentException_whenKeyIsEmpty() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new EncryptionService("", VALID_SALT);
        });
        assertEquals("Encryption key cannot be null or empty.", exception.getMessage());
    }

    @Test
    void constructor_shouldThrowIllegalArgumentException_whenSaltIsNull() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new EncryptionService(VALID_KEY, null);
        });
        assertEquals("Encryption salt must be a 32-character hex string.", exception.getMessage());
    }

    @Test
    void constructor_shouldThrowIllegalArgumentException_whenSaltIsEmpty() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new EncryptionService(VALID_KEY, "");
        });
        assertEquals("Encryption salt must be a 32-character hex string.", exception.getMessage());
    }

    @Test
    void constructor_shouldThrowIllegalArgumentException_whenSaltIsTooShort() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new EncryptionService(VALID_KEY, INVALID_SALT_SHORT);
        });
        assertEquals("Encryption salt must be a 32-character hex string.", exception.getMessage());
    }

    @Test
    void constructor_shouldThrowIllegalArgumentException_whenSaltIsTooLong() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new EncryptionService(VALID_KEY, INVALID_SALT_LONG);
        });
        assertEquals("Encryption salt must be a 32-character hex string.", exception.getMessage());
    }

    // Note: Testing for non-hex characters in salt is tricky with Encryptors.stronger as it expects hex.
    // The length check (32) implicitly helps, but stronger() might throw its own specific errors for malformed hex.
    // The current implementation explicitly checks length, which is a good first-pass validation.
    // If specific hex format validation inside the constructor was added, that could be tested too.

    @Test
    void encrypt_shouldHandleEmptyString() {
        EncryptionService encryptionService = new EncryptionService(VALID_KEY, VALID_SALT);
        String originalText = "";
        String encryptedText = encryptionService.encrypt(originalText);
        assertNotNull(encryptedText);
        assertNotEquals(originalText, encryptedText);
        String decryptedText = encryptionService.decrypt(encryptedText);
        assertEquals(originalText, decryptedText);
    }
} 
