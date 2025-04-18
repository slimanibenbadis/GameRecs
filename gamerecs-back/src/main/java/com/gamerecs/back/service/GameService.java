package com.gamerecs.back.service;

import com.gamerecs.back.model.Game;
import com.gamerecs.back.repository.GameRepository;
import com.gamerecs.back.util.StringNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(GameService.class);
    private final GameRepository gameRepository;

    @Autowired
    public GameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    /**
     * Search for games in the local database based on a title query with improved handling 
     * of accents and punctuation.
     * This method normalizes both the query and the game titles by:
     * - Converting to lowercase
     * - Removing diacritics (accents)
     * - Removing punctuation
     *
     * @param query the search query string
     * @param page the page number (0-indexed)
     * @param size the page size
     * @return a Page of Game objects matching the normalized search criteria
     * @throws ResponseStatusException with HTTP 400 if the query is too short
     */
    @Transactional(readOnly = true)
    public Page<Game> searchGamesByTitleNormalized(String query, int page, int size) {
        // Validate input: reject queries with fewer than 2 characters
        if (query == null || query.trim().length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Search query must be at least 2 characters long");
        }
        
        try {
            // Normalize the query to handle accents and punctuation
            String normalizedQuery = StringNormalizer.normalize(query.trim());
            
            // Ensure the normalized query has at least 2 characters
            if (normalizedQuery.length() < 2) {
                logger.warn("Normalized query '{}' is too short after normalization", query);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Search query must be at least 2 characters long after normalization");
            }
            
            logger.debug("Normalized query '{}' to '{}'", query, normalizedQuery);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<Game> gamesPage = gameRepository.findByNormalizedTitleContaining(normalizedQuery, pageable);
            
            // Trigger loading of lazy collections for each game to avoid LazyInitializationException
            for (Game game : gamesPage.getContent()) {
                game.getGenres().size();
                game.getPlatforms().size();
                game.getPublishers().size();
                game.getDevelopers().size();
            }
            
            return gamesPage;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error searching games with normalized title: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error searching games: " + e.getMessage(), e);
        }
    }
} 
