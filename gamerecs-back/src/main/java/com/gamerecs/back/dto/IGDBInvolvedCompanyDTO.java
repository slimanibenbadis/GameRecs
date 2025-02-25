package com.gamerecs.back.dto;

import lombok.Data;

@Data
public class IGDBInvolvedCompanyDTO {
    private IGDBCompanyDTO company;
    private boolean developer;
    private boolean publisher;
} 
