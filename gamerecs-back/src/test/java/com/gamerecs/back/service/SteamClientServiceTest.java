package com.gamerecs.back.service;

import com.gamerecs.back.dto.SteamGameInfo;
import com.gamerecs.back.dto.SteamOwnedGamesResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;


@ExtendWith(MockitoExtension.class)
class SteamClientServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private SteamClientService steamClientService;

    private final String testSteamProfileId = "testProfileId";
    private final String testApiKey = "testApiKey";
    private final String expectedUrl = "https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/?key=" + testApiKey + "&steamid=" + testSteamProfileId + "&include_appinfo=1&format=json";

    @BeforeEach
    void setUp() {
        // Initialization if needed, though @Mock and @InjectMocks handle much of it.
    }

    @Test
    void getOwnedGameAppIds_success() {
        SteamOwnedGamesResponse.SteamResponseData mockSteamResponseData = new SteamOwnedGamesResponse.SteamResponseData();
        SteamGameInfo game1 = new SteamGameInfo();
        game1.setAppId(100);
        SteamGameInfo game2 = new SteamGameInfo();
        game2.setAppId(200);
        mockSteamResponseData.setGames(Arrays.asList(game1, game2));
        mockSteamResponseData.setGameCount(2);

        SteamOwnedGamesResponse mockResponse = new SteamOwnedGamesResponse();
        mockResponse.setResponse(mockSteamResponseData);

        when(restTemplate.getForObject(eq(expectedUrl), eq(SteamOwnedGamesResponse.class))).thenReturn(mockResponse);

        List<String> appIds = steamClientService.getOwnedGameAppIds(testSteamProfileId, testApiKey);

        assertNotNull(appIds);
        assertEquals(2, appIds.size());
        assertTrue(appIds.contains("100"));
        assertTrue(appIds.contains("200"));
        verify(restTemplate, times(1)).getForObject(eq(expectedUrl), eq(SteamOwnedGamesResponse.class));
    }

    @Test
    void getOwnedGameAppIds_nullApiKey_returnsEmptyList() {
        List<String> appIds = steamClientService.getOwnedGameAppIds(testSteamProfileId, null);
        assertTrue(appIds.isEmpty());
        verify(restTemplate, never()).getForObject(anyString(), eq(SteamOwnedGamesResponse.class));
    }

    @Test
    void getOwnedGameAppIds_emptyApiKey_returnsEmptyList() {
        List<String> appIds = steamClientService.getOwnedGameAppIds(testSteamProfileId, "");
        assertTrue(appIds.isEmpty());
        verify(restTemplate, never()).getForObject(anyString(), eq(SteamOwnedGamesResponse.class));
    }
    
    @Test
    void getOwnedGameAppIds_blankApiKey_returnsEmptyList() {
        List<String> appIds = steamClientService.getOwnedGameAppIds(testSteamProfileId, "   ");
        assertTrue(appIds.isEmpty());
        verify(restTemplate, never()).getForObject(anyString(), eq(SteamOwnedGamesResponse.class));
    }

    @Test
    void getOwnedGameAppIds_nullProfileId_returnsEmptyList() {
        List<String> appIds = steamClientService.getOwnedGameAppIds(null, testApiKey);
        assertTrue(appIds.isEmpty());
        verify(restTemplate, never()).getForObject(anyString(), eq(SteamOwnedGamesResponse.class));
    }

    @Test
    void getOwnedGameAppIds_emptyProfileId_returnsEmptyList() {
        List<String> appIds = steamClientService.getOwnedGameAppIds("", testApiKey);
        assertTrue(appIds.isEmpty());
        verify(restTemplate, never()).getForObject(anyString(), eq(SteamOwnedGamesResponse.class));
    }

    @Test
    void getOwnedGameAppIds_blankProfileId_returnsEmptyList() {
        List<String> appIds = steamClientService.getOwnedGameAppIds("   ", testApiKey);
        assertTrue(appIds.isEmpty());
        verify(restTemplate, never()).getForObject(anyString(), eq(SteamOwnedGamesResponse.class));
    }

    @Test
    void getOwnedGameAppIds_steamApiReturnsNullResponse_returnsEmptyList() {
        when(restTemplate.getForObject(eq(expectedUrl), eq(SteamOwnedGamesResponse.class))).thenReturn(null);
        List<String> appIds = steamClientService.getOwnedGameAppIds(testSteamProfileId, testApiKey);
        assertTrue(appIds.isEmpty());
        verify(restTemplate, times(1)).getForObject(eq(expectedUrl), eq(SteamOwnedGamesResponse.class));
    }

    @Test
    void getOwnedGameAppIds_steamApiReturnsResponseWithNullInternalResponse_returnsEmptyList() {
        SteamOwnedGamesResponse mockResponse = new SteamOwnedGamesResponse();
        mockResponse.setResponse(null); // Internal response is null

        when(restTemplate.getForObject(eq(expectedUrl), eq(SteamOwnedGamesResponse.class))).thenReturn(mockResponse);
        List<String> appIds = steamClientService.getOwnedGameAppIds(testSteamProfileId, testApiKey);
        assertTrue(appIds.isEmpty());
        verify(restTemplate, times(1)).getForObject(eq(expectedUrl), eq(SteamOwnedGamesResponse.class));
    }

    @Test
    void getOwnedGameAppIds_steamApiReturnsResponseWithNullGamesList_returnsEmptyList() {
        SteamOwnedGamesResponse.SteamResponseData mockSteamResponseData = new SteamOwnedGamesResponse.SteamResponseData();
        mockSteamResponseData.setGames(null); // Games list is null
        SteamOwnedGamesResponse mockResponse = new SteamOwnedGamesResponse();
        mockResponse.setResponse(mockSteamResponseData);

        when(restTemplate.getForObject(eq(expectedUrl), eq(SteamOwnedGamesResponse.class))).thenReturn(mockResponse);
        List<String> appIds = steamClientService.getOwnedGameAppIds(testSteamProfileId, testApiKey);
        assertTrue(appIds.isEmpty());
        verify(restTemplate, times(1)).getForObject(eq(expectedUrl), eq(SteamOwnedGamesResponse.class));
    }

    @Test
    void getOwnedGameAppIds_steamApiReturnsResponseWithEmptyGamesList_returnsEmptyList() {
        SteamOwnedGamesResponse.SteamResponseData mockSteamResponseData = new SteamOwnedGamesResponse.SteamResponseData();
        mockSteamResponseData.setGames(Collections.emptyList()); // Empty games list
        mockSteamResponseData.setGameCount(0);
        SteamOwnedGamesResponse mockResponse = new SteamOwnedGamesResponse();
        mockResponse.setResponse(mockSteamResponseData);

        when(restTemplate.getForObject(eq(expectedUrl), eq(SteamOwnedGamesResponse.class))).thenReturn(mockResponse);
        List<String> appIds = steamClientService.getOwnedGameAppIds(testSteamProfileId, testApiKey);
        assertTrue(appIds.isEmpty());
        verify(restTemplate, times(1)).getForObject(eq(expectedUrl), eq(SteamOwnedGamesResponse.class));
    }
    
    @Test
    void getOwnedGameAppIds_steamApiReturnsGamesWithNullAppId_filtersOutNulls() {
        SteamOwnedGamesResponse.SteamResponseData mockSteamResponseData = new SteamOwnedGamesResponse.SteamResponseData();
        SteamGameInfo game1 = new SteamGameInfo();
        game1.setAppId(100);
        SteamGameInfo game2WithNullAppId = new SteamGameInfo(); // AppId is null by default
        SteamGameInfo game3 = new SteamGameInfo();
        game3.setAppId(300);
        mockSteamResponseData.setGames(Arrays.asList(game1, game2WithNullAppId, game3));
        mockSteamResponseData.setGameCount(3);

        SteamOwnedGamesResponse mockResponse = new SteamOwnedGamesResponse();
        mockResponse.setResponse(mockSteamResponseData);

        when(restTemplate.getForObject(eq(expectedUrl), eq(SteamOwnedGamesResponse.class))).thenReturn(mockResponse);

        List<String> appIds = steamClientService.getOwnedGameAppIds(testSteamProfileId, testApiKey);

        assertNotNull(appIds);
        assertEquals(2, appIds.size());
        assertTrue(appIds.contains("100"));
        assertTrue(appIds.contains("300"));
        assertFalse(appIds.contains(null)); // Ensure null was filtered
        verify(restTemplate, times(1)).getForObject(eq(expectedUrl), eq(SteamOwnedGamesResponse.class));
    }

    @Test
    void getOwnedGameAppIds_httpClientErrorException_Unauthorized_returnsEmptyList() {
        when(restTemplate.getForObject(eq(expectedUrl), eq(SteamOwnedGamesResponse.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        
        List<String> appIds = steamClientService.getOwnedGameAppIds(testSteamProfileId, testApiKey);
        assertTrue(appIds.isEmpty());
        verify(restTemplate, times(1)).getForObject(eq(expectedUrl), eq(SteamOwnedGamesResponse.class));
    }

    @Test
    void getOwnedGameAppIds_httpClientErrorException_Forbidden_returnsEmptyList() {
        when(restTemplate.getForObject(eq(expectedUrl), eq(SteamOwnedGamesResponse.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN, "Forbidden"));

        List<String> appIds = steamClientService.getOwnedGameAppIds(testSteamProfileId, testApiKey);
        assertTrue(appIds.isEmpty());
        verify(restTemplate, times(1)).getForObject(eq(expectedUrl), eq(SteamOwnedGamesResponse.class));
    }
    
    @Test
    void getOwnedGameAppIds_httpClientErrorException_OtherHttpError_returnsEmptyList() {
        when(restTemplate.getForObject(eq(expectedUrl), eq(SteamOwnedGamesResponse.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error"));

        List<String> appIds = steamClientService.getOwnedGameAppIds(testSteamProfileId, testApiKey);
        assertTrue(appIds.isEmpty());
        verify(restTemplate, times(1)).getForObject(eq(expectedUrl), eq(SteamOwnedGamesResponse.class));
    }

    @Test
    void getOwnedGameAppIds_restClientException_returnsEmptyList() {
        when(restTemplate.getForObject(eq(expectedUrl), eq(SteamOwnedGamesResponse.class)))
            .thenThrow(new RestClientException("Network error"));

        List<String> appIds = steamClientService.getOwnedGameAppIds(testSteamProfileId, testApiKey);
        assertTrue(appIds.isEmpty());
        verify(restTemplate, times(1)).getForObject(eq(expectedUrl), eq(SteamOwnedGamesResponse.class));
    }

    @Test
    void getOwnedGameAppIds_unexpectedException_returnsEmptyList() {
        // Simulate an unexpected exception during the stream processing or mapping
        // For example, if String.valueOf threw an exception for some reason (highly unlikely for Integer)
        // or if the response structure was valid up to games list but caused issues in stream.
        SteamOwnedGamesResponse.SteamResponseData mockSteamResponseData = new SteamOwnedGamesResponse.SteamResponseData();
        SteamGameInfo game1 = new SteamGameInfo() { // Anonymous inner class to override
            @Override
            public Integer getAppId() {
                throw new NumberFormatException("Unexpected format issue");
            }
        };
        mockSteamResponseData.setGames(Collections.singletonList(game1));
        mockSteamResponseData.setGameCount(1);
        SteamOwnedGamesResponse mockResponse = new SteamOwnedGamesResponse();
        mockResponse.setResponse(mockSteamResponseData);

        when(restTemplate.getForObject(eq(expectedUrl), eq(SteamOwnedGamesResponse.class))).thenReturn(mockResponse);

        List<String> appIds = steamClientService.getOwnedGameAppIds(testSteamProfileId, testApiKey);
        assertTrue(appIds.isEmpty());
        verify(restTemplate, times(1)).getForObject(eq(expectedUrl), eq(SteamOwnedGamesResponse.class));
    }
} 
