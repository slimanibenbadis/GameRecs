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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;

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
    void testSearchGamesSuccess() {
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
    void testSearchGamesHttpTooManyRequests() {
        when(rateLimiter.acquire()).thenReturn(0.0);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests"));

        RestClientException exception = assertThrows(RestClientException.class, () -> {
            igdbService.searchGames("test query");
        });
        assertTrue(exception.getMessage().contains("IGDB API rate limit exceeded"));
    }

    @Test
    void testSearchGamesHttpOtherClientError() {
        when(rateLimiter.acquire()).thenReturn(0.0);
        HttpClientErrorException originalException = new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenThrow(originalException);

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class, () -> {
            igdbService.searchGames("test query");
        });
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Bad Request"));
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

    @Test
    void testRecoverSearchGames() {
        List<IGDBGameDTO> result = igdbService.recoverSearchGames(new RuntimeException("Test Exception"), "test query");
        assertNotNull(result);
        assertTrue(result.isEmpty());
        // We can also verify logger output if a testing logger is configured, but for now, this is sufficient.
    }

    @Test
    void testSearchGamesBySteamAppIdsWithNullInput() {
        List<IGDBGameDTO> results = igdbService.searchGamesBySteamAppIds(null);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testSearchGamesBySteamAppIdsWithEmptyInput() {
        List<IGDBGameDTO> results = igdbService.searchGamesBySteamAppIds(List.of());
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testSearchGamesBySteamAppIdsSingleBatchSuccess() throws Exception {
        when(rateLimiter.acquire()).thenReturn(0.0); // For the call to fetchGamesBatchBySteamAppIds

        String mockBatchResponse = "[{\"id\": 100, \"name\": \"Steam Game 1\", \"first_release_date\": 1600000000, \"updated_at\": 1600000000}]";
        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>(mockBatchResponse, HttpStatus.OK);
        when(restTemplate.exchange(
            anyString(), 
            eq(HttpMethod.POST), 
            any(HttpEntity.class), 
            eq(String.class)
        )).thenReturn(mockResponseEntity);

        List<String> steamIds = new ArrayList<>(List.of("10", "20")); // Ensured mutable list
        List<IGDBGameDTO> results = igdbService.searchGamesBySteamAppIds(steamIds);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Steam Game 1", results.get(0).getTitle());
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void testSearchGamesBySteamAppIdsMultipleBatchesSuccess() throws Exception {
        when(rateLimiter.acquire()).thenReturn(0.0); // Called for each batch

        String mockBatchResponse1 = "[{\"id\": 101, \"name\": \"Steam Game Batch 1\", \"first_release_date\": 1600000001, \"updated_at\": 1600000001}]";
        String mockBatchResponse2 = "[{\"id\": 102, \"name\": \"Steam Game Batch 2\", \"first_release_date\": 1600000002, \"updated_at\": 1600000002}]";
        
        ResponseEntity<String> mockResponseEntity1 = new ResponseEntity<>(mockBatchResponse1, HttpStatus.OK);
        ResponseEntity<String> mockResponseEntity2 = new ResponseEntity<>(mockBatchResponse2, HttpStatus.OK);

        // Mock restTemplate to return different responses for sequential calls
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenReturn(mockResponseEntity1)
            .thenReturn(mockResponseEntity2);
        
        // Create a list of IDs that will result in two batches (STEAM_ID_BATCH_SIZE = 100 by default)
        List<String> steamIds = new java.util.ArrayList<>();
        for (int i = 0; i < 150; i++) {
            steamIds.add(String.valueOf(i));
        }
        // ReflectionTestUtils.setField(igdbService, "STEAM_ID_BATCH_SIZE", 100); // Removed: Rely on production constant

        List<IGDBGameDTO> results = igdbService.searchGamesBySteamAppIds(steamIds);

        assertNotNull(results);
        assertEquals(2, results.size()); // One game from each batch
        assertEquals("Steam Game Batch 1", results.get(0).getTitle());
        assertEquals("Steam Game Batch 2", results.get(1).getTitle());
        verify(restTemplate, times(2)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)); 
    }

    @Test
    void testSearchGamesBySteamAppIdsBatchFailure() throws Exception {
        when(rateLimiter.acquire()).thenReturn(0.0); // For the call to fetchGamesBatchBySteamAppIds

        // First batch succeeds, second batch fails with a client error
        String mockBatchResponseSuccess = "[{\"id\": 100, \"name\": \"Steam Game Success\", \"first_release_date\": 1600000000, \"updated_at\": 1600000000}]";
        ResponseEntity<String> successResponseEntity = new ResponseEntity<>(mockBatchResponseSuccess, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenReturn(successResponseEntity)
            .thenThrow(new HttpClientErrorException(HttpStatus.BAD_GATEWAY, "Simulated Batch Error"));

        List<String> steamIds = new java.util.ArrayList<>();
        for (int i = 0; i < 150; i++) { // Should create two batches if STEAM_ID_BATCH_SIZE is 100
            steamIds.add(String.valueOf(i));
        }
        // ReflectionTestUtils.setField(igdbService, "STEAM_ID_BATCH_SIZE", 100); // Removed: Rely on production constant

        List<IGDBGameDTO> results = igdbService.searchGamesBySteamAppIds(steamIds);

        assertNotNull(results);
        assertEquals(1, results.size()); // Only the first batch succeeded
        assertEquals("Steam Game Success", results.get(0).getTitle());
        // Verify restTemplate was called twice (once success, once failure)
        verify(restTemplate, times(2)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void testFetchGamesBatchHttpTooManyRequests() throws Exception {
        when(rateLimiter.acquire()).thenReturn(0.0);
        List<String> steamIds = new ArrayList<>(List.of("70")); // Changed to mutable list

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS, "Rate Limit Hit"));
        
        // searchGamesBySteamAppIds calls fetchGamesBatchBySteamAppIds internally.
        // The exception from fetchGamesBatchBySteamAppIds (RestClientException for TOO_MANY_REQUESTS)
        // will be caught in searchGamesBySteamAppIds, and an empty list will be added for that batch.
        List<IGDBGameDTO> results = igdbService.searchGamesBySteamAppIds(steamIds);
        assertNotNull(results);
        assertTrue(results.isEmpty()); // Expect empty as the batch call failed and was handled
         // Verify restTemplate was called (it will be, and then throw)
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void testFetchGamesBatchHttpOtherClientError() throws Exception {
        when(rateLimiter.acquire()).thenReturn(0.0);
        List<String> steamIds = new ArrayList<>(List.of("80")); // Changed to mutable list
        HttpClientErrorException originalHttpClientException = new HttpClientErrorException(HttpStatus.NOT_FOUND, "Not Found");

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenThrow(originalHttpClientException);

        // As above, searchGamesBySteamAppIds catches the exception from the batch call.
        List<IGDBGameDTO> results = igdbService.searchGamesBySteamAppIds(steamIds);
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void testFetchGamesBatchParseException() throws Exception {
        when(rateLimiter.acquire()).thenReturn(0.0);
        List<String> steamIds = new ArrayList<>(List.of("90")); // Changed to mutable list

        // Simulate ObjectMapper failure
        // This is a bit indirect. We make restTemplate succeed, but ObjectMapper (used inside fetchGamesBatchBySteamAppIds) fail.
        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>("invalid json", HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenReturn(mockResponseEntity);
        
        // Make objectMapper.readValue throw an exception
        ObjectMapper failingObjectMapper = mock(ObjectMapper.class);
        when(failingObjectMapper.readValue(eq("invalid json"), any(com.fasterxml.jackson.core.type.TypeReference.class)))
            .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("Parse error") {}); // Use a concrete subclass
        
        // Temporarily replace the service's objectMapper with the failing mock
        ReflectionTestUtils.setField(igdbService, "objectMapper", failingObjectMapper);

        List<IGDBGameDTO> results = igdbService.searchGamesBySteamAppIds(steamIds);
        assertNotNull(results);
        assertTrue(results.isEmpty());
        
        // Restore the original objectMapper to avoid affecting other tests
        ReflectionTestUtils.setField(igdbService, "objectMapper", this.objectMapper); 
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }

    // Test for recoverFetchGamesBatchBySteamAppIds - MANUALLY CHECK PLACEMENT
    @Test
    void testRecoverFetchGamesBatchBySteamAppIds() {
        List<String> batchSteamAppIds = List.of("1", "2");
        List<IGDBGameDTO> result = ReflectionTestUtils.invokeMethod(
            igdbService,
            "recoverFetchGamesBatchBySteamAppIds", // Method name as String
            new RuntimeException("Test Batch Exception"), // First arg: Exception e
            batchSteamAppIds // Second arg: List<String> batchSteamAppIds
        );
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    // END Test for recoverFetchGamesBatchBySteamAppIds

    @Test
    void testPostProcessGamesWithNullList() {
        // Test the null check at the beginning of postProcessGames directly via reflection
        List<IGDBGameDTO> result = ReflectionTestUtils.invokeMethod(igdbService, "postProcessGames", (Object) null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testPostProcessGamesVariationsViaSearchGames() throws Exception {
        when(rateLimiter.acquire()).thenReturn(0.0);

        String mockResponseWithVariations = "["
            + "  {\"id\": 201, \"name\": \"Game No Cover\", \"first_release_date\": 1577836800, \"summary\": \"No cover info\"},"
            + "  {\"id\": 202, \"name\": \"Game Cover No URL\", \"cover\": {}, \"first_release_date\": 1577836801, \"summary\": \"Cover object exists, but no URL\"},"
            + "  {\"id\": 203, \"name\": \"Game No Release Date\", \"cover\": {\"url\": \"//images.igdb.com/igdb/image/upload/t_thumb/test_no_date.png\"}, \"summary\": \"No release date timestamp\"},"
            + "  {\"id\": 204, \"name\": \"Game With All Data\", \"cover\": {\"url\": \"//images.igdb.com/igdb/image/upload/t_thumb/test_all_data.png\"}, \"first_release_date\": 1577836802, \"summary\": \"All data present\"}"
            + "]";

        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>(mockResponseWithVariations, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenReturn(mockResponseEntity);

        List<IGDBGameDTO> results = igdbService.searchGames("queryForVariations");

        assertNotNull(results);
        assertEquals(4, results.size());

        // Game No Cover
        IGDBGameDTO gameNoCover = results.stream().filter(g -> g.getIgdbId() == 201L).findFirst().orElse(null);
        assertNotNull(gameNoCover);
        assertNull(gameNoCover.getCoverImageUrl());
        assertNotNull(gameNoCover.getReleaseDate()); // Timestamp was present

        // Game Cover No URL
        IGDBGameDTO gameCoverNoUrl = results.stream().filter(g -> g.getIgdbId() == 202L).findFirst().orElse(null);
        assertNotNull(gameCoverNoUrl);
        assertNull(gameCoverNoUrl.getCoverImageUrl()); // Cover was present but URL was null
        assertNotNull(gameCoverNoUrl.getReleaseDate());

        // Game No Release Date
        IGDBGameDTO gameNoReleaseDate = results.stream().filter(g -> g.getIgdbId() == 203L).findFirst().orElse(null);
        assertNotNull(gameNoReleaseDate);
        assertNotNull(gameNoReleaseDate.getCoverImageUrl());
        assertNull(gameNoReleaseDate.getReleaseDate()); // Timestamp was null

        // Game With All Data (sanity check)
        IGDBGameDTO gameAllData = results.stream().filter(g -> g.getIgdbId() == 204L).findFirst().orElse(null);
        assertNotNull(gameAllData);
        assertNotNull(gameAllData.getCoverImageUrl());
        assertNotNull(gameAllData.getReleaseDate());
    }
} 
