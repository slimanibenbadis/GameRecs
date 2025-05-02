package com.gamerecs.back.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // Ignore the top-level 'response' object if present directly
public class SteamOwnedGamesResponse {

    @JsonProperty("response")
    private SteamResponseData response;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SteamResponseData {

        @JsonProperty("game_count")
        private int gameCount;

        @JsonProperty("games")
        private List<SteamGameInfo> games;
    }
} 
