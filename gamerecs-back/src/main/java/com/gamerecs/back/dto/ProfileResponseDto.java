package com.gamerecs.back.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponseDto {
    private String username;
    private String email;
    private String profilePictureUrl;
    private String bio;
    private boolean emailVerified;
} 
