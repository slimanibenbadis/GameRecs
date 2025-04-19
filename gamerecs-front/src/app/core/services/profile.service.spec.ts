import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ProfileService } from './profile.service';
import { ProfileResponseDto, UpdateProfileRequest } from '../../models/profile-response.dto';

describe('ProfileService', () => {
  let service: ProfileService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ProfileService]
    });
    service = TestBed.inject(ProfileService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should retrieve user profile', () => {
    const mockProfile: ProfileResponseDto = {
      username: 'testuser',
      email: 'test@example.com',
      profilePictureUrl: 'http://example.com/profile.jpg',
      bio: 'Test bio',
      emailVerified: true,
      gamesRated: 10,
      gamesInLibrary: 25,
      joinDate: '2023-01-01'
    };

    service.getProfile().subscribe(profile => {
      expect(profile).toEqual(mockProfile);
    });

    const req = httpMock.expectOne('/api/users/profile');
    expect(req.request.method).toBe('GET');
    req.flush(mockProfile);
  });

  it('should update user bio', () => {
    const newBio = 'My updated bio';
    const mockResponse: ProfileResponseDto = {
      username: 'testuser',
      email: 'test@example.com',
      profilePictureUrl: 'http://example.com/profile.jpg',
      bio: newBio,
      emailVerified: true,
      gamesRated: 10,
      gamesInLibrary: 25,
      joinDate: '2023-01-01'
    };

    service.updateBio(newBio).subscribe(profile => {
      expect(profile.bio).toBe(newBio);
      expect(profile).toEqual(mockResponse);
    });

    const req = httpMock.expectOne('/api/users/profile/bio');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ bio: newBio });
    req.flush(mockResponse);
  });

  it('should update user profile', () => {
    const updateRequest: UpdateProfileRequest = {
      username: 'newusername',
      profilePictureUrl: 'http://example.com/new-profile.jpg',
      bio: 'New bio information'
    };

    const mockResponse: ProfileResponseDto = {
      username: 'newusername',
      email: 'test@example.com',
      profilePictureUrl: 'http://example.com/new-profile.jpg',
      bio: 'New bio information',
      emailVerified: true,
      gamesRated: 10,
      gamesInLibrary: 25,
      joinDate: '2023-01-01'
    };

    service.updateProfile(updateRequest).subscribe(profile => {
      expect(profile).toEqual(mockResponse);
    });

    const req = httpMock.expectOne('/api/users/profile');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(updateRequest);
    req.flush(mockResponse);
  });

  it('should handle errors when retrieving profile', () => {
    service.getProfile().subscribe({
      next: () => fail('Should have failed with an error'),
      error: error => {
        expect(error.status).toBe(500);
      }
    });

    const req = httpMock.expectOne('/api/users/profile');
    expect(req.request.method).toBe('GET');
    req.flush('Internal server error', { status: 500, statusText: 'Server Error' });
  });

  it('should handle errors when updating bio', () => {
    service.updateBio('New bio').subscribe({
      next: () => fail('Should have failed with an error'),
      error: error => {
        expect(error.status).toBe(400);
      }
    });

    const req = httpMock.expectOne('/api/users/profile/bio');
    expect(req.request.method).toBe('PATCH');
    req.flush('Bad request', { status: 400, statusText: 'Bad Request' });
  });

  it('should handle errors when updating profile', () => {
    const updateRequest: UpdateProfileRequest = {
      username: 'newusername',
      profilePictureUrl: 'http://example.com/new-profile.jpg',
      bio: 'New bio information'
    };

    service.updateProfile(updateRequest).subscribe({
      next: () => fail('Should have failed with an error'),
      error: error => {
        expect(error.status).toBe(404);
      }
    });

    const req = httpMock.expectOne('/api/users/profile');
    expect(req.request.method).toBe('PUT');
    req.flush('Not found', { status: 404, statusText: 'Not Found' });
  });
}); 
