package com.gamerecs.back.repository;

import com.gamerecs.back.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    Optional<Game> findByIgdbId(Long igdbId);
    boolean existsByIgdbId(Long igdbId);
} 
