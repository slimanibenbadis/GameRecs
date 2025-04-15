package com.gamerecs.back.dto;

import com.gamerecs.back.model.Game;
import lombok.Data;

import java.util.List;

/**
 * Data Transfer Object for game search responses with pagination.
 */
@Data
public class GameSearchResponse {
    private List<Game> games;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int pageSize;
    private String query;
} 
