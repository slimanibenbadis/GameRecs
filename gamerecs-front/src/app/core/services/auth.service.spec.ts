/// <reference types="jasmine" />

import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService, IUserRegistration, IRegistrationResponse, IApiError } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('registerUser', () => {
    const mockUser: IUserRegistration = {
      username: 'testuser',
      email: 'test@example.com',
      password: 'StrongPass123',
      bio: 'Test bio',
      profilePictureUrl: 'https://example.com/image.jpg'
    };

    const mockResponse: IRegistrationResponse = {
      userId: '123',
      username: 'testuser',
      email: 'test@example.com',
      bio: 'Test bio',
      profilePictureUrl: 'https://example.com/image.jpg',
      joinDate: new Date().toISOString()
    };

    it('should successfully register a user', () => {
      service.registerUser(mockUser).subscribe(response => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne('/api/users/register');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(mockUser);
      req.flush(mockResponse);
    });

    it('should handle client-side error', () => {
      const errorEvent = new ErrorEvent('Network error', {
        message: 'No internet connection'
      });

      service.registerUser(mockUser).subscribe({
        error: error => {
          expect(error.message).toBe('No internet connection');
        }
      });

      const req = httpMock.expectOne('/api/users/register');
      req.error(errorEvent);
    });

    it('should handle server-side error with field errors', () => {
      const apiError: IApiError = {
        timestamp: new Date().toISOString(),
        status: 400,
        message: 'Validation failed',
        errors: {
          email: 'Email already exists',
          username: 'Username already taken'
        }
      };

      service.registerUser(mockUser).subscribe({
        error: error => {
          expect(error.message).toBe('Email already exists. Username already taken');
        }
      });

      const req = httpMock.expectOne('/api/users/register');
      req.flush(apiError, { status: 400, statusText: 'Bad Request' });
    });

    it('should handle server-side error without field errors', () => {
      const apiError: IApiError = {
        timestamp: new Date().toISOString(),
        status: 500,
        message: 'Internal server error'
      };

      service.registerUser(mockUser).subscribe({
        error: error => {
          expect(error.message).toBe('Internal server error');
        }
      });

      const req = httpMock.expectOne('/api/users/register');
      req.flush(apiError, { status: 500, statusText: 'Internal Server Error' });
    });

    it('should handle malformed error response', () => {
      service.registerUser(mockUser).subscribe({
        error: error => {
          expect(error.message).toBe('An error occurred during registration');
        }
      });

      const req = httpMock.expectOne('/api/users/register');
      req.flush('Invalid response', { status: 500, statusText: 'Internal Server Error' });
    });
  });
}); 