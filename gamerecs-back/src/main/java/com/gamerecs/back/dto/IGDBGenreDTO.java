package com.gamerecs.back.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class IGDBGenreDTO {
    @JsonProperty("id")
    private Long igdbGenreId;
    private String name;
} 
