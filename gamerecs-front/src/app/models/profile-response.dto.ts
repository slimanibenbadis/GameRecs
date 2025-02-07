export interface ProfileResponseDto {
  username: string;
  email: string;
  profilePictureUrl: string;
  bio: string;
  emailVerified: boolean;
  gamesRated: number;
  gamesInLibrary: number;
  joinDate: string;
}

export interface UpdateProfileRequest {
  username: string;
  profilePictureUrl?: string;
  bio?: string;
} 
