package com.gamerecs.back.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.gamerecs.back.dto.IGDBCompanyDTO;
import com.gamerecs.back.dto.IGDBGenreDTO;
import com.gamerecs.back.dto.IGDBPlatformDTO;
import com.gamerecs.back.model.Developer;
import com.gamerecs.back.model.Genre;
import com.gamerecs.back.model.Platform;
import com.gamerecs.back.model.Publisher;
import com.gamerecs.back.repository.DeveloperRepository;
import com.gamerecs.back.repository.GameRepository;
import com.gamerecs.back.repository.GenreRepository;
import com.gamerecs.back.repository.PlatformRepository;
import com.gamerecs.back.repository.PublisherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

/**
 * Test class for the upsert methods in GameSyncService
 */
@ExtendWith(MockitoExtension.class)
class GameSyncServiceUpsertTest {

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
    
    private GameSyncService gameSyncService;
    
    @BeforeEach
    void setUp() {
        gameSyncService = new GameSyncService(
            gameRepository, 
            publisherRepository, 
            developerRepository, 
            genreRepository,
            platformRepository,
            igdbClientService
        );
    }
    
    @Nested
    @DisplayName("Publisher Upsert Tests")
    class PublisherUpsertTests {
        
        @Test
        @DisplayName("Should return existing publisher when found by IGDB company ID")
        void shouldReturnExistingPublisherWhenFound() {
            // Arrange
            Long igdbCompanyId = 123L;
            String publisherName = "Existing Publisher";
            
            IGDBCompanyDTO companyDTO = new IGDBCompanyDTO();
            companyDTO.setIgdbCompanyId(igdbCompanyId);
            companyDTO.setName(publisherName);
            
            Publisher existingPublisher = new Publisher();
            existingPublisher.setPublisherId(1L);
            existingPublisher.setIgdbCompanyId(igdbCompanyId);
            existingPublisher.setName(publisherName);
            
            when(publisherRepository.findByIgdbCompanyId(igdbCompanyId))
                .thenReturn(Optional.of(existingPublisher));
            
            // Act
            Publisher result = gameSyncService.upsertPublisher(companyDTO);
            
            // Assert
            assertNotNull(result);
            assertEquals(existingPublisher.getPublisherId(), result.getPublisherId());
            assertEquals(igdbCompanyId, result.getIgdbCompanyId());
            assertEquals(publisherName, result.getName());
            
            // Verify repository was called with correct ID
            verify(publisherRepository).findByIgdbCompanyId(igdbCompanyId);
            // Verify save was not called since we found an existing publisher
            verify(publisherRepository, never()).save(any(Publisher.class));
        }
        
        @Test
        @DisplayName("Should create and save new publisher when not found by IGDB company ID")
        void shouldCreateAndSaveNewPublisherWhenNotFound() {
            // Arrange
            Long igdbCompanyId = 456L;
            String publisherName = "New Publisher";
            
            IGDBCompanyDTO companyDTO = new IGDBCompanyDTO();
            companyDTO.setIgdbCompanyId(igdbCompanyId);
            companyDTO.setName(publisherName);
            
            Publisher savedPublisher = new Publisher();
            savedPublisher.setPublisherId(2L);
            savedPublisher.setIgdbCompanyId(igdbCompanyId);
            savedPublisher.setName(publisherName);
            
            when(publisherRepository.findByIgdbCompanyId(igdbCompanyId))
                .thenReturn(Optional.empty());
            when(publisherRepository.save(any(Publisher.class)))
                .thenReturn(savedPublisher);
            
            // Act
            Publisher result = gameSyncService.upsertPublisher(companyDTO);
            
            // Assert
            assertNotNull(result);
            assertEquals(savedPublisher.getPublisherId(), result.getPublisherId());
            assertEquals(igdbCompanyId, result.getIgdbCompanyId());
            assertEquals(publisherName, result.getName());
            
            // Verify repository was called with correct ID
            verify(publisherRepository).findByIgdbCompanyId(igdbCompanyId);
            
            // Capture and verify the saved publisher
            ArgumentCaptor<Publisher> publisherCaptor = ArgumentCaptor.forClass(Publisher.class);
            verify(publisherRepository).save(publisherCaptor.capture());
            
            Publisher capturedPublisher = publisherCaptor.getValue();
            assertEquals(igdbCompanyId, capturedPublisher.getIgdbCompanyId());
            assertEquals(publisherName, capturedPublisher.getName());
        }
        
