package com.gamerecs.back.util;

import java.lang.reflect.Field;

/**
 * Utility class for test helpers
 */
public class TestUtils {
    
    /**
     * Sets a field value on an object using reflection
     * Useful for setting private fields in tests
     * 
     * @param target the target object to modify
     * @param fieldName the name of the field to set
     * @param value the value to set
     */
    public static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            if (field == null) {
                throw new IllegalArgumentException("Field '" + fieldName + "' not found on " + target.getClass());
            }
            
            field.setAccessible(true);
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not set field " + fieldName, e);
        }
    }
    
    /**
     * Finds a field in a class or its superclasses
     * 
     * @param clazz the class to search
     * @param fieldName the name of the field to find
     * @return the Field object or null if not found
     */
    private static Field findField(Class<?> clazz, String fieldName) {
        Class<?> searchType = clazz;
        while (searchType != null) {
            try {
                return searchType.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                // Field not found, search in parent class
                searchType = searchType.getSuperclass();
            }
        }
        return null;
    }
} 
