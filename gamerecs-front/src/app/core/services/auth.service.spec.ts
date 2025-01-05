import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService, IUserRegistration, IRegistrationResponse, IApiError, IVerificationResponse, ILoginResponse, ILoginRequest } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  const mockLoginResponse: ILoginResponse = {
    token: 'mock-jwt-token',
    username: 'testuser',
    email: 'test@example.com',
    emailVerified: true
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService]
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    httpMock.verify();
  });

  describe('basic service functionality', () => {
    beforeEach(() => {
      localStorage.clear();
      sessionStorage.clear();
      service = TestBed.inject(AuthService);
    });

    it('should be created', () => {
      expect(service).toBeTruthy();
    });
  });

  describe('authentication state', () => {
    it('should initialize with null when no token is stored', () => {
      localStorage.clear();
      sessionStorage.clear();
      service = TestBed.inject(AuthService);

      expect(service.currentUserValue).toBeNull();
      service.isAuthenticated$.subscribe(isAuth => {
        expect(isAuth).toBeFalse();
      });
    });

    it('should initialize with stored token from localStorage', () => {
      localStorage.clear();
      sessionStorage.clear();
      
      // Setup stored auth data before creating service
      localStorage.setItem('auth_token', mockLoginResponse.token);
      localStorage.setItem('current_user', JSON.stringify(mockLoginResponse));
      
      service = TestBed.inject(AuthService);
      
      expect(service.currentUserValue).toEqual(mockLoginResponse);
      service.isAuthenticated$.subscribe(isAuth => {
        expect(isAuth).toBeTrue();
      });
    });

    it('should initialize with stored token from sessionStorage', () => {
      localStorage.clear();
      sessionStorage.clear();
      
      // Setup stored auth data before creating service
      sessionStorage.setItem('auth_token', mockLoginResponse.token);
      sessionStorage.setItem('current_user', JSON.stringify(mockLoginResponse));
      
      service = TestBed.inject(AuthService);
      
      expect(service.currentUserValue).toEqual(mockLoginResponse);
      service.isAuthenticated$.subscribe(isAuth => {
        expect(isAuth).toBeTrue();
      });
    });

    it('should handle corrupted stored user data', () => {
      localStorage.clear();
      sessionStorage.clear();
      
      // Setup corrupted auth data before creating service
      localStorage.setItem('auth_token', mockLoginResponse.token);
      localStorage.setItem('current_user', 'invalid-json');
      
      service = TestBed.inject(AuthService);
      
      expect(service.currentUserValue).toBeNull();
      service.isAuthenticated$.subscribe(isAuth => {
        expect(isAuth).toBeTrue(); // Still true because token exists
      });
    });
  });

  describe('API operations', () => {
    beforeEach(() => {
      localStorage.clear();
      sessionStorage.clear();
      service = TestBed.inject(AuthService);
    });

    describe('verifyEmail', () => {
      const mockToken = 'valid-token';

      it('should successfully verify email', () => {
        const mockResponse: IVerificationResponse = {
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
          message: 'Email verification failed'
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
    });

    describe('login', () => {
      const mockLoginRequest: ILoginRequest = {
        username: 'testuser',
        password: 'password123',
        rememberMe: false
      };

      it('should successfully login and store session data', () => {
        service.login(mockLoginRequest).subscribe(response => {
          expect(response).toEqual(mockLoginResponse);
          expect(service.getAuthToken()).toBe(mockLoginResponse.token);
          expect(service.currentUserValue).toEqual(mockLoginResponse);
        });

        const req = httpMock.expectOne('/api/auth/login');
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual(mockLoginRequest);
        req.flush(mockLoginResponse);

        // Verify authentication state
        service.isAuthenticated$.subscribe(isAuth => {
          expect(isAuth).toBe(true);
        });
      });

      it('should store auth data in localStorage when rememberMe is true', () => {
        const rememberMeRequest = { ...mockLoginRequest, rememberMe: true };

        service.login(rememberMeRequest).subscribe(() => {
          expect(localStorage.getItem('auth_token')).toBe(mockLoginResponse.token);
          expect(localStorage.getItem('current_user')).toBe(JSON.stringify(mockLoginResponse));
          expect(sessionStorage.getItem('auth_token')).toBeNull();
        });

        const req = httpMock.expectOne('/api/auth/login');
        req.flush(mockLoginResponse);
      });

      it('should store auth data in sessionStorage when rememberMe is false', () => {
        service.login(mockLoginRequest).subscribe(() => {
          expect(sessionStorage.getItem('auth_token')).toBe(mockLoginResponse.token);
          expect(sessionStorage.getItem('current_user')).toBe(JSON.stringify(mockLoginResponse));
          expect(localStorage.getItem('auth_token')).toBeNull();
        });

        const req = httpMock.expectOne('/api/auth/login');
        req.flush(mockLoginResponse);
      });

      it('should handle unauthorized error', () => {
        const apiError: IApiError = {
          timestamp: new Date().toISOString(),
          status: 401,
          message: 'Invalid credentials'
        };

        service.login(mockLoginRequest).subscribe({
          error: error => {
            expect(error.message).toBe('Invalid username or password');
          }
        });

        const req = httpMock.expectOne('/api/auth/login');
        req.flush(apiError, { status: 401, statusText: 'Unauthorized' });
      });
    });

    describe('logout', () => {
      beforeEach(() => {
        // Setup initial auth state
        localStorage.setItem('auth_token', mockLoginResponse.token);
        localStorage.setItem('current_user', JSON.stringify(mockLoginResponse));
        sessionStorage.setItem('auth_token', mockLoginResponse.token);
        sessionStorage.setItem('current_user', JSON.stringify(mockLoginResponse));
        service = TestBed.inject(AuthService);
      });

      it('should clear all auth data and update state', () => {
        service.logout();

        expect(localStorage.getItem('auth_token')).toBeNull();
        expect(localStorage.getItem('current_user')).toBeNull();
        expect(sessionStorage.getItem('auth_token')).toBeNull();
        expect(sessionStorage.getItem('current_user')).toBeNull();

        service.isAuthenticated$.subscribe(isAuth => {
          expect(isAuth).toBe(false);
        });

        service.currentUser$.subscribe(user => {
          expect(user).toBeNull();
        });
      });
    });
  });
}); 
