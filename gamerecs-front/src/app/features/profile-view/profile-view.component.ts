import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageModule } from 'primeng/message';
import { ButtonModule } from 'primeng/button';
import { InputTextarea } from 'primeng/inputtextarea';
import { ProfileService } from '../../core/services/profile.service';
import { ProfileResponseDto } from '../../models/profile-response.dto';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-profile-view',
  templateUrl: './profile-view.component.html',
  styleUrl: './profile-view.component.css',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ProgressSpinnerModule,
    MessageModule,
    ButtonModule,
    InputTextarea
  ]
})
export class ProfileViewComponent implements OnInit {
  profile: ProfileResponseDto | undefined;
  error: string | undefined;
  isLoading: boolean = true;
  isEditing: boolean = false;
  editedBio: string = '';
  isOwnProfile: boolean = true; // This will be set based on route params in a future update

  constructor(private profileService: ProfileService) {}

  ngOnInit(): void {
    this.loadProfile();
  }

  private loadProfile(): void {
    this.profileService.getProfile().subscribe({
      next: (data: ProfileResponseDto) => {
        this.profile = data;
        this.editedBio = data.bio;
        this.isLoading = false;
      },
      error: (err: HttpErrorResponse) => {
        console.error('Profile retrieval error:', err);
        this.error = 'Failed to load profile. Please try again.';
        this.isLoading = false;
      }
    });
  }

  handleImageError(event: Event): void {
    const imgElement = event.target as HTMLImageElement;
    imgElement.src = 'assets/images/default-avatar.png';
  }

  onUpdateProfilePicture(): void {
    // This will be implemented in a future update
    console.log('Update profile picture clicked');
  }

  startEditing(): void {
    this.isEditing = true;
    this.editedBio = this.profile?.bio || '';
  }

  cancelEditing(): void {
    this.isEditing = false;
    this.editedBio = this.profile?.bio || '';
  }

  saveBio(): void {
    if (!this.profile) return;
    
    this.isLoading = true;
    this.profileService.updateBio(this.editedBio).subscribe({
      next: (updatedProfile: ProfileResponseDto) => {
        this.profile = updatedProfile;
        this.isEditing = false;
        this.isLoading = false;
      },
      error: (err: HttpErrorResponse) => {
        console.error('Bio update error:', err);
        this.error = 'Failed to update bio. Please try again.';
        this.isLoading = false;
      }
    });
  }
}
