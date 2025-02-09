package com.gamerecs.back.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UsernameNormalizerTest {

    @Test
    void normalize_WithMixedCase_ReturnsLowerCase() {
        assertEquals("johndoe", UsernameNormalizer.normalize("JohnDoe"));
        assertEquals("user123", UsernameNormalizer.normalize("User123"));
    }

    @Test
    void normalize_WithNullInput_ReturnsNull() {
        assertNull(UsernameNormalizer.normalize(null));
    }

    @Test
    void normalize_WithEmptyString_ReturnsEmptyString() {
        assertEquals("", UsernameNormalizer.normalize(""));
    }

    @Test
    void normalize_WithSpecialCharacters_PreservesCharacters() {
        assertEquals("john.doe@123", UsernameNormalizer.normalize("John.Doe@123"));
    }
} 
