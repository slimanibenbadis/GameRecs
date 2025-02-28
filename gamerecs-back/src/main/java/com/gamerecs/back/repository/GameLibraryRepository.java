package com.gamerecs.back.repository;

import com.gamerecs.back.model.GameLibrary;
import com.gamerecs.back.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
     * Find a game library by its associated user and eagerly fetch its games.
     *
     * @param user the user whose library to find
     * @return an Optional containing the library with games if found
     */
    @Query("SELECT gl FROM GameLibrary gl LEFT JOIN FETCH gl.games g WHERE gl.user = :user")
    Optional<GameLibrary> findByUserWithGames(@Param("user") User user);
    
    /**
     * Find a game library by its associated user and eagerly fetch its games and all game-related collections.
     * This solves lazy loading exceptions when serializing the response.
     *
     * @param user the user whose library to find
     * @return an Optional containing the library with games and all related collections if found
     */
    @Query("SELECT DISTINCT gl FROM GameLibrary gl " +
           "LEFT JOIN FETCH gl.games g " +
           "LEFT JOIN FETCH g.genres " +
           "LEFT JOIN FETCH g.platforms " +
           "LEFT JOIN FETCH g.publishers " +
           "LEFT JOIN FETCH g.developers " +
           "WHERE gl.user = :user")
    Optional<GameLibrary> findByUserWithGamesAndCollections(@Param("user") User user);
    
    /**
     * Check if a game library exists for the given user.
     *
     * @param user the user to check
     * @return true if a library exists for the user
     */
    boolean existsByUser(User user);
} 
