package com.gamerecs.back.repository;

import com.gamerecs.back.model.Game;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    Optional<Game> findByIgdbId(Long igdbId);
    boolean existsByIgdbId(Long igdbId);
    
    /**
     * Find games where the title contains the search query string (case insensitive).
     *
     * @param query the search query
     * @param pageable pagination information
     * @return a Page of Game objects matching the search criteria
     */
    @Query("SELECT g FROM Game g WHERE LOWER(g.title) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY g.title ASC")
    Page<Game> findByTitleContainingIgnoreCase(@Param("query") String query, Pageable pageable);
} 
