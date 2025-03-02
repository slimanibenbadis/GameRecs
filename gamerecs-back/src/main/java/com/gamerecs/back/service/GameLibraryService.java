package com.gamerecs.back.service;

import com.gamerecs.back.model.Game;
import com.gamerecs.back.model.GameLibrary;
import com.gamerecs.back.model.User;
import com.gamerecs.back.repository.GameLibraryRepository;
import com.gamerecs.back.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
} 
