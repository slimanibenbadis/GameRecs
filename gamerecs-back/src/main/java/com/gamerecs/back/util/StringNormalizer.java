package com.gamerecs.back.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Utility class for normalizing strings to facilitate accent-insensitive and punctuation-insensitive searches.
 * This class provides methods to remove diacritics (accents) and punctuation from strings.
 */
public final class StringNormalizer {
    
    // Pattern for any character that is not a letter, number, or whitespace
    private static final Pattern PUNCTUATION_PATTERN = Pattern.compile("[^\\p{L}\\p{N}\\s]");
    
    // Pattern for specific punctuation that should be converted to spaces for better readability
    private static final Pattern SPACE_PUNCTUATION_PATTERN = Pattern.compile("[-]");
    
    // Pattern for ampersand character to be replaced with "and"
    private static final Pattern AMPERSAND_PATTERN = Pattern.compile("&");
    
    // Pattern for diacritical marks
    private static final Pattern DIACRITICAL_MARKS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}");
    
    private StringNormalizer() {
        // Private constructor to prevent instantiation
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }
    
    /**
     * Normalizes a string by converting to lowercase, removing diacritics (accents),
     * and removing punctuation.
     * 
     * @param input the string to normalize
     * @return the normalized string, or null if the input is null
     */
    public static String normalize(String input) {
        if (input == null) {
            return null;
        }
        
        // Convert to lowercase
        String result = input.toLowerCase();
        
        // Remove diacritics (decompose, then remove combining marks)
        result = Normalizer.normalize(result, Normalizer.Form.NFD);
        result = DIACRITICAL_MARKS_PATTERN.matcher(result).replaceAll("");
        
        // Replace ampersands with "and"
        result = AMPERSAND_PATTERN.matcher(result).replaceAll(" and ");
        
        // Convert specific punctuation to spaces first (like hyphens)
        result = SPACE_PUNCTUATION_PATTERN.matcher(result).replaceAll(" ");
        
        // Remove other punctuation
        result = PUNCTUATION_PATTERN.matcher(result).replaceAll("");
        
        // Replace multiple spaces with a single space and trim
        result = result.replaceAll("\\s+", " ").trim();
        
        return result;
    }
} 
