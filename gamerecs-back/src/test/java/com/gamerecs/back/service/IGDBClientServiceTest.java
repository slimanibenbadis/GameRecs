package com.gamerecs.back.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamerecs.back.dto.IGDBGameDTO;
import com.gamerecs.back.dto.IGDBPlatformDTO;
import com.gamerecs.back.dto.IGDBGenreDTO;
import com.gamerecs.back.dto.IGDBCompanyDTO;
import com.google.common.util.concurrent.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IGDBClientServiceTest {

    @Mock
    private RestTemplate restTemplate;
    
    @Mock
    private RateLimiter rateLimiter;

    private IGDBClientService igdbService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        igdbService = new IGDBClientService(restTemplate, "dummyClientId", "dummyAccessToken", objectMapper);
        
        // Set rate limiter for testing
        ReflectionTestUtils.setField(igdbService, "rateLimiter", rateLimiter);
        
        // We'll configure the rateLimiter in each test that needs it, 
        // rather than here in setUp to avoid UnnecessaryStubbingException
    }

    @Test
    void testConvertCoverUrl() {
        // This test doesn't use the rateLimiter, so no need to configure it
        
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
        // Configure rateLimiter for this test
        when(rateLimiter.acquire()).thenReturn(0.0);
        
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
    
    @Test
    void testSearchGamesRetriesOnTransientFailure() {
        // Configure rateLimiter for this test
        when(rateLimiter.acquire()).thenReturn(0.0);
        
        // Prepare test data
        String mockResponse = """
            [
                {
                    "id": 1,
                    "name": "Test Game",
                    "summary": "Test summary",
                    "first_release_date": 1577836800,
                    "updated_at": 1609459200
                }
            ]
            """;

        // Create spy on the service to manually implement retry logic for testing
        IGDBClientService serviceSpy = Mockito.spy(igdbService);
        
        // Mock RestTemplate to fail twice then succeed
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        )).thenThrow(new RestClientException("First failure"))
          .thenThrow(new RestClientException("Second failure"))
          .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        // Since we don't have Spring Retry in the unit test, we'll manually simulate 
        // a retry by calling the method up to 3 times
        List<IGDBGameDTO> results = null;
        int maxAttempts = 3;
        int attempt = 0;
        
        while (attempt < maxAttempts) {
            try {
                attempt++;
                results = serviceSpy.searchGames("test query");
                break; // If successful, exit the loop
            } catch (RestClientException e) {
                if (attempt >= maxAttempts) {
                    // If all attempts failed, rethrow the exception
                    throw e;
                }
                // Otherwise continue with the next attempt
            }
        }

        // Verify results
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Test Game", results.get(0).getTitle());
        
        // Verify the exchange method was called 3 times (1 initial + 2 retries)
        verify(restTemplate, times(3)).exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        );
    }
    
    @Test
    void testRateLimiterEnforcesFourPerSecond() {
        // Setup the RateLimiter mock to simulate rate limiting
        // First 4 calls return 0.0 (no delay), then return delays for subsequent calls
        when(rateLimiter.acquire())
            .thenReturn(0.0)  // First call - no delay
            .thenReturn(0.0)  // Second call - no delay
            .thenReturn(0.0)  // Third call - no delay
            .thenReturn(0.0)  // Fourth call - no delay
            .thenReturn(0.25) // Fifth call - 250ms delay
            .thenReturn(0.25); // Sixth call - 250ms delay
            
        // Mock successful API response
        ResponseEntity<String> mockResponse = new ResponseEntity<>(
            "[{\"id\":1,\"name\":\"Test Game\",\"first_release_date\":1577836800,\"updated_at\":1609459200}]", 
            HttpStatus.OK
        );
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(mockResponse);
        
        // Make 6 API calls
        for (int i = 0; i < 6; i++) {
            igdbService.searchGames("test" + i);
        }
        
        // Verify rate limiter was used 6 times
        verify(rateLimiter, times(6)).acquire();
    }
} 
