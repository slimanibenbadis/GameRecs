package com.gamerecs.back.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SteamGameInfo {

    @JsonProperty("appid")
    private Integer appId;

    @JsonProperty("name")
    private String name; // included because include_appinfo=1

    @JsonProperty("playtime_forever")
    private int playtimeForever;

    // Add other fields if needed, like img_icon_url, etc.
} 
