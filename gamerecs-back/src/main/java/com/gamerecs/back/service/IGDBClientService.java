package com.gamerecs.back.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamerecs.back.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

@Service
public class IGDBClientService {
    private static final Logger logger = LoggerFactory.getLogger(IGDBClientService.class);
    
    private final RestTemplate restTemplate;
    private final String clientId;
    private final String accessToken;
    private final ObjectMapper objectMapper;

    public IGDBClientService(
            RestTemplate restTemplate,
            @Qualifier("igdbClientId") String clientId,
            @Qualifier("igdbAccessToken") String accessToken,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.clientId = clientId;
        this.accessToken = accessToken;
        this.objectMapper = objectMapper;
    }

    public List<IGDBGameDTO> searchGames(String query) {
        try {
            String endpoint = "https://api.igdb.com/v4/games";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Client-ID", clientId);
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.TEXT_PLAIN);
            
            String body = """
                search "%s";
                fields name,cover.url,first_release_date,summary,platforms.name,genres.name,
                      involved_companies.company.name,involved_companies.developer,involved_companies.publisher,
                      updated_at;
                      where first_release_date != null & version_parent = null & game_type = 0;
                limit 500;
                """.formatted(query);
            
            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                endpoint,
                HttpMethod.POST,
                request,
                String.class
            );
            
            List<IGDBGameDTO> games = objectMapper.readValue(
                response.getBody(),
                new TypeReference<List<IGDBGameDTO>>() {}
            );
            
            // Post-process the games
            for (IGDBGameDTO game : games) {
                // Convert cover URL
                if (game.getCoverImage() != null) {
                    game.setCoverImageUrl(convertCoverUrl(game.getCoverImage().getUrl()));
                }
                
                // Convert release date to LocalDate
                if (game.getReleaseDateTimestamp() != null) {
                    game.setReleaseDate(
                        Instant.ofEpochSecond(game.getReleaseDateTimestamp())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    );
                }
                
                // Process involved companies into publishers and developers
                game.processInvolvedCompanies();
            }
            
            return games;
        } catch (Exception e) {
            logger.error("Failed to parse IGDB API response", e);
            throw new RuntimeException("Failed to parse IGDB API response", e);
        }
    }

    public String convertCoverUrl(String originalUrl) {
        if (originalUrl == null) {
            return null;
        }
        
        String httpsUrl = originalUrl.startsWith("//") 
            ? "https:" + originalUrl 
            : originalUrl;
            
        return httpsUrl.replace("t_thumb", "t_cover_big");
    }
} 
