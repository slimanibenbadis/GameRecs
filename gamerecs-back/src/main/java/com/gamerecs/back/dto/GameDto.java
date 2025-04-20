package com.gamerecs.back.dto;

import java.time.LocalDate;
import java.util.List;

import com.gamerecs.back.model.Developer;
import com.gamerecs.back.model.Genre;
import com.gamerecs.back.model.Platform;
import com.gamerecs.back.model.Publisher;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Game entities when sending to the front-end.
 * Includes user-specific data like ratings and backlog status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameDto {
    
    @NotNull(message = "Game ID is required")
    private Long id;
    
    @NotBlank(message = "Game title is required")
    private String title;
    
    private String description;
    
    private LocalDate releaseDate;
    
    private String coverImageUrl;
    
    private List<Genre> genres;
    
    private List<Platform> platforms;
    
    private List<Publisher> publishers;
    
    private List<Developer> developers;
    
    // User-specific data
    private Integer userRating;  // The user's rating (0-100)
    
    private Integer percentileRank;  // The percentile rank of the user's rating
    
    private Integer predictedRating;  // Predicted rating based on similar users
    
    private String backlogStatus;  // "To Play", "In Progress", "Completed", "Abandoned"
} 
