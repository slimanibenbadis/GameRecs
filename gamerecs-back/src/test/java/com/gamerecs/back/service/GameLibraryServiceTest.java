package com.gamerecs.back.service;

import com.gamerecs.back.model.GameLibrary;
import com.gamerecs.back.model.User;
import com.gamerecs.back.repository.GameLibraryRepository;
import com.gamerecs.back.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GameLibraryServiceTest {

    @Mock
    private GameLibraryRepository gameLibraryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GameLibraryService gameLibraryService;

    private User testUser;
    private GameLibrary testLibrary;
    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setUserId(userId);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        // Create test library
        testLibrary = new GameLibrary();
        testLibrary.setLibraryId(1L);
        testLibrary.setUser(testUser);
    }

    @Test
    void getLibraryForUser_ValidUser_ReturnsLibrary() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(gameLibraryRepository.findByUserWithGames(testUser)).thenReturn(Optional.of(testLibrary));

        // Act
        GameLibrary result = gameLibraryService.getLibraryForUser(userId);

        // Assert
        assertNotNull(result);
        assertEquals(testLibrary.getLibraryId(), result.getLibraryId());
        assertEquals(testUser, result.getUser());
        
        // Verify repository methods were called
        verify(userRepository, times(1)).findById(userId);
        verify(gameLibraryRepository, times(1)).findByUserWithGames(testUser);
    }

    @Test
    void getLibraryForUser_UserNotFound_ThrowsUnauthorized() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> gameLibraryService.getLibraryForUser(userId)
        );
        
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("User not found", exception.getReason());
        
        // Verify repository method was called
        verify(userRepository, times(1)).findById(userId);
        verify(gameLibraryRepository, never()).findByUser(any());
    }

    @Test
    void getLibraryForUser_LibraryNotFound_ThrowsNotFound() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(gameLibraryRepository.findByUserWithGames(testUser)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> gameLibraryService.getLibraryForUser(userId)
        );
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Game library not found", exception.getReason());
        
        // Verify repository methods were called
        verify(userRepository, times(1)).findById(userId);
        verify(gameLibraryRepository, times(1)).findByUserWithGames(testUser);
    }
} 
