package com.gamerecs.back.service;

import com.gamerecs.back.dto.*;
import com.gamerecs.back.model.*;
import com.gamerecs.back.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameSyncService {
    private final GameRepository gameRepository;
    private final PublisherRepository publisherRepository;
    private final DeveloperRepository developerRepository;
    private final GenreRepository genreRepository;
    private final PlatformRepository platformRepository;
    private final IGDBClientService igdbClientService;

    @Transactional
    public List<Game> syncGamesFromSearch(String searchQuery) {
        List<IGDBGameDTO> igdbGames = igdbClientService.searchGames(searchQuery);
        return syncGamesFromSearch(igdbGames);
    }

    @Transactional
    public List<Game> syncGamesFromSearch(List<IGDBGameDTO> igdbGames) {
        return igdbGames.stream()
            .map(this::upsertGame)
            .toList();
    }

    @Transactional
    public Game upsertGame(IGDBGameDTO igdbGame) {
        // Check if game exists
        Optional<Game> existingGame = gameRepository.findByIgdbId(igdbGame.getIgdbId());
        
        // If game exists and has a newer or equal update time, skip update
        if (existingGame.isPresent()) {
            LocalDateTime existingUpdateTime = existingGame.get().getUpdatedAt();
            LocalDateTime incomingUpdateTime = Instant.ofEpochSecond(igdbGame.getUpdatedAt())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
                
            if (existingUpdateTime != null && !incomingUpdateTime.isAfter(existingUpdateTime)) {
                log.debug("Game {} already up to date", igdbGame.getTitle());
                return existingGame.get();
            }
        }

        // Create or update game
        Game game = existingGame.orElse(new Game());
        updateGameFromDTO(game, igdbGame);
        
        // Save game first to ensure it exists for relationships
        game = gameRepository.save(game);
        
        // Update publishers
        if (igdbGame.getPublishers() != null) {
            game.getPublishers().clear();
            for (IGDBCompanyDTO publisherDTO : igdbGame.getPublishers()) {
                Publisher publisher = upsertPublisher(publisherDTO);
                game.getPublishers().add(publisher);
            }
        }
        
        // Update developers
        if (igdbGame.getDevelopers() != null) {
            game.getDevelopers().clear();
            for (IGDBCompanyDTO developerDTO : igdbGame.getDevelopers()) {
                Developer developer = upsertDeveloper(developerDTO);
                game.getDevelopers().add(developer);
            }
        }
        
        // Update genres
        if (igdbGame.getGenres() != null) {
            game.getGenres().clear();
            for (IGDBGenreDTO genreDTO : igdbGame.getGenres()) {
                Genre genre = upsertGenre(genreDTO);
                game.getGenres().add(genre);
            }
        }
        
        // Update platforms
        if (igdbGame.getPlatforms() != null) {
            game.getPlatforms().clear();
            for (IGDBPlatformDTO platformDTO : igdbGame.getPlatforms()) {
                Platform platform = upsertPlatform(platformDTO);
                game.getPlatforms().add(platform);
            }
        }
        
        return gameRepository.save(game);
    }

    private void updateGameFromDTO(Game game, IGDBGameDTO dto) {
        game.setIgdbId(dto.getIgdbId());
        game.setTitle(dto.getTitle());
        game.setDescription(dto.getDescription());
        game.setReleaseDate(dto.getReleaseDate());
        game.setCoverImageUrl(dto.getCoverImageUrl());
        game.setUpdatedAt(
            Instant.ofEpochSecond(dto.getUpdatedAt())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
        );
    }

    @Transactional
    public Publisher upsertPublisher(IGDBCompanyDTO companyDTO) {
        return publisherRepository.findByIgdbCompanyId(companyDTO.getIgdbCompanyId())
            .orElseGet(() -> {
                Publisher publisher = new Publisher();
                publisher.setIgdbCompanyId(companyDTO.getIgdbCompanyId());
                publisher.setName(companyDTO.getName());
                return publisherRepository.save(publisher);
            });
    }

    @Transactional
    public Developer upsertDeveloper(IGDBCompanyDTO companyDTO) {
        return developerRepository.findByIgdbCompanyId(companyDTO.getIgdbCompanyId())
            .orElseGet(() -> {
                Developer developer = new Developer();
                developer.setIgdbCompanyId(companyDTO.getIgdbCompanyId());
                developer.setName(companyDTO.getName());
                return developerRepository.save(developer);
            });
    }

    @Transactional
    public Genre upsertGenre(IGDBGenreDTO genreDTO) {
        return genreRepository.findByName(genreDTO.getName())
            .orElseGet(() -> {
                Genre genre = new Genre();
                genre.setName(genreDTO.getName());
                return genreRepository.save(genre);
            });
    }

    @Transactional
    public Platform upsertPlatform(IGDBPlatformDTO platformDTO) {
        return platformRepository.findByName(platformDTO.getName())
            .orElseGet(() -> {
                Platform platform = new Platform();
                platform.setName(platformDTO.getName());
                return platformRepository.save(platform);
            });
    }
} 
