package com.gamerecs.back.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class IGDBPlatformDTO {
    @JsonProperty("id")
    private Long igdbPlatformId;
    private String name;
} 
