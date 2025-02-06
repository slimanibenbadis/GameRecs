import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { By } from '@angular/platform-browser';
import { HttpErrorResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageModule } from 'primeng/message';
import { ButtonModule } from 'primeng/button';
import { InputTextarea } from 'primeng/inputtextarea';

import { ProfileViewComponent } from './profile-view.component';
import { ProfileService } from '../../core/services/profile.service';
import { ProfileResponseDto } from '../../models/profile-response.dto';

describe('ProfileViewComponent', () => {
  let component: ProfileViewComponent;
  let fixture: ComponentFixture<ProfileViewComponent>;
  let profileServiceSpy: jasmine.SpyObj<ProfileService>;
  let mockProfile: ProfileResponseDto;

  beforeEach(async () => {
    profileServiceSpy = jasmine.createSpyObj('ProfileService', ['getProfile', 'updateBio']);
    
    mockProfile = {
      username: 'testUser',
      email: 'test@example.com',
      profilePictureUrl: 'http://example.com/avatar.jpg',
      bio: 'Test bio',
      emailVerified: true,
      gamesRated: 10,
      gamesInLibrary: 20,
      joinDate: '2024-01-01'
    };

    await TestBed.configureTestingModule({
      imports: [
        CommonModule,
        FormsModule,
        NoopAnimationsModule,
        ProgressSpinnerModule,
        MessageModule,
        ButtonModule,
        InputTextarea
      ],
      providers: [
        { provide: ProfileService, useValue: profileServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProfileViewComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show loading state initially', () => {
    profileServiceSpy.getProfile.and.returnValue(of(mockProfile));
    expect(component.isLoading).toBeTrue();
    fixture.detectChanges();
  });

  it('should load profile data on init', () => {
    profileServiceSpy.getProfile.and.returnValue(of(mockProfile));
    fixture.detectChanges();

    expect(component.profile).toEqual(mockProfile);
    expect(component.isLoading).toBeFalse();
    expect(component.error).toBeUndefined();
  });

  it('should handle profile load error', () => {
    const errorResponse = new HttpErrorResponse({
      error: 'Test error',
      status: 404,
      statusText: 'Not Found'
    });
    profileServiceSpy.getProfile.and.returnValue(throwError(() => errorResponse));
    
    fixture.detectChanges();

    expect(component.error).toBe('Failed to load profile. Please try again.');
    expect(component.isLoading).toBeFalse();
    expect(component.profile).toBeUndefined();
  });

  describe('Bio editing', () => {
    beforeEach(() => {
      profileServiceSpy.getProfile.and.returnValue(of(mockProfile));
      fixture.detectChanges();
    });

    it('should enter edit mode when startEditing is called', () => {
      component.startEditing();
      expect(component.isEditing).toBeTrue();
      expect(component.editedBio).toBe(mockProfile.bio);
    });

    it('should exit edit mode and revert changes when cancelEditing is called', () => {
      component.editedBio = 'New bio text';
      component.isEditing = true;
      
      component.cancelEditing();
      
      expect(component.isEditing).toBeFalse();
      expect(component.editedBio).toBe(mockProfile.bio);
    });

    it('should update bio successfully', () => {
      const updatedProfile = { ...mockProfile, bio: 'Updated bio' };
      profileServiceSpy.updateBio.and.returnValue(of(updatedProfile));
      
      component.isEditing = true;
      component.editedBio = 'Updated bio';
      component.saveBio();

      expect(profileServiceSpy.updateBio).toHaveBeenCalledWith('Updated bio');
      expect(component.profile).toEqual(updatedProfile);
      expect(component.isEditing).toBeFalse();
      expect(component.isLoading).toBeFalse();
    });

    it('should handle bio update error', () => {
      const errorResponse = new HttpErrorResponse({
        error: 'Test error',
        status: 400,
        statusText: 'Bad Request'
      });
      profileServiceSpy.updateBio.and.returnValue(throwError(() => errorResponse));
      
      component.isEditing = true;
      component.editedBio = 'Updated bio';
      component.saveBio();

      expect(component.error).toBe('Failed to update bio. Please try again.');
      expect(component.isLoading).toBeFalse();
    });
  });

  it('should handle profile picture load error', () => {
    const imgElement = document.createElement('img');
    imgElement.src = 'invalid-url';
    const event = { target: imgElement } as unknown as Event;
    
    component.handleImageError(event);
    
    expect(imgElement.src).toContain('assets/images/default-avatar.png');
  });
});
