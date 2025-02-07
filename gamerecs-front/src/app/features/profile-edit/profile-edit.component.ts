import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ProfileService } from '../../core/services/profile.service';
import { ProfileResponseDto } from '../../models/profile-response.dto';

@Component({
  selector: 'app-profile-edit',
  templateUrl: './profile-edit.component.html',
  styleUrl: './profile-edit.component.css',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    InputTextModule,
    TextareaModule,
    ButtonModule,
    MessageModule,
    ProgressSpinnerModule
  ]
})
export class ProfileEditComponent implements OnInit {
  profileForm: FormGroup;
  isLoading = false;
  error: string | undefined;
  successMessage: string | undefined;

  constructor(
    private fb: FormBuilder,
    private profileService: ProfileService,
    private router: Router
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

  onSave(): void {
    if (this.profileForm.invalid) {
      // Mark all fields as touched to trigger validation messages
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
    const updateData = {
      username: formValue.username,
      profilePictureUrl: formValue.profilePictureUrl || undefined,
      bio: formValue.bio || undefined
    };

    this.profileService.updateProfile(updateData).subscribe({
      next: (response) => {
        this.isLoading = false;
        this.successMessage = 'Profile updated successfully';
        // Navigate to profile view after a short delay
        setTimeout(() => {
          this.router.navigate(['/profile']);
        }, 1500);
      },
      error: (error) => {
        this.isLoading = false;
        this.error = error.error?.message || 'Failed to update profile';
        console.error('Error updating profile:', error);
      }
    });
  }

  private loadProfile(): void {
    this.isLoading = true;
    this.profileService.getProfile().subscribe({
      next: (profile: ProfileResponseDto) => {
        this.profileForm.patchValue({
          username: profile.username,
          profilePictureUrl: profile.profilePictureUrl,
          bio: profile.bio
        });
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading profile:', error);
        this.error = 'Failed to load profile data';
        this.isLoading = false;
      }
    });
  }

  // Helper method to get form control error messages
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
