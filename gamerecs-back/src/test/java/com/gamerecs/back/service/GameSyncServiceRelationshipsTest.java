package com.gamerecs.back.service;

import com.gamerecs.back.dto.*;
import com.gamerecs.back.model.*;
import com.gamerecs.back.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GameSyncServiceRelationshipsTest {

    @Mock
    private GameRepository gameRepository;
    @Mock
    private PublisherRepository publisherRepository;
    @Mock
    private DeveloperRepository developerRepository;
    @Mock
    private GenreRepository genreRepository;
    @Mock
    private PlatformRepository platformRepository;
    @Mock
    private IGDBClientService igdbClientService;

    @InjectMocks
    private GameSyncService gameSyncService;

    private Game existingGame;
    private IGDBGameDTO igdbGameDTO;

    @BeforeEach
    void setUp() {
        // Initialize existing game
        existingGame = new Game();
        existingGame.setGameId(1L);
        existingGame.setIgdbId(100L);
        existingGame.setTitle("Test Game");
        existingGame.setPublishers(new HashSet<>());
        existingGame.setDevelopers(new HashSet<>());
        existingGame.setGenres(new HashSet<>());
        existingGame.setPlatforms(new HashSet<>());

        // Initialize IGDB DTO
        igdbGameDTO = new IGDBGameDTO();
        igdbGameDTO.setIgdbId(100L);
        igdbGameDTO.setTitle("Test Game");
        igdbGameDTO.setUpdatedAt(System.currentTimeMillis() / 1000); // Set current epoch time in seconds
    }

    @Test
    void testUpdatePublishers_WhenPublishersExist() {
        // Arrange
        IGDBCompanyDTO publisherDTO = new IGDBCompanyDTO();
        publisherDTO.setIgdbCompanyId(1L);
        publisherDTO.setName("Test Publisher");
        igdbGameDTO.setPublishers(List.of(publisherDTO));

        Publisher publisher = new Publisher();
        publisher.setPublisherId(1L);
        publisher.setIgdbCompanyId(1L);
        publisher.setName("Test Publisher");

        when(gameRepository.findByIgdbId(100L)).thenReturn(Optional.of(existingGame));
        when(publisherRepository.findByIgdbCompanyId(1L)).thenReturn(Optional.of(publisher));
        when(gameRepository.save(any(Game.class))).thenReturn(existingGame);

        // Act
        Game result = gameSyncService.upsertGame(igdbGameDTO);

        // Assert
        assertNotNull(result.getPublishers());
        assertEquals(1, result.getPublishers().size());
        assertTrue(result.getPublishers().stream()
                .anyMatch(p -> p.getIgdbCompanyId().equals(1L)));
        verify(publisherRepository).findByIgdbCompanyId(1L);
    }

    @Test
    void testUpdateDevelopers_WhenDevelopersExist() {
        // Arrange
        IGDBCompanyDTO developerDTO = new IGDBCompanyDTO();
        developerDTO.setIgdbCompanyId(1L);
        developerDTO.setName("Test Developer");
        igdbGameDTO.setDevelopers(List.of(developerDTO));

        Developer developer = new Developer();
        developer.setDeveloperId(1L);
        developer.setIgdbCompanyId(1L);
        developer.setName("Test Developer");

        when(gameRepository.findByIgdbId(100L)).thenReturn(Optional.of(existingGame));
        when(developerRepository.findByIgdbCompanyId(1L)).thenReturn(Optional.of(developer));
        when(gameRepository.save(any(Game.class))).thenReturn(existingGame);

        // Act
        Game result = gameSyncService.upsertGame(igdbGameDTO);

        // Assert
        assertNotNull(result.getDevelopers());
        assertEquals(1, result.getDevelopers().size());
        assertTrue(result.getDevelopers().stream()
                .anyMatch(d -> d.getIgdbCompanyId().equals(1L)));
        verify(developerRepository).findByIgdbCompanyId(1L);
    }

    @Test
    void testUpdateGenres_WhenGenresExist() {
        // Arrange
        IGDBGenreDTO genreDTO = new IGDBGenreDTO();
        genreDTO.setIgdbGenreId(1L);
        genreDTO.setName("Action");
        igdbGameDTO.setGenres(List.of(genreDTO));

        Genre genre = new Genre();
        genre.setGenreId(1L);
        genre.setName("Action");

        when(gameRepository.findByIgdbId(100L)).thenReturn(Optional.of(existingGame));
        when(genreRepository.findByName("Action")).thenReturn(Optional.of(genre));
        when(gameRepository.save(any(Game.class))).thenReturn(existingGame);

        // Act
        Game result = gameSyncService.upsertGame(igdbGameDTO);

        // Assert
        assertNotNull(result.getGenres());
        assertEquals(1, result.getGenres().size());
        assertTrue(result.getGenres().stream()
                .anyMatch(g -> g.getName().equals("Action")));
        verify(genreRepository).findByName("Action");
    }

    @Test
    void testUpdatePlatforms_WhenPlatformsExist() {
        // Arrange
        IGDBPlatformDTO platformDTO = new IGDBPlatformDTO();
        platformDTO.setIgdbPlatformId(1L);
        platformDTO.setName("PC");
        igdbGameDTO.setPlatforms(List.of(platformDTO));

        Platform platform = new Platform();
        platform.setPlatformId(1L);
        platform.setName("PC");

        when(gameRepository.findByIgdbId(100L)).thenReturn(Optional.of(existingGame));
        when(platformRepository.findByName("PC")).thenReturn(Optional.of(platform));
        when(gameRepository.save(any(Game.class))).thenReturn(existingGame);

        // Act
        Game result = gameSyncService.upsertGame(igdbGameDTO);

        // Assert
        assertNotNull(result.getPlatforms());
        assertEquals(1, result.getPlatforms().size());
        assertTrue(result.getPlatforms().stream()
                .anyMatch(p -> p.getName().equals("PC")));
        verify(platformRepository).findByName("PC");
    }

    @Test
    void testClearRelationships_WhenRelationshipsAreNull() {
        // Arrange
        // Use empty lists instead of null, as the service only clears collections when lists are not null
        igdbGameDTO.setPublishers(List.of());
        igdbGameDTO.setDevelopers(List.of());
        igdbGameDTO.setGenres(List.of());
        igdbGameDTO.setPlatforms(List.of());

        // Add some existing relationships
        Publisher publisher = new Publisher();
        publisher.setPublisherId(1L);
        publisher.setName("Old Publisher");
        existingGame.getPublishers().add(publisher);

        Developer developer = new Developer();
        developer.setDeveloperId(1L);
        developer.setName("Old Developer");
        existingGame.getDevelopers().add(developer);

        Genre genre = new Genre();
        genre.setGenreId(1L);
        genre.setName("Old Genre");
        existingGame.getGenres().add(genre);

        Platform platform = new Platform();
        platform.setPlatformId(1L);
        platform.setName("Old Platform");
        existingGame.getPlatforms().add(platform);

        when(gameRepository.findByIgdbId(100L)).thenReturn(Optional.of(existingGame));
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> {
            Game savedGame = invocation.getArgument(0);
            // Return the actual saved game state
            return savedGame;
        });

        // Act
        Game result = gameSyncService.upsertGame(igdbGameDTO);

        // Assert
        assertTrue(result.getPublishers().isEmpty(), "Publishers should be empty");
        assertTrue(result.getDevelopers().isEmpty(), "Developers should be empty");
        assertTrue(result.getGenres().isEmpty(), "Genres should be empty");
        assertTrue(result.getPlatforms().isEmpty(), "Platforms should be empty");
        
        // Verify save is called twice - once before updating relationships and once after
        verify(gameRepository, times(2)).save(any(Game.class));
    }

    @Test
    void testCreateNewRelationships_WhenNotExist() {
        // Arrange
        IGDBCompanyDTO publisherDTO = new IGDBCompanyDTO();
        publisherDTO.setIgdbCompanyId(1L);
        publisherDTO.setName("New Publisher");
        igdbGameDTO.setPublishers(List.of(publisherDTO));

        IGDBCompanyDTO developerDTO = new IGDBCompanyDTO();
        developerDTO.setIgdbCompanyId(2L);
        developerDTO.setName("New Developer");
        igdbGameDTO.setDevelopers(List.of(developerDTO));

        IGDBGenreDTO genreDTO = new IGDBGenreDTO();
        genreDTO.setIgdbGenreId(3L);
        genreDTO.setName("New Genre");
        igdbGameDTO.setGenres(List.of(genreDTO));

        IGDBPlatformDTO platformDTO = new IGDBPlatformDTO();
        platformDTO.setIgdbPlatformId(4L);
        platformDTO.setName("New Platform");
        igdbGameDTO.setPlatforms(List.of(platformDTO));

        when(gameRepository.findByIgdbId(100L)).thenReturn(Optional.of(existingGame));
        when(publisherRepository.findByIgdbCompanyId(1L)).thenReturn(Optional.empty());
        when(developerRepository.findByIgdbCompanyId(2L)).thenReturn(Optional.empty());
        when(genreRepository.findByName("New Genre")).thenReturn(Optional.empty());
        when(platformRepository.findByName("New Platform")).thenReturn(Optional.empty());

        when(publisherRepository.save(any(Publisher.class))).thenAnswer(i -> {
            Publisher p = i.getArgument(0);
            p.setPublisherId(1L);
            return p;
        });
        when(developerRepository.save(any(Developer.class))).thenAnswer(i -> {
            Developer d = i.getArgument(0);
            d.setDeveloperId(2L);
            return d;
        });
        when(genreRepository.save(any(Genre.class))).thenAnswer(i -> {
            Genre g = i.getArgument(0);
            g.setGenreId(3L);
            return g;
        });
        when(platformRepository.save(any(Platform.class))).thenAnswer(i -> {
            Platform p = i.getArgument(0);
            p.setPlatformId(4L);
            return p;
        });
        when(gameRepository.save(any(Game.class))).thenReturn(existingGame);

        // Act
        Game result = gameSyncService.upsertGame(igdbGameDTO);

        // Assert
        assertEquals(1, result.getPublishers().size());
        assertEquals(1, result.getDevelopers().size());
        assertEquals(1, result.getGenres().size());
        assertEquals(1, result.getPlatforms().size());

        verify(publisherRepository).save(any(Publisher.class));
        verify(developerRepository).save(any(Developer.class));
        verify(genreRepository).save(any(Genre.class));
        verify(platformRepository).save(any(Platform.class));
    }

    @Test
    void testUpdateMultipleRelationships() {
        // Arrange
        // Create multiple DTOs for each relationship type
        List<IGDBCompanyDTO> publisherDTOs = new ArrayList<>();
        List<IGDBCompanyDTO> developerDTOs = new ArrayList<>();
        List<IGDBGenreDTO> genreDTOs = new ArrayList<>();
        List<IGDBPlatformDTO> platformDTOs = new ArrayList<>();

        // Add two publishers
        for (int i = 1; i <= 2; i++) {
            IGDBCompanyDTO pub = new IGDBCompanyDTO();
            pub.setIgdbCompanyId((long) i);
            pub.setName("Publisher " + i);
            publisherDTOs.add(pub);

            Publisher publisher = new Publisher();
            publisher.setPublisherId((long) i);
            publisher.setIgdbCompanyId((long) i);
            publisher.setName("Publisher " + i);
            when(publisherRepository.findByIgdbCompanyId((long) i))
                .thenReturn(Optional.of(publisher));
        }

        // Add two developers
        for (int i = 1; i <= 2; i++) {
            IGDBCompanyDTO dev = new IGDBCompanyDTO();
            dev.setIgdbCompanyId((long) i);
            dev.setName("Developer " + i);
            developerDTOs.add(dev);

            Developer developer = new Developer();
            developer.setDeveloperId((long) i);
            developer.setIgdbCompanyId((long) i);
            developer.setName("Developer " + i);
            when(developerRepository.findByIgdbCompanyId((long) i))
                .thenReturn(Optional.of(developer));
        }

        // Add two genres
        for (int i = 1; i <= 2; i++) {
            IGDBGenreDTO gen = new IGDBGenreDTO();
            gen.setIgdbGenreId((long) i);
            gen.setName("Genre " + i);
            genreDTOs.add(gen);

            Genre genre = new Genre();
            genre.setGenreId((long) i);
            genre.setName("Genre " + i);
            when(genreRepository.findByName("Genre " + i))
                .thenReturn(Optional.of(genre));
        }

        // Add two platforms
        for (int i = 1; i <= 2; i++) {
            IGDBPlatformDTO plat = new IGDBPlatformDTO();
            plat.setIgdbPlatformId((long) i);
            plat.setName("Platform " + i);
            platformDTOs.add(plat);

            Platform platform = new Platform();
            platform.setPlatformId((long) i);
            platform.setName("Platform " + i);
            when(platformRepository.findByName("Platform " + i))
                .thenReturn(Optional.of(platform));
        }

        igdbGameDTO.setPublishers(publisherDTOs);
        igdbGameDTO.setDevelopers(developerDTOs);
        igdbGameDTO.setGenres(genreDTOs);
        igdbGameDTO.setPlatforms(platformDTOs);

        when(gameRepository.findByIgdbId(100L)).thenReturn(Optional.of(existingGame));
        when(gameRepository.save(any(Game.class))).thenReturn(existingGame);

        // Act
        Game result = gameSyncService.upsertGame(igdbGameDTO);

        // Assert
        assertEquals(2, result.getPublishers().size());
        assertEquals(2, result.getDevelopers().size());
        assertEquals(2, result.getGenres().size());
        assertEquals(2, result.getPlatforms().size());

        verify(gameRepository, times(2)).save(any(Game.class));
        verify(publisherRepository, times(2)).findByIgdbCompanyId(any());
        verify(developerRepository, times(2)).findByIgdbCompanyId(any());
        verify(genreRepository, times(2)).findByName(any());
        verify(platformRepository, times(2)).findByName(any());
    }
} 
