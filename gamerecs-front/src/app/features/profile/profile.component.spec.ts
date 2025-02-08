import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ProfileComponent } from './profile.component';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ProfileService } from '../../core/services/profile.service';
import { of, throwError } from 'rxjs';
import { ProfileResponseDto } from '../../models/profile-response.dto';
import { CommonModule } from '@angular/common';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

describe('ProfileComponent', () => {
  let component: ProfileComponent;
  let fixture: ComponentFixture<ProfileComponent>;
  let profileService: jasmine.SpyObj<ProfileService>;
  const mockProfile: ProfileResponseDto = {
    username: 'testUser',
    email: 'test@example.com',
    profilePictureUrl: 'https://example.com/image.png',
    bio: 'Test bio',
    emailVerified: true,
    gamesRated: 0,
    gamesInLibrary: 0,
    joinDate: '2024-03-20'
  };

  beforeEach(async () => {
    const profileServiceSpy = jasmine.createSpyObj('ProfileService', ['getProfile', 'updateProfile']);
    profileServiceSpy.getProfile.and.returnValue(of(mockProfile));
    profileServiceSpy.updateProfile.and.returnValue(of(mockProfile));

    await TestBed.configureTestingModule({
      imports: [
        CommonModule,
        ReactiveFormsModule,
        InputTextModule,
        TextareaModule,
        ButtonModule,
        MessageModule,
        ProgressSpinnerModule,
        ProfileComponent
      ],
      providers: [
        FormBuilder,
        { provide: ProfileService, useValue: profileServiceSpy }
      ]
    }).compileComponents();

    profileService = TestBed.inject(ProfileService) as jasmine.SpyObj<ProfileService>;
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ProfileComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Form Initialization', () => {
    it('should initialize the form with validators', () => {
      expect(component.profileForm.get('username')).toBeTruthy();
      expect(component.profileForm.get('profilePictureUrl')).toBeTruthy();
      expect(component.profileForm.get('bio')).toBeTruthy();
    });

    it('should load profile data on init', fakeAsync(() => {
      component.ngOnInit();
      tick();
      
      expect(profileService.getProfile).toHaveBeenCalled();
      expect(component.profile).toEqual(mockProfile);
      expect(component.profileForm.value).toEqual({
        username: mockProfile.username,
        profilePictureUrl: mockProfile.profilePictureUrl,
        bio: mockProfile.bio
      });
    }));

    it('should handle profile loading error', fakeAsync(() => {
      profileService.getProfile.and.returnValue(throwError(() => new Error('Failed to load')));
      
      component.ngOnInit();
      tick();

      expect(component.error).toBe('Failed to load profile data');
      expect(component.isLoading).toBeFalse();
    }));
  });

  describe('Form Validation', () => {
    it('should validate required username', () => {
      const usernameControl = component.profileForm.get('username');
      usernameControl?.setValue('');
      expect(usernameControl?.errors?.['required']).toBeTruthy();
    });

    it('should validate username minimum length', () => {
      const usernameControl = component.profileForm.get('username');
      usernameControl?.setValue('ab');
      expect(usernameControl?.errors?.['minlength']).toBeTruthy();
    });

    it('should validate username maximum length', () => {
      const usernameControl = component.profileForm.get('username');
      usernameControl?.setValue('a'.repeat(21));
      expect(usernameControl?.errors?.['maxlength']).toBeTruthy();
    });

    it('should validate username pattern', () => {
      const usernameControl = component.profileForm.get('username');
      usernameControl?.setValue('user@name');
      expect(usernameControl?.errors?.['pattern']).toBeTruthy();
    });

    it('should validate profile picture URL format', () => {
      const urlControl = component.profileForm.get('profilePictureUrl');
      urlControl?.setValue('invalid-url');
      expect(urlControl?.errors?.['pattern']).toBeTruthy();
    });

    it('should validate bio maximum length', () => {
      const bioControl = component.profileForm.get('bio');
      bioControl?.setValue('a'.repeat(501));
      expect(bioControl?.errors?.['maxlength']).toBeTruthy();
    });
  });

  describe('Edit Mode Toggle', () => {
    it('should toggle edit mode', () => {
      expect(component.isEditMode).toBeFalse();
      component.toggleEditMode();
      expect(component.isEditMode).toBeTrue();
    });

    it('should reset form when canceling edit mode', () => {
      component.isEditMode = true;
      component.profile = mockProfile;
      component.profileForm.patchValue({
        username: 'changedUsername',
        bio: 'changed bio'
      });

      component.toggleEditMode();

      expect(component.profileForm.value).toEqual({
        username: mockProfile.username,
        profilePictureUrl: mockProfile.profilePictureUrl,
        bio: mockProfile.bio
      });
    });
  });

  describe('Profile Update', () => {
    it('should not submit invalid form', () => {
      component.profileForm.get('username')?.setValue('');
      component.onSave();
      expect(profileService.updateProfile).not.toHaveBeenCalled();
    });

    it('should successfully update profile', fakeAsync(() => {
      const updateData = {
        username: 'newUsername',
        profilePictureUrl: 'https://example.com/new.png',
        bio: 'new bio'
      };
      
      component.profileForm.patchValue(updateData);
      component.onSave();
      tick();

      expect(profileService.updateProfile).toHaveBeenCalledWith(updateData);
      expect(component.successMessage).toBe('Profile updated successfully');
      expect(component.isEditMode).toBeFalse();
    }));

    it('should handle update error', fakeAsync(() => {
      const errorMessage = 'Update failed';
      profileService.updateProfile.and.returnValue(throwError(() => ({ error: { message: errorMessage } })));
      
      component.profileForm.patchValue({ username: 'newUsername' });
      component.onSave();
      tick();

      expect(component.error).toBe(errorMessage);
      expect(component.isLoading).toBeFalse();
    }));
  });

  describe('Error Messages', () => {
    it('should return appropriate error message for username', () => {
      const usernameControl = component.profileForm.get('username');
      usernameControl?.setValue('');
      usernameControl?.markAsTouched();
      
      expect(component.getErrorMessage('username')).toBe('Username is required');
    });

    it('should return appropriate error message for profile picture URL', () => {
      const urlControl = component.profileForm.get('profilePictureUrl');
      urlControl?.setValue('invalid-url');
      urlControl?.markAsTouched();
      
      expect(component.getErrorMessage('profilePictureUrl'))
        .toBe('Please enter a valid image URL (http/https ending in .png, .jpg, .jpeg, or .gif)');
    });

    it('should return empty string for untouched controls', () => {
      expect(component.getErrorMessage('username')).toBe('');
    });
  });

  describe('Image Error Handling', () => {
    it('should set default avatar on image error', () => {
      const imgElement = document.createElement('img');
      component.handleImageError({ target: imgElement } as unknown as Event);
      expect(imgElement.src).toContain('assets/images/default-avatar.png');
    });
  });
}); 
