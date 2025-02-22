package com.gamerecs.back.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class IGDBCompanyDTO {
    @JsonProperty("id")
    private Long igdbCompanyId;
    private String name;
} 
