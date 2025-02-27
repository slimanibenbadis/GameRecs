package com.gamerecs.back.repository;

import com.gamerecs.back.model.GameLibrary;
import com.gamerecs.back.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for GameLibrary entity operations.
 * Extends JpaRepository to inherit standard CRUD operations.
 */
@Repository
public interface GameLibraryRepository extends JpaRepository<GameLibrary, Long> {
    
    /**
     * Find a game library by its associated user.
     *
     * @param user the user whose library to find
     * @return an Optional containing the library if found
     */
    Optional<GameLibrary> findByUser(User user);
    
    /**
     * Check if a game library exists for the given user.
     *
     * @param user the user to check
     * @return true if a library exists for the user
     */
    boolean existsByUser(User user);
} 
