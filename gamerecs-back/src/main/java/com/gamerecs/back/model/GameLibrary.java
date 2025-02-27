package com.gamerecs.back.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing a user's game library.
 * Each user can have only one game library (one-to-one).
 * A library can contain multiple games (many-to-many).
 */
@Entity
@Table(name = "game_libraries")
@Data
public class GameLibrary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "library_id")
    private Long libraryId;

    /**
     * One-to-one relationship with User.
     * Each user can have only one game library.
     */
    @NotNull
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * Many-to-many relationship with Game.
     * A library can contain multiple games, and a game can be in multiple libraries.
     */
    @ManyToMany
    @JoinTable(
        name = "library_games",
        joinColumns = @JoinColumn(name = "library_id"),
        inverseJoinColumns = @JoinColumn(name = "game_id")
    )
    private Set<Game> games = new HashSet<>();
} 
