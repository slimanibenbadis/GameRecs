package com.gamerecs.back.service;

import com.gamerecs.back.model.Game;
import com.gamerecs.back.model.GameLibrary;
import com.gamerecs.back.model.PaginatedGameLibraryResponse;
import com.gamerecs.back.model.User;
import com.gamerecs.back.repository.GameLibraryRepository;
import com.gamerecs.back.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class GameLibraryService {

    private final GameLibraryRepository gameLibraryRepository;
    private final UserRepository userRepository;

    @Autowired
    public GameLibraryService(GameLibraryRepository gameLibraryRepository, UserRepository userRepository) {
        this.gameLibraryRepository = gameLibraryRepository;
        this.userRepository = userRepository;
    }

    /**
     * Retrieve the game library for a given user ID.
     *
     * @param userId the authenticated user's ID
     * @return the GameLibrary object associated with the user
     * @throws ResponseStatusException with HTTP 404 if library not found and 401 if the user is missing.
     */
    @Transactional(readOnly = true)
    public GameLibrary getLibraryForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        
        return gameLibraryRepository.findByUserWithGamesAndCollections(user)
                .orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Game library not found"));
    }
    
    /**
     * Retrieve the game library for a given user ID with sorting and filtering options.
     *
     * @param userId the authenticated user's ID
     * @param sortBy the field to sort games by (e.g., "title", "releaseDate")
     * @param filterByGenre the genre name to filter games by (empty string means no filtering)
     * @return the GameLibrary object associated with the user, with games sorted and filtered
     * @throws ResponseStatusException with HTTP 404 if library not found and 401 if the user is missing.
     */
    @Transactional(readOnly = true)
    public GameLibrary getLibraryForUser(Long userId, String sortBy, String filterByGenre) {
        // First get the basic library
        GameLibrary library = getLibraryForUser(userId);
        
        // Apply filtering if a genre is specified
        Set<Game> filteredGames = library.getGames();
        if (filterByGenre != null && !filterByGenre.isEmpty()) {
            filteredGames = library.getGames().stream()
                .filter(game -> game.getGenres().stream()
                    .anyMatch(genre -> genre.getName().equalsIgnoreCase(filterByGenre)))
                .collect(java.util.stream.Collectors.toSet());
        }
        
        // Apply sorting based on the sortBy parameter
        List<Game> sortedGames;
        switch (sortBy.toLowerCase()) {
            case "releasedate":
                sortedGames = filteredGames.stream()
                    .sorted(Comparator.comparing(Game::getReleaseDate, 
                        Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(java.util.stream.Collectors.toList());
                break;
            case "title":
            default:
                sortedGames = filteredGames.stream()
                    .sorted(Comparator.comparing(
                        game -> game.getTitle().toLowerCase()))
                    .collect(java.util.stream.Collectors.toList());
                break;
        }
        
        // Create a new GameLibrary with the sorted and filtered games
        GameLibrary sortedAndFilteredLibrary = new GameLibrary();
        sortedAndFilteredLibrary.setLibraryId(library.getLibraryId());
        sortedAndFilteredLibrary.setUser(library.getUser());
        sortedAndFilteredLibrary.setGames(new LinkedHashSet<>(sortedGames));
        
        return sortedAndFilteredLibrary;
    }

    /**
     * Retrieve the game library for a given user ID with pagination, sorting and filtering options.
     *
     * @param userId the authenticated user's ID
     * @param sortBy the field to sort games by (e.g., "title", "releaseDate")
     * @param filterByGenre the genre name to filter games by (empty string means no filtering)
     * @param page the page number (0-indexed)
     * @param size the page size
     * @return a PaginatedGameLibraryResponse containing the games and pagination metadata
     * @throws ResponseStatusException with HTTP 404 if library not found and 401 if the user is missing.
     */
    @Transactional(readOnly = true)
    public PaginatedGameLibraryResponse getPaginatedLibraryForUser(Long userId,
            String sortBy, String filterByGenre, int page, int size) {
        
        // Get the user
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        
        // Create pageable without sort as we're using explicit sort in the query methods
        Pageable pageable = PageRequest.of(page, size);
        
        // Get games page based on sort and filter criteria
        Page<Game> gamesPage;
        try {
            // Determine which repository method to call based on sort and filter criteria
            boolean isReleaseDate = "releasedate".equalsIgnoreCase(sortBy);
            
            if (filterByGenre != null && !filterByGenre.trim().isEmpty()) {
                if (isReleaseDate) {
                    gamesPage = gameLibraryRepository.findGamesByUserAndGenreOrderByReleaseDate(user, filterByGenre, pageable);
                } else {
                    gamesPage = gameLibraryRepository.findGamesByUserAndGenreOrderByTitle(user, filterByGenre, pageable);
                }
            } else {
                if (isReleaseDate) {
                    gamesPage = gameLibraryRepository.findGamesByUserOrderByReleaseDate(user, pageable);
                } else {
                    gamesPage = gameLibraryRepository.findGamesByUserOrderByTitle(user, pageable);
                }
            }
            
            // Trigger loading of lazy collections for each game
            for (Game game : gamesPage.getContent()) {
                game.getGenres().size(); // Force initialization
                game.getPlatforms().size(); // Force initialization
                game.getPublishers().size(); // Force initialization
                game.getDevelopers().size(); // Force initialization
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error retrieving paginated game library: " + e.getMessage(), e);
        }
        
        PaginatedGameLibraryResponse response = new PaginatedGameLibraryResponse();
        // Set metadata
        response.setLibraryId(gameLibraryRepository.findByUser(user)
                                              .orElseThrow(() ->
                                                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Game library not found"))
                                              .getLibraryId());
        response.setGames(gamesPage.getContent());
        response.setCurrentPage(gamesPage.getNumber());
        response.setTotalPages(gamesPage.getTotalPages());
        response.setTotalElements(gamesPage.getTotalElements());
        response.setPageSize(gamesPage.getSize());
        
        return response;
    }
} 
