package com.gamerecs.back.service;

import com.gamerecs.back.dto.SteamGameInfo;
import com.gamerecs.back.dto.SteamOwnedGamesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SteamClientService {

    private static final Logger logger = LoggerFactory.getLogger(SteamClientService.class);
    private static final String STEAM_API_BASE_URL = "https://api.steampowered.com";
    private static final String OWNED_GAMES_PATH = "/IPlayerService/GetOwnedGames/v1/";

    private final RestTemplate restTemplate;
    private final String steamApiKey;

    public SteamClientService(RestTemplate restTemplate, @Qualifier("steamApiKey") String steamApiKey) {
        this.restTemplate = restTemplate;
        this.steamApiKey = steamApiKey;
    }

    /**
     * Fetches the App IDs of games owned by a Steam user.
     *
     * @param steamId The 64-bit Steam ID of the user.
     * @return A list of owned game App IDs as Strings, or an empty list if an error occurs or the profile is private.
     */
    public List<String> getOwnedGameAppIds(String steamId) {
        if (!StringUtils.hasText(steamApiKey)) {
            logger.warn("Steam API Key is not configured. Cannot fetch owned games for Steam ID: {}", steamId);
            return Collections.emptyList();
        }

        if (!StringUtils.hasText(steamId)) {
            logger.warn("Steam ID is null or empty. Cannot fetch owned games.");
            return Collections.emptyList();
        }

        String url = UriComponentsBuilder.fromUriString(STEAM_API_BASE_URL + OWNED_GAMES_PATH)
                .queryParam("key", steamApiKey)
                .queryParam("steamid", steamId)
                .queryParam("include_appinfo", "1") // Include game name, playtime, etc.
                .queryParam("format", "json")
                .toUriString();

        logger.debug("Requesting owned games from Steam API for Steam ID: {}", steamId);

        try {
            SteamOwnedGamesResponse response = restTemplate.getForObject(url, SteamOwnedGamesResponse.class);

            if (response == null || response.getResponse() == null || response.getResponse().getGames() == null) {
                // This can happen if the profile is private or the Steam ID is invalid
                logger.warn("Received null or empty response from Steam API for Steam ID: {}. Profile might be private or ID invalid.", steamId);
                return Collections.emptyList();
            }

            List<String> appIds = response.getResponse().getGames().stream()
                    .map(SteamGameInfo::getAppId)
                    .filter(Objects::nonNull) // Ensure app ID is not null
                    .map(String::valueOf)     // Convert Integer to String
                    .collect(Collectors.toList());

            logger.info("Successfully retrieved {} owned game App IDs for Steam ID: {}", appIds.size(), steamId);
            return appIds;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                logger.error("Steam API request failed for Steam ID: {}. Status: {}. Likely an invalid API key or access denied.", steamId, e.getStatusCode());
            } else {
                logger.error("HTTP error fetching owned games for Steam ID: {}. Status: {}, Response: {}", steamId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            }
            return Collections.emptyList();
        } catch (RestClientException e) {
            logger.error("Error fetching owned games from Steam API for Steam ID: {}. Message: {}", steamId, e.getMessage(), e);
            return Collections.emptyList();
        } catch (Exception e) { // Catch broader exceptions for parsing issues, etc.
            logger.error("Unexpected error processing Steam API response for Steam ID: {}", steamId, e);
            return Collections.emptyList();
        }
    }
} 
