package com.gamerecs.back.service;

import com.gamerecs.back.model.GameLibrary;
import com.gamerecs.back.model.User;
import com.gamerecs.back.repository.GameLibraryRepository;
import com.gamerecs.back.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
} 
