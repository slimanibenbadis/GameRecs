package com.gamerecs.back.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamerecs.back.config.CacheConfig;
import com.gamerecs.back.dto.IGDBGameDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration test for the IGDB caching functionality
 * Tests that the cache is working as expected and that
 * API calls are only made when necessary
 */
@SpringBootTest
@ActiveProfiles("test")
public class IGDBCacheTest {

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private IGDBClientService igdbClientService;
    
    @Autowired
    private ObjectMapper objectMapper;

    private static final String TEST_QUERY = "zelda";
    private static final String MOCK_RESPONSE = """
        [
            {
                "id": 1,
                "name": "The Legend of Zelda",
                "cover": {
                    "url": "//images.igdb.com/igdb/image/upload/t_thumb/zelda.png"
                },
                "first_release_date": 1577836800,
                "summary": "The classic adventure game",
                "platforms": [{"id": 1, "name": "NES"}],
                "genres": [{"id": 1, "name": "Adventure"}],
                "involved_companies": [
                    {
                        "company": {"id": 1, "name": "Nintendo"},
                        "developer": true,
                        "publisher": true
                    }
                ],
                "updated_at": 1609459200
            }
        ]
        """;

    @BeforeEach
    void setUp() {
        // Clear the cache before each test
        cacheManager.getCache(CacheConfig.IGDB_GAME_SEARCH_CACHE).clear();
        
        // Mock RestTemplate response
        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>(MOCK_RESPONSE, HttpStatus.OK);
        when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        )).thenReturn(mockResponseEntity);
    }

    @Test
    void testCacheHitAndMiss() {
        // First call should result in a cache miss and make an API call
        List<IGDBGameDTO> firstResult = igdbClientService.searchGames(TEST_QUERY);
        
        // Verify the RestTemplate was called once
        verify(restTemplate, times(1)).exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        );
        
        // Second call with the same query should hit the cache and not make an API call
        List<IGDBGameDTO> secondResult = igdbClientService.searchGames(TEST_QUERY);
        
        // Verify the RestTemplate was not called again
        verify(restTemplate, times(1)).exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        );
        
        // Results should be the same
        assertThat(secondResult).isEqualTo(firstResult);
    }
    
    @Test
    void testDifferentQueriesDoNotHitCache() {
        // First call with one query
        igdbClientService.searchGames("zelda");
        
        // Second call with a different query
        igdbClientService.searchGames("mario");
        
        // Verify the RestTemplate was called twice
        verify(restTemplate, times(2)).exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        );
    }
    
    @Test
    void testCacheEviction() {
        // First call should result in a cache miss
        igdbClientService.searchGames(TEST_QUERY);
        
        // Clear the cache
        igdbClientService.clearGameSearchCache();
        
        // Second call should also result in a cache miss
        igdbClientService.searchGames(TEST_QUERY);
        
        // Verify the RestTemplate was called twice
        verify(restTemplate, times(2)).exchange(
            anyString(),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)
        );
    }
    
    @Test
    void testCacheContents() {
        // Make a call that should be cached
        List<IGDBGameDTO> result = igdbClientService.searchGames(TEST_QUERY);
        
        // Check that the cache contains the expected entry
        Object cachedValue = cacheManager.getCache(CacheConfig.IGDB_GAME_SEARCH_CACHE).get(TEST_QUERY).get();
        
        // The cached value should be a List of IGDBGameDTO objects
        assertThat(cachedValue).isInstanceOf(List.class);
        
        // The cached value should equal our original result
        assertThat(cachedValue).isEqualTo(result);
        
        // The cached data should contain our test game
        List<?> cachedList = (List<?>) cachedValue;
        assertThat(cachedList).hasSize(1);
        assertThat(cachedList.get(0)).isInstanceOf(IGDBGameDTO.class);
        IGDBGameDTO cachedGame = (IGDBGameDTO) cachedList.get(0);
        assertThat(cachedGame.getTitle()).isEqualTo("The Legend of Zelda");
    }
} 
