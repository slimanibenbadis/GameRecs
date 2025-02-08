import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ProfileService } from '../../core/services/profile.service';
import { ProfileResponseDto, UpdateProfileRequest } from '../../models/profile-response.dto';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    InputTextModule,
    TextareaModule,
    ButtonModule,
    MessageModule,
    ProgressSpinnerModule
  ]
})
export class ProfileComponent implements OnInit {
  profile: ProfileResponseDto | undefined;
  profileForm: FormGroup;
  isLoading = false;
  error: string | undefined;
  successMessage: string | undefined;
  isEditMode = false;
  isOwnProfile = true; // This will be set based on route params in a future update

  constructor(
    private fb: FormBuilder,
    private profileService: ProfileService
  ) {
    this.profileForm = this.fb.group({
      username: ['', [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(20),
        Validators.pattern('^[a-zA-Z0-9_-]*$')
      ]],
      profilePictureUrl: ['', [
        Validators.pattern('^(https?:\\/\\/.*\\.(?:png|jpg|jpeg|gif))$')
      ]],
      bio: ['', [
        Validators.maxLength(500)
      ]]
    });
  }

  ngOnInit(): void {
    this.loadProfile();
  }

  private loadProfile(): void {
    this.isLoading = true;
    this.error = undefined;
    
    this.profileService.getProfile().subscribe({
      next: (profile: ProfileResponseDto) => {
        this.profile = profile;
        this.profileForm.patchValue({
          username: profile.username,
          profilePictureUrl: profile.profilePictureUrl,
          bio: profile.bio
        });
        this.isLoading = false;
      },
      error: (error) => {
        console.error('[ProfileComponent] Error loading profile:', error);
        this.error = 'Failed to load profile data';
        this.isLoading = false;
      }
    });
  }

  toggleEditMode(): void {
    if (this.isEditMode) {
      // Reset form to current profile values when canceling edit
      this.profileForm.patchValue({
        username: this.profile?.username,
        profilePictureUrl: this.profile?.profilePictureUrl,
        bio: this.profile?.bio
      });
    }
    this.isEditMode = !this.isEditMode;
    this.error = undefined;
    this.successMessage = undefined;
  }

  onSave(): void {
    if (this.profileForm.invalid) {
      Object.keys(this.profileForm.controls).forEach(key => {
        const control = this.profileForm.get(key);
        control?.markAsTouched();
      });
      return;
    }

    this.isLoading = true;
    this.error = undefined;
    this.successMessage = undefined;

    const formValue = this.profileForm.value;
    const updateData: UpdateProfileRequest = {
      username: formValue.username,
      profilePictureUrl: formValue.profilePictureUrl || undefined,
      bio: formValue.bio || undefined
    };

    this.profileService.updateProfile(updateData).subscribe({
      next: (response) => {
        this.profile = response;
        this.isLoading = false;
        this.successMessage = 'Profile updated successfully';
        this.isEditMode = false;
      },
      error: (error) => {
        console.error('[ProfileComponent] Error updating profile:', error);
        this.isLoading = false;
        this.error = error.error?.message || 'Failed to update profile';
      }
    });
  }

  handleImageError(event: Event): void {
    const imgElement = event.target as HTMLImageElement;
    imgElement.src = 'assets/images/default-avatar.png';
  }

  getErrorMessage(controlName: string): string {
    const control = this.profileForm.get(controlName);
    if (!control?.errors || !control.touched) return '';

    const errors = control.errors;
    
    switch (controlName) {
    case 'username':
      if (errors['required']) return 'Username is required';
      if (errors['minlength']) return 'Username must be at least 3 characters';
      if (errors['maxlength']) return 'Username cannot exceed 20 characters';
      if (errors['pattern']) return 'Username can only contain letters, numbers, underscores, and hyphens';
      break;
    case 'profilePictureUrl':
      if (errors['pattern']) return 'Please enter a valid image URL (http/https ending in .png, .jpg, .jpeg, or .gif)';
      break;
    case 'bio':
      if (errors['maxlength']) return 'Bio cannot exceed 500 characters';
      break;
    }
    return '';
  }
} 
