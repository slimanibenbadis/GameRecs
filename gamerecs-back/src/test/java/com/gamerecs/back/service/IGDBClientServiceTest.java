package com.gamerecs.back.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamerecs.back.dto.IGDBGameDTO;
import com.gamerecs.back.dto.IGDBPlatformDTO;
import com.gamerecs.back.dto.IGDBGenreDTO;
import com.gamerecs.back.dto.IGDBCompanyDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IGDBClientServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private IGDBClientService igdbService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        igdbService = new IGDBClientService(restTemplate, "dummyClientId", "dummyAccessToken", objectMapper);
    }

    @Test
    void testConvertCoverUrl() {
        // Test with valid URL
        String originalUrl = "//images.igdb.com/igdb/image/upload/t_thumb/abc123.png";
        String expectedUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/abc123.png";
        assertEquals(expectedUrl, igdbService.convertCoverUrl(originalUrl));

        // Test with null URL
        assertNull(igdbService.convertCoverUrl(null));

        // Test with URL that's already HTTPS
        String httpsUrl = "https://images.igdb.com/igdb/image/upload/t_thumb/abc123.png";
        assertEquals(expectedUrl, igdbService.convertCoverUrl(httpsUrl));
    }

    @Test
    void testSearchGames() {
        // Prepare test data
        String mockResponse = """
            [
                {
                    "id": 1,
                    "name": "Test Game",
                    "cover": {
                        "url": "//images.igdb.com/igdb/image/upload/t_thumb/test.png"
                    },
                    "first_release_date": 1577836800,
                    "summary": "Test summary",
                    "platforms": [{"id": 1, "name": "PC"}],
                    "genres": [{"id": 1, "name": "RPG"}],
                    "involved_companies": [
                        {
                            "company": {"id": 1, "name": "Test Studio"},
                            "developer": true,
                            "publisher": false
                        }
                    ],
                    "updated_at": 1609459200
                }
            ]
            """;

        // Mock RestTemplate response
        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(mockResponseEntity);

        // Execute test
        List<IGDBGameDTO> results = igdbService.searchGames("test query");

        // Verify results
        assertNotNull(results);
        assertEquals(1, results.size());
        IGDBGameDTO game = results.get(0);
        
        // Basic game info
        assertEquals(1L, game.getIgdbId());
        assertEquals("Test Game", game.getTitle());
        assertEquals("https://images.igdb.com/igdb/image/upload/t_cover_big/test.png", game.getCoverImageUrl());
        assertEquals("Test summary", game.getDescription());
        assertEquals(1609459200L, game.getUpdatedAt());
        
        // Platform
        assertNotNull(game.getPlatforms());
        assertEquals(1, game.getPlatforms().size());
        IGDBPlatformDTO platform = game.getPlatforms().get(0);
        assertEquals(1L, platform.getIgdbPlatformId());
        assertEquals("PC", platform.getName());
        
        // Genre
        assertNotNull(game.getGenres());
        assertEquals(1, game.getGenres().size());
        IGDBGenreDTO genre = game.getGenres().get(0);
        assertEquals(1L, genre.getIgdbGenreId());
        assertEquals("RPG", genre.getName());
        
        // Developer/Publisher
        assertNotNull(game.getDevelopers());
        assertEquals(1, game.getDevelopers().size());
        IGDBCompanyDTO developer = game.getDevelopers().get(0);
        assertEquals(1L, developer.getIgdbCompanyId());
        assertEquals("Test Studio", developer.getName());
        
        // Release date
        assertEquals(
            LocalDate.ofInstant(
                Instant.ofEpochSecond(1577836800L),
                java.time.ZoneId.systemDefault()
            ),
            game.getReleaseDate()
        );
    }
} 
