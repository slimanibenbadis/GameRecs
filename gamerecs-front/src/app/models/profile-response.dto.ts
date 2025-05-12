export interface ProfileResponseDto {
  username: string;
  email: string;
  profilePictureUrl?: string;
  bio?: string;
  emailVerified: boolean;
  steamProfileId?: string;
  steamCredentialsSet?: boolean;
}

export interface UpdateProfileRequest {
  username: string;
  profilePictureUrl?: string;
  bio?: string;
  steamApiKey?: string;
  steamProfileId?: string;
} 
