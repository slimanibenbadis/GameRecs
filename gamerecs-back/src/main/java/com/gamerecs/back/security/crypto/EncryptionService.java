package com.gamerecs.back.security.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

@Service
public class EncryptionService {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionService.class);

    private final TextEncryptor textEncryptor;

    public EncryptionService(@Value("${app.encryption.key}") String encryptionKey,
                             @Value("${app.encryption.salt}") String salt) {
        if (encryptionKey == null || encryptionKey.isEmpty()) {
            logger.error("Encryption key is not configured. Please set app.encryption.key.");
            throw new IllegalArgumentException("Encryption key cannot be null or empty.");
        }
        if (salt == null || salt.isEmpty() || salt.length() != 32) {
            // A 16-byte salt is typically represented by 32 hexadecimal characters.
            logger.error("Encryption salt is not configured properly. Please set app.encryption.salt to a 32-character hex string.");
            throw new IllegalArgumentException("Encryption salt must be a 32-character hex string.");
        }
        logger.info("Initializing EncryptionService.");
        try {
            this.textEncryptor = Encryptors.delux(encryptionKey, salt);
            logger.info("TextEncryptor initialized successfully using delux encryption (AES-256 GCM).");
        } catch (Exception e) {
            logger.error("Failed to initialize TextEncryptor. Ensure the key and salt are valid and JCE Unlimited Strength Jurisdiction Policy files are installed if needed.", e);
            throw new IllegalStateException("Could not initialize TextEncryptor", e);
        }
    }

    /**
     * Encrypts the given plain text.
     *
     * @param plainText The text to encrypt.
     * @return The encrypted text (ciphertext).
     * @throws IllegalArgumentException if plainText is null.
     */
    public String encrypt(String plainText) {
        if (plainText == null) {
            // Depending on requirements, could return null or encrypt an empty string.
            // For API keys, null is unlikely, but good to define behavior.
            logger.warn("Attempted to encrypt null plainText. Returning null.");
            return null;
        }
        try {
            String encryptedText = textEncryptor.encrypt(plainText);
            logger.debug("Successfully encrypted data.");
            return encryptedText;
        } catch (Exception e) {
            logger.error("Encryption failed.", e);
            // Rethrow as a runtime exception or a custom exception
            throw new EncryptionOperationException("Encryption failed", e);
        }
    }

    /**
     * Decrypts the given ciphertext.
     *
     * @param cipherText The encrypted text to decrypt.
     * @return The original plain text.
     * @throws IllegalArgumentException if cipherText is null.
     * @throws EncryptionOperationException if decryption fails (e.g., wrong key, malformed ciphertext).
     */
    public String decrypt(String cipherText) {
        if (cipherText == null) {
            logger.warn("Attempted to decrypt null cipherText. Returning null.");
            return null;
        }
        try {
            String decryptedText = textEncryptor.decrypt(cipherText);
            logger.debug("Successfully decrypted data.");
            return decryptedText;
        } catch (Exception e) {
            // This can happen if the key is wrong, salt is wrong, or ciphertext is corrupted/not valid
            logger.error("Decryption failed. This could be due to an incorrect key/salt, or corrupted data. Ciphertext: '{}'", cipherText, e);
            throw new EncryptionOperationException("Decryption failed. Ensure the correct key and salt are used and the data is not corrupted.", e);
        }
    }
} 
