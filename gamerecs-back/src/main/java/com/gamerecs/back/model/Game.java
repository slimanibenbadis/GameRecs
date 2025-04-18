package com.gamerecs.back.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gamerecs.back.util.StringNormalizer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "games")
@Data
@EqualsAndHashCode(exclude = {"genres", "platforms", "publishers", "developers"})
@ToString(exclude = {"genres", "platforms", "publishers", "developers"})
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_id")
    private Long gameId;

    @Column(name = "igdb_id", unique = true, nullable = false)
    private Long igdbId;

    @Column(nullable = false)
    private String title;
    
    @Column(name = "normalized_title")
    private String normalizedTitle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToMany
    @JoinTable(
        name = "game_genres",
        joinColumns = @JoinColumn(name = "game_id"),
        inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Set<Genre> genres = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "game_platforms",
        joinColumns = @JoinColumn(name = "game_id"),
        inverseJoinColumns = @JoinColumn(name = "platform_id")
    )
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Set<Platform> platforms = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "game_publishers",
        joinColumns = @JoinColumn(name = "game_id"),
        inverseJoinColumns = @JoinColumn(name = "publisher_id")
    )
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Set<Publisher> publishers = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "game_developers",
        joinColumns = @JoinColumn(name = "game_id"),
        inverseJoinColumns = @JoinColumn(name = "developer_id")
    )
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Set<Developer> developers = new HashSet<>();
    
    @PrePersist
    @PreUpdate
    private void prepareData() {
        if (title != null) {
            this.normalizedTitle = StringNormalizer.normalize(title);
        }
    }
} 
