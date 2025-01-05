import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService, IUserRegistration, IRegistrationResponse, IApiError, IVerificationResponse } from './auth.service';

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

  describe('verifyEmail', () => {
    const mockToken = 'valid-token';

    it('should successfully verify email', () => {
      const mockResponse: IVerificationResponse = {
        verified: true,
        message: 'Email verified successfully'
      };

      service.verifyEmail(mockToken).subscribe(response => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`/api/users/verify?token=${mockToken}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should handle verification failure', () => {
      const mockResponse: IVerificationResponse = {
        verified: false,
        message: 'Token expired'
      };

      service.verifyEmail(mockToken).subscribe(response => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`/api/users/verify?token=${mockToken}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should handle server error', () => {
      const apiError: IApiError = {
        timestamp: new Date().toISOString(),
        status: 400,
        message: 'Invalid token'
      };

      service.verifyEmail(mockToken).subscribe({
        error: error => {
          expect(error.message).toBe('Invalid token');
        }
      });

      const req = httpMock.expectOne(`/api/users/verify?token=${mockToken}`);
      req.flush(apiError, { status: 400, statusText: 'Bad Request' });
    });
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

    it('should handle error when parsing error response fails', () => {
      service.registerUser(mockUser).subscribe({
        error: error => {
          expect(error.message).toBe('An error occurred during registration');
        }
      });

      const req = httpMock.expectOne('/api/users/register');
      req.flush(null, { status: 500, statusText: 'Internal Server Error' });
    });
  });

  describe('login', () => {
    const mockLoginRequest = {
      username: 'testuser',
      password: 'password123',
      rememberMe: false
    };

    it('should handle malformed error response', () => {
      service.login(mockLoginRequest).subscribe({
        error: error => {
          expect(error.message).toBe('An error occurred during login');
        }
      });

      const req = httpMock.expectOne('/api/users/login');
      req.flush('Invalid response', { status: 500, statusText: 'Internal Server Error' });
    });

    it('should handle unauthorized error', () => {
      service.login(mockLoginRequest).subscribe({
        error: error => {
          expect(error.message).toBe('Invalid username or password');
        }
      });

      const req = httpMock.expectOne('/api/users/login');
      req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    });

    it('should handle server error with message', () => {
      const apiError: IApiError = {
        timestamp: new Date().toISOString(),
        status: 500,
        message: 'Server error occurred'
      };

      service.login(mockLoginRequest).subscribe({
        error: error => {
          expect(error.message).toBe('Server error occurred');
        }
      });

      const req = httpMock.expectOne('/api/users/login');
      req.flush(apiError, { status: 500, statusText: 'Internal Server Error' });
    });
  });
}); 
