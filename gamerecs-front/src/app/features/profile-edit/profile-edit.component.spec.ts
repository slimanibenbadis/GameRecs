import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { Router } from '@angular/router';

// PrimeNG Imports
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

import { ProfileEditComponent } from './profile-edit.component';
import { ProfileService } from '../../core/services/profile.service';

describe('ProfileEditComponent', () => {
  let component: ProfileEditComponent;
  let fixture: ComponentFixture<ProfileEditComponent>;
  let profileService: jasmine.SpyObj<ProfileService>;
  let router: Router;

  const mockProfileData = {
    username: 'testuser',
    email: 'test@example.com',
    profilePictureUrl: 'https://example.com/pic.jpg',
    bio: 'Test bio',
    emailVerified: true,
    gamesRated: 0,
    gamesInLibrary: 0,
    joinDate: '2024-01-01'
  };

  beforeEach(async () => {
    const profileServiceSpy = jasmine.createSpyObj('ProfileService', ['getProfile', 'updateProfile']);
    profileServiceSpy.getProfile.and.returnValue(of(mockProfileData));
    profileServiceSpy.updateProfile.and.returnValue(of(mockProfileData));

    await TestBed.configureTestingModule({
      imports: [
        ProfileEditComponent,
        HttpClientTestingModule,
        ReactiveFormsModule,
        NoopAnimationsModule,
        RouterTestingModule,
        ButtonModule,
        InputTextModule,
        TextareaModule,
        MessageModule,
        ProgressSpinnerModule
      ],
      providers: [
        { provide: ProfileService, useValue: profileServiceSpy }
      ]
    }).compileComponents();

    profileService = TestBed.inject(ProfileService) as jasmine.SpyObj<ProfileService>;
    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(ProfileEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Initial Load', () => {
    it('should load profile data on init', () => {
      expect(profileService.getProfile).toHaveBeenCalled();
      expect(component.profileForm.get('username')?.value).toBe('testuser');
      expect(component.profileForm.get('bio')?.value).toBe('Test bio');
      expect(component.profileForm.get('profilePictureUrl')?.value).toBe('https://example.com/pic.jpg');
    });

    it('should handle profile load error', () => {
      profileService.getProfile.and.returnValue(throwError(() => new Error('Failed to load')));
      component.ngOnInit();
      expect(component.error).toBe('Failed to load profile data');
      expect(component.isLoading).toBeFalse();
    });
  });

  describe('Form Validation', () => {
    it('should validate username field', () => {
      const usernameControl = component.profileForm.get('username');
      
      usernameControl?.setValue('');
      usernameControl?.markAsTouched();
      expect(usernameControl?.errors?.['required']).toBeTruthy();
      expect(component.getErrorMessage('username')).toBe('Username is required');
      
      usernameControl?.setValue('ab');
      usernameControl?.markAsTouched();
      expect(usernameControl?.errors?.['minlength']).toBeTruthy();
      expect(component.getErrorMessage('username')).toBe('Username must be at least 3 characters');
      
      usernameControl?.setValue('test@user');
      usernameControl?.markAsTouched();
      expect(usernameControl?.errors?.['pattern']).toBeTruthy();
      expect(component.getErrorMessage('username')).toBe('Username can only contain letters, numbers, underscores, and hyphens');
      
      usernameControl?.setValue('validusername');
      usernameControl?.markAsTouched();
      expect(usernameControl?.errors).toBeNull();
      expect(component.getErrorMessage('username')).toBe('');
    });

    it('should validate profilePictureUrl field', () => {
      const urlControl = component.profileForm.get('profilePictureUrl');
      
      urlControl?.setValue('invalid-url');
      urlControl?.markAsTouched();
      expect(urlControl?.errors?.['pattern']).toBeTruthy();
      expect(component.getErrorMessage('profilePictureUrl')).toBe('Please enter a valid image URL (http/https ending in .png, .jpg, .jpeg, or .gif)');
      
      urlControl?.setValue('https://example.com/image.jpg');
      urlControl?.markAsTouched();
      expect(urlControl?.errors).toBeNull();
      expect(component.getErrorMessage('profilePictureUrl')).toBe('');
    });

    it('should validate bio field', () => {
      const bioControl = component.profileForm.get('bio');
      
      bioControl?.setValue('a'.repeat(501));
      bioControl?.markAsTouched();
      expect(bioControl?.errors?.['maxlength']).toBeTruthy();
      expect(component.getErrorMessage('bio')).toBe('Bio cannot exceed 500 characters');
      
      bioControl?.setValue('valid bio');
      bioControl?.markAsTouched();
      expect(bioControl?.errors).toBeNull();
      expect(component.getErrorMessage('bio')).toBe('');
    });
  });

  describe('Form Submission', () => {
    it('should not submit if form is invalid', () => {
      component.profileForm.get('username')?.setValue('');
      component.onSave();
      expect(profileService.updateProfile).not.toHaveBeenCalled();
    });

    it('should submit valid form data', fakeAsync(() => {
      const spyRouter = spyOn(router, 'navigate');
      const updateData = {
        username: 'newusername',
        profilePictureUrl: 'https://example.com/new.jpg',
        bio: 'New bio'
      };
      
      component.profileForm.patchValue(updateData);
      component.onSave();

      expect(profileService.updateProfile).toHaveBeenCalledWith(updateData);
      expect(component.isLoading).toBeFalse();
      expect(component.successMessage).toBe('Profile updated successfully');
      
      tick(1500);
      expect(spyRouter).toHaveBeenCalledWith(['/profile']);
    }));

    it('should handle update error', () => {
      const errorMessage = 'Update failed';
      profileService.updateProfile.and.returnValue(throwError(() => ({ error: { message: errorMessage } })));
      
      component.profileForm.patchValue({
        username: 'newusername',
        bio: 'New bio'
      });
      component.onSave();

      expect(component.error).toBe(errorMessage);
      expect(component.isLoading).toBeFalse();
      expect(component.successMessage).toBeUndefined();
    });
  });
});
