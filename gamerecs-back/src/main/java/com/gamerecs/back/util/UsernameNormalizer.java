package com.gamerecs.back.util;

/**
 * Utility class for normalizing usernames consistently across the application.
 * This ensures that username normalization follows the same rules everywhere.
 */
public final class UsernameNormalizer {
    
    private UsernameNormalizer() {
        // Private constructor to prevent instantiation
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }

    /**
     * Normalizes a username by converting it to lowercase.
     * This method ensures consistent username handling across the application.
     *
     * @param username the username to normalize
     * @return the normalized username, or null if the input is null
     */
    public static String normalize(String username) {
        return username != null ? username.toLowerCase() : null;
    }
} 
