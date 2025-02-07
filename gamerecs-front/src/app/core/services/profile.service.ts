import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ProfileResponseDto, UpdateProfileRequest } from '../../models/profile-response.dto';

@Injectable({
  providedIn: 'root'
})
export class ProfileService {
  private readonly profileUrl = '/api/users/profile';

  constructor(private http: HttpClient) {}

  /**
   * Retrieves the profile information of the currently authenticated user
   * @returns An Observable of ProfileResponseDto containing the user's profile data
   */
  getProfile(): Observable<ProfileResponseDto> {
    return this.http.get<ProfileResponseDto>(this.profileUrl);
  }

  /**
   * Updates the user's bio
   * @param bio The new bio text
   * @returns An Observable of ProfileResponseDto containing the updated user's profile data
   */
  updateBio(bio: string): Observable<ProfileResponseDto> {
    return this.http.patch<ProfileResponseDto>(`${this.profileUrl}/bio`, { bio });
  }

  /**
   * Updates the user's complete profile information
   * @param profile The profile data to update
   * @returns An Observable of ProfileResponseDto containing the updated user's profile data
   */
  updateProfile(profile: UpdateProfileRequest): Observable<ProfileResponseDto> {
    return this.http.put<ProfileResponseDto>(this.profileUrl, profile);
  }
} 