        @Test
        @DisplayName("Should handle null values in company DTO")
        void shouldHandleNullValuesInCompanyDTO() {
            // Arrange
            IGDBCompanyDTO companyDTO = new IGDBCompanyDTO();
            // Leave fields as null
            
            when(publisherRepository.findByIgdbCompanyId(null))
                .thenReturn(Optional.empty());
            when(publisherRepository.save(any(Publisher.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            
            // Act
            Publisher result = gameSyncService.upsertPublisher(companyDTO);
            
            // Assert
            assertNotNull(result);
            assertNull(result.getIgdbCompanyId());
            assertNull(result.getName());
            
            // Verify repository interactions
            verify(publisherRepository).findByIgdbCompanyId(null);
            verify(publisherRepository).save(any(Publisher.class));
        }
    }
    
    @Nested
    @DisplayName("Developer Upsert Tests")
    class DeveloperUpsertTests {
        
        @Test
        @DisplayName("Should return existing developer when found by IGDB company ID")
        void shouldReturnExistingDeveloperWhenFound() {
            // Arrange
            Long igdbCompanyId = 789L;
            String developerName = "Existing Developer";
            
            IGDBCompanyDTO companyDTO = new IGDBCompanyDTO();
            companyDTO.setIgdbCompanyId(igdbCompanyId);
            companyDTO.setName(developerName);
            
            Developer existingDeveloper = new Developer();
            existingDeveloper.setDeveloperId(3L);
            existingDeveloper.setIgdbCompanyId(igdbCompanyId);
            existingDeveloper.setName(developerName);
            
            when(developerRepository.findByIgdbCompanyId(igdbCompanyId))
                .thenReturn(Optional.of(existingDeveloper));
            
            // Act
            Developer result = gameSyncService.upsertDeveloper(companyDTO);
            
            // Assert
            assertNotNull(result);
            assertEquals(existingDeveloper.getDeveloperId(), result.getDeveloperId());
            assertEquals(igdbCompanyId, result.getIgdbCompanyId());
            assertEquals(developerName, result.getName());
            
            // Verify repository was called with correct ID
            verify(developerRepository).findByIgdbCompanyId(igdbCompanyId);
            // Verify save was not called since we found an existing developer
            verify(developerRepository, never()).save(any(Developer.class));
        }
        
        @Test
        @DisplayName("Should create and save new developer when not found by IGDB company ID")
        void shouldCreateAndSaveNewDeveloperWhenNotFound() {
            // Arrange
            Long igdbCompanyId = 101L;
            String developerName = "New Developer";
            
            IGDBCompanyDTO companyDTO = new IGDBCompanyDTO();
            companyDTO.setIgdbCompanyId(igdbCompanyId);
            companyDTO.setName(developerName);
            
            Developer savedDeveloper = new Developer();
            savedDeveloper.setDeveloperId(4L);
            savedDeveloper.setIgdbCompanyId(igdbCompanyId);
            savedDeveloper.setName(developerName);
            
            when(developerRepository.findByIgdbCompanyId(igdbCompanyId))
                .thenReturn(Optional.empty());
            when(developerRepository.save(any(Developer.class)))
                .thenReturn(savedDeveloper);
            
            // Act
            Developer result = gameSyncService.upsertDeveloper(companyDTO);
            
            // Assert
            assertNotNull(result);
            assertEquals(savedDeveloper.getDeveloperId(), result.getDeveloperId());
            assertEquals(igdbCompanyId, result.getIgdbCompanyId());
            assertEquals(developerName, result.getName());
            
            // Verify repository was called with correct ID
            verify(developerRepository).findByIgdbCompanyId(igdbCompanyId);
            
            // Capture and verify the saved developer
            ArgumentCaptor<Developer> developerCaptor = ArgumentCaptor.forClass(Developer.class);
            verify(developerRepository).save(developerCaptor.capture());
            
            Developer capturedDeveloper = developerCaptor.getValue();
            assertEquals(igdbCompanyId, capturedDeveloper.getIgdbCompanyId());
            assertEquals(developerName, capturedDeveloper.getName());
        }
        
        @Test
        @DisplayName("Should handle null values in company DTO for developer")
        void shouldHandleNullValuesInCompanyDTOForDeveloper() {
            // Arrange
            IGDBCompanyDTO companyDTO = new IGDBCompanyDTO();
            // Leave fields as null
            
            when(developerRepository.findByIgdbCompanyId(null))
                .thenReturn(Optional.empty());
            when(developerRepository.save(any(Developer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            
            // Act
            Developer result = gameSyncService.upsertDeveloper(companyDTO);
            
            // Assert
            assertNotNull(result);
            assertNull(result.getIgdbCompanyId());
            assertNull(result.getName());
            
            // Verify repository interactions
            verify(developerRepository).findByIgdbCompanyId(null);
            verify(developerRepository).save(any(Developer.class));
        }
    }
    
    @Nested
    @DisplayName("Genre Upsert Tests")
    class GenreUpsertTests {
        
        @Test
        @DisplayName("Should return existing genre when found by name")
        void shouldReturnExistingGenreWhenFound() {
            // Arrange
            String genreName = "Existing Genre";
            
            IGDBGenreDTO genreDTO = new IGDBGenreDTO();
            genreDTO.setIgdbGenreId(555L);
            genreDTO.setName(genreName);
            
            Genre existingGenre = new Genre();
            existingGenre.setGenreId(5L);
            existingGenre.setName(genreName);
            
            when(genreRepository.findByName(genreName))
                .thenReturn(Optional.of(existingGenre));
            
            // Act
            Genre result = gameSyncService.upsertGenre(genreDTO);
            
            // Assert
            assertNotNull(result);
            assertEquals(existingGenre.getGenreId(), result.getGenreId());
            assertEquals(genreName, result.getName());
            
            // Verify repository was called with correct name
            verify(genreRepository).findByName(genreName);
            // Verify save was not called since we found an existing genre
            verify(genreRepository, never()).save(any(Genre.class));
        }
        
        @Test
        @DisplayName("Should create and save new genre when not found by name")
        void shouldCreateAndSaveNewGenreWhenNotFound() {
            // Arrange
            String genreName = "New Genre";
            
            IGDBGenreDTO genreDTO = new IGDBGenreDTO();
            genreDTO.setIgdbGenreId(666L);
            genreDTO.setName(genreName);
            
            Genre savedGenre = new Genre();
            savedGenre.setGenreId(6L);
            savedGenre.setName(genreName);
            
            when(genreRepository.findByName(genreName))
                .thenReturn(Optional.empty());
            when(genreRepository.save(any(Genre.class)))
                .thenReturn(savedGenre);
            
            // Act
            Genre result = gameSyncService.upsertGenre(genreDTO);
            
            // Assert
            assertNotNull(result);
            assertEquals(savedGenre.getGenreId(), result.getGenreId());
            assertEquals(genreName, result.getName());
            
            // Verify repository was called with correct name
            verify(genreRepository).findByName(genreName);
            
            // Capture and verify the saved genre
            ArgumentCaptor<Genre> genreCaptor = ArgumentCaptor.forClass(Genre.class);
            verify(genreRepository).save(genreCaptor.capture());
            
            Genre capturedGenre = genreCaptor.getValue();
            assertEquals(genreName, capturedGenre.getName());
        }
        
        @Test
        @DisplayName("Should handle null values in genre DTO")
        void shouldHandleNullValuesInGenreDTO() {
            // Arrange
            IGDBGenreDTO genreDTO = new IGDBGenreDTO();
            // Leave fields as null
            
            when(genreRepository.findByName(null))
                .thenReturn(Optional.empty());
            when(genreRepository.save(any(Genre.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            
            // Act
            Genre result = gameSyncService.upsertGenre(genreDTO);
            
            // Assert
            assertNotNull(result);
            assertNull(result.getName());
            
            // Verify repository interactions
            verify(genreRepository).findByName(null);
            verify(genreRepository).save(any(Genre.class));
        }
    }
    
    @Nested
    @DisplayName("Platform Upsert Tests")
    class PlatformUpsertTests {
        
        @Test
        @DisplayName("Should return existing platform when found by name")
        void shouldReturnExistingPlatformWhenFound() {
            // Arrange
            String platformName = "Existing Platform";
            
            IGDBPlatformDTO platformDTO = new IGDBPlatformDTO();
            platformDTO.setIgdbPlatformId(777L);
            platformDTO.setName(platformName);
            
            Platform existingPlatform = new Platform();
            existingPlatform.setPlatformId(7L);
            existingPlatform.setName(platformName);
            
            when(platformRepository.findByName(platformName))
                .thenReturn(Optional.of(existingPlatform));
            
            // Act
            Platform result = gameSyncService.upsertPlatform(platformDTO);
            
            // Assert
            assertNotNull(result);
            assertEquals(existingPlatform.getPlatformId(), result.getPlatformId());
            assertEquals(platformName, result.getName());
            
            // Verify repository was called with correct name
            verify(platformRepository).findByName(platformName);
            // Verify save was not called since we found an existing platform
            verify(platformRepository, never()).save(any(Platform.class));
        }
        
        @Test
        @DisplayName("Should create and save new platform when not found by name")
        void shouldCreateAndSaveNewPlatformWhenNotFound() {
            // Arrange
            String platformName = "New Platform";
            
            IGDBPlatformDTO platformDTO = new IGDBPlatformDTO();
            platformDTO.setIgdbPlatformId(888L);
            platformDTO.setName(platformName);
            
            Platform savedPlatform = new Platform();
            savedPlatform.setPlatformId(8L);
            savedPlatform.setName(platformName);
            
            when(platformRepository.findByName(platformName))
                .thenReturn(Optional.empty());
            when(platformRepository.save(any(Platform.class)))
                .thenReturn(savedPlatform);
            
            // Act
            Platform result = gameSyncService.upsertPlatform(platformDTO);
            
            // Assert
            assertNotNull(result);
            assertEquals(savedPlatform.getPlatformId(), result.getPlatformId());
            assertEquals(platformName, result.getName());
            
            // Verify repository was called with correct name
            verify(platformRepository).findByName(platformName);
            
            // Capture and verify the saved platform
            ArgumentCaptor<Platform> platformCaptor = ArgumentCaptor.forClass(Platform.class);
            verify(platformRepository).save(platformCaptor.capture());
            
            Platform capturedPlatform = platformCaptor.getValue();
            assertEquals(platformName, capturedPlatform.getName());
        }
        
        @Test
        @DisplayName("Should handle null values in platform DTO")
        void shouldHandleNullValuesInPlatformDTO() {
            // Arrange
            IGDBPlatformDTO platformDTO = new IGDBPlatformDTO();
            // Leave fields as null
            
            when(platformRepository.findByName(null))
                .thenReturn(Optional.empty());
            when(platformRepository.save(any(Platform.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            
            // Act
            Platform result = gameSyncService.upsertPlatform(platformDTO);
            
            // Assert
            assertNotNull(result);
            assertNull(result.getName());
            
            // Verify repository interactions
            verify(platformRepository).findByName(null);
            verify(platformRepository).save(any(Platform.class));
        }
    }
    
    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandling {
        
        @Test
        @DisplayName("Should handle repository exceptions when finding publisher")
        void shouldHandleRepositoryExceptionsWhenFindingPublisher() {
            // Arrange
            IGDBCompanyDTO companyDTO = new IGDBCompanyDTO();
            companyDTO.setIgdbCompanyId(999L);
            companyDTO.setName("Exception Publisher");
            
            when(publisherRepository.findByIgdbCompanyId(999L))
                .thenThrow(new RuntimeException("Database error"));
            
            // Act & Assert
            assertThrows(RuntimeException.class, () -> 
                gameSyncService.upsertPublisher(companyDTO));
            
            // Verify repository was called
            verify(publisherRepository).findByIgdbCompanyId(999L);
            // Verify save was not called due to exception
            verify(publisherRepository, never()).save(any(Publisher.class));
        }
        
        @Test
        @DisplayName("Should handle repository exceptions when saving developer")
        void shouldHandleRepositoryExceptionsWhenSavingDeveloper() {
            // Arrange
            IGDBCompanyDTO companyDTO = new IGDBCompanyDTO();
            companyDTO.setIgdbCompanyId(1010L);
            companyDTO.setName("Exception Developer");
            
            when(developerRepository.findByIgdbCompanyId(1010L))
                .thenReturn(Optional.empty());
            when(developerRepository.save(any(Developer.class)))
                .thenThrow(new RuntimeException("Database error on save"));
            
            // Act & Assert
            assertThrows(RuntimeException.class, () -> 
                gameSyncService.upsertDeveloper(companyDTO));
            
            // Verify repository interactions
            verify(developerRepository).findByIgdbCompanyId(1010L);
            verify(developerRepository).save(any(Developer.class));
        }
        
        @Test
        @DisplayName("Should handle repository exceptions when finding genre")
        void shouldHandleRepositoryExceptionsWhenFindingGenre() {
            // Arrange
            IGDBGenreDTO genreDTO = new IGDBGenreDTO();
            genreDTO.setIgdbGenreId(1111L);
            genreDTO.setName("Exception Genre");
            
            when(genreRepository.findByName("Exception Genre"))
                .thenThrow(new RuntimeException("Database error"));
            
            // Act & Assert
            assertThrows(RuntimeException.class, () -> 
                gameSyncService.upsertGenre(genreDTO));
            
            // Verify repository was called
            verify(genreRepository).findByName("Exception Genre");
            // Verify save was not called due to exception
            verify(genreRepository, never()).save(any(Genre.class));
        }
        
        @Test
        @DisplayName("Should handle repository exceptions when saving platform")
        void shouldHandleRepositoryExceptionsWhenSavingPlatform() {
            // Arrange
            IGDBPlatformDTO platformDTO = new IGDBPlatformDTO();
            platformDTO.setIgdbPlatformId(1212L);
            platformDTO.setName("Exception Platform");
            
            when(platformRepository.findByName("Exception Platform"))
                .thenReturn(Optional.empty());
            when(platformRepository.save(any(Platform.class)))
                .thenThrow(new RuntimeException("Database error on save"));
            
            // Act & Assert
            assertThrows(RuntimeException.class, () -> 
                gameSyncService.upsertPlatform(platformDTO));
            
            // Verify repository interactions
            verify(platformRepository).findByName("Exception Platform");
            verify(platformRepository).save(any(Platform.class));
        }
    }
} 
