package com.gamerecs.back.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class IGDBGameDTO {
    @JsonProperty("id")
    private Long igdbId;
    
    @JsonProperty("name")
    private String title;
    
    @JsonProperty("summary")
    private String description;
    
    @JsonProperty("first_release_date")
    private Long releaseDateTimestamp;
    
    private LocalDate releaseDate;
    
    @JsonProperty("cover")
    private Cover coverImage;
    
    @JsonProperty("updated_at")
    private Long updatedAt;
    
    private List<IGDBPlatformDTO> platforms;
    private List<IGDBGenreDTO> genres;
    
    @JsonProperty("involved_companies")
    private List<IGDBInvolvedCompanyDTO> involvedCompanies;
    
    private List<IGDBCompanyDTO> publishers = new ArrayList<>();
    private List<IGDBCompanyDTO> developers = new ArrayList<>();
    
    private String coverImageUrl;
    
    @Data
    public static class Cover {
        private String url;
    }
    
    public void processInvolvedCompanies() {
        if (involvedCompanies == null) return;
        
        publishers.clear();
        developers.clear();
        
        for (IGDBInvolvedCompanyDTO involved : involvedCompanies) {
            if (involved.getCompany() == null) continue;
            
            if (involved.isPublisher()) {
                publishers.add(involved.getCompany());
            }
            if (involved.isDeveloper()) {
                developers.add(involved.getCompany());
            }
        }
    }
} 
