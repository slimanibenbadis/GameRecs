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

    public SteamClientService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Fetches the App IDs of games owned by a Steam user.
     *
     * @param userSteamProfileId The 64-bit Steam ID of the user.
     * @param decryptedUserApiKey The user's decrypted Steam API key.
     * @return A list of owned game App IDs as Strings, or an empty list if an error occurs or the profile is private.
     */
    public List<String> getOwnedGameAppIds(String userSteamProfileId, String decryptedUserApiKey) {
        if (!StringUtils.hasText(decryptedUserApiKey)) {
            logger.warn("User's Steam API Key is not available. Cannot fetch owned games for Steam Profile ID: {}", userSteamProfileId);
            return Collections.emptyList();
        }

        if (!StringUtils.hasText(userSteamProfileId)) {
            logger.warn("User's Steam Profile ID is null or empty. Cannot fetch owned games.");
            return Collections.emptyList();
        }

        String url = UriComponentsBuilder.fromUriString(STEAM_API_BASE_URL + OWNED_GAMES_PATH)
                .queryParam("key", decryptedUserApiKey)
                .queryParam("steamid", userSteamProfileId)
                .queryParam("include_appinfo", "1") // Include game name, playtime, etc.
                .queryParam("format", "json")
                .toUriString();

        logger.debug("Requesting owned games from Steam API for Steam Profile ID: {}", userSteamProfileId);

        try {
            SteamOwnedGamesResponse response = restTemplate.getForObject(url, SteamOwnedGamesResponse.class);

            if (response == null || response.getResponse() == null || response.getResponse().getGames() == null) {
                // This can happen if the profile is private or the Steam ID is invalid
                logger.warn("Received null or empty response from Steam API for Steam Profile ID: {}. Profile might be private or ID invalid.", userSteamProfileId);
                return Collections.emptyList();
            }

            List<String> appIds = response.getResponse().getGames().stream()
                    .map(SteamGameInfo::getAppId)
                    .filter(Objects::nonNull) // Ensure app ID is not null
                    .map(String::valueOf)     // Convert Integer to String
                    .collect(Collectors.toList());

            logger.info("Successfully retrieved {} owned game App IDs for Steam Profile ID: {}", appIds.size(), userSteamProfileId);
            return appIds;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                logger.error("Steam API request failed for Steam Profile ID: {}. Status: {}. Likely an invalid API key or access denied.", userSteamProfileId, e.getStatusCode());
            } else {
                logger.error("HTTP error fetching owned games for Steam Profile ID: {}. Status: {}, Response: {}", userSteamProfileId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            }
            return Collections.emptyList();
        } catch (RestClientException e) {
            logger.error("Error fetching owned games from Steam API for Steam Profile ID: {}. Message: {}", userSteamProfileId, e.getMessage(), e);
            return Collections.emptyList();
        } catch (Exception e) { // Catch broader exceptions for parsing issues, etc.
            logger.error("Unexpected error processing Steam API response for Steam Profile ID: {}", userSteamProfileId, e);
            return Collections.emptyList();
        }
    }
} 
