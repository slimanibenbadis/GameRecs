package com.gamerecs.back.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gamerecs.back.model.Developer;
import com.gamerecs.back.model.Genre;
import com.gamerecs.back.model.Platform;
import com.gamerecs.back.model.Publisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameDtoTest {

    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }
    
    @Test
    void testGameDtoCreation() {
        // Create a GameDto using builder pattern
        GameDto gameDto = GameDto.builder()
                .id(1L)
                .title("The Witcher 3: Wild Hunt")
                .description("An open-world RPG")
                .releaseDate(LocalDate.of(2015, 5, 19))
                .coverImageUrl("https://example.com/witcher3.jpg")
                .genres(new ArrayList<>())
                .platforms(new ArrayList<>())
                .publishers(new ArrayList<>())
                .developers(new ArrayList<>())
                .userRating(90)
                .percentileRank(95)
                .predictedRating(88)
                .backlogStatus("Completed")
                .build();
        
        // Verify object creation was successful
        assertNotNull(gameDto);
        assertEquals(1L, gameDto.getId());
        assertEquals("The Witcher 3: Wild Hunt", gameDto.getTitle());
        assertEquals("An open-world RPG", gameDto.getDescription());
        assertEquals(LocalDate.of(2015, 5, 19), gameDto.getReleaseDate());
        assertEquals("https://example.com/witcher3.jpg", gameDto.getCoverImageUrl());
        assertEquals(90, gameDto.getUserRating());
        assertEquals(95, gameDto.getPercentileRank());
        assertEquals(88, gameDto.getPredictedRating());
        assertEquals("Completed", gameDto.getBacklogStatus());
    }
    
    @Test
    void testGameDtoConstructor() {
        // Create lists for related entities
        List<Genre> genres = new ArrayList<>();
        List<Platform> platforms = new ArrayList<>();
        List<Publisher> publishers = new ArrayList<>();
        List<Developer> developers = new ArrayList<>();
        
        // Create a GameDto using all-args constructor
        GameDto gameDto = new GameDto(
                2L,
                "Cyberpunk 2077",
                "A futuristic open-world game",
                LocalDate.of(2020, 12, 10),
                "https://example.com/cyberpunk.jpg",
                genres,
                platforms,
                publishers,
                developers,
                85,
                80,
                83,
                "In Progress"
        );
        
        // Verify object creation was successful
        assertNotNull(gameDto);
        assertEquals(2L, gameDto.getId());
        assertEquals("Cyberpunk 2077", gameDto.getTitle());
    }
    
    @Test
    void testGameDtoSerialization() throws Exception {
        // Create a simple GameDto
        GameDto gameDto = GameDto.builder()
                .id(3L)
                .title("Elden Ring")
                .description("An open-world action RPG")
                .releaseDate(LocalDate.of(2022, 2, 25))
                .coverImageUrl("https://example.com/eldenring.jpg")
                .userRating(95)
                .percentileRank(98)
                .predictedRating(92)
                .backlogStatus("In Progress")
                .build();
        
        // Serialize to JSON
        String json = objectMapper.writeValueAsString(gameDto);
        
        // Verify JSON contains expected field names
        assertTrue(json.contains("\"id\":3"));
        assertTrue(json.contains("\"title\":\"Elden Ring\""));
        assertTrue(json.contains("\"description\":\"An open-world action RPG\""));
        assertTrue(json.contains("\"releaseDate\":"));
        assertTrue(json.contains("\"coverImageUrl\":\"https://example.com/eldenring.jpg\""));
        assertTrue(json.contains("\"userRating\":95"));
        assertTrue(json.contains("\"percentileRank\":98"));
        assertTrue(json.contains("\"predictedRating\":92"));
        assertTrue(json.contains("\"backlogStatus\":\"In Progress\""));
        
        // Deserialize back to object
        GameDto deserializedDto = objectMapper.readValue(json, GameDto.class);
        
        // Verify deserialization works correctly
        assertEquals(gameDto.getId(), deserializedDto.getId());
        assertEquals(gameDto.getTitle(), deserializedDto.getTitle());
        assertEquals(gameDto.getUserRating(), deserializedDto.getUserRating());
    }
} 
