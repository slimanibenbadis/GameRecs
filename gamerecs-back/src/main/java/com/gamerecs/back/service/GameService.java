package com.gamerecs.back.service;

import com.gamerecs.back.model.Game;
import com.gamerecs.back.repository.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service for handling game-related operations.
 */
@Service
public class GameService {

    private final GameRepository gameRepository;

    @Autowired
    public GameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    /**
     * Search for games in the local database based on a title query.
     *
     * @param query the search query string
     * @param page the page number (0-indexed)
     * @param size the page size
     * @return a Page of Game objects matching the search criteria
     * @throws ResponseStatusException with HTTP 400 if the query is too short
     */
    @Transactional(readOnly = true)
    public Page<Game> searchGamesByTitle(String query, int page, int size) {
        // Validate input: reject queries with fewer than 2 characters
        if (query == null || query.trim().length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Search query must be at least 2 characters long");
        }
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Game> gamesPage = gameRepository.findByTitleContainingIgnoreCase(query.trim(), pageable);
            
            // Trigger loading of lazy collections for each game to avoid LazyInitializationException
            for (Game game : gamesPage.getContent()) {
                game.getGenres().size();
                game.getPlatforms().size();
                game.getPublishers().size();
                game.getDevelopers().size();
            }
            
            return gamesPage;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error searching games: " + e.getMessage(), e);
        }
    }
} 
