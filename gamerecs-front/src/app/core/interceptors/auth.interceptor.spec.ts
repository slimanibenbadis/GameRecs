import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClient, HttpInterceptorFn, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';
import { DOCUMENT } from '@angular/common';
import { PLATFORM_ID } from '@angular/core';

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let httpMock: HttpTestingController;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;
  let mockWindow: { reload: jasmine.Spy };
  let mockDocument: { cookie: string; defaultView: { location: { reload: jasmine.Spy } } };

  function setupTestBed(platformId: string = 'browser') {
    const authServiceSpy = jasmine.createSpyObj('AuthService', ['getAuthToken', 'logout']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: DOCUMENT, useValue: mockDocument },
        { provide: PLATFORM_ID, useValue: platformId }
      ]
    });

    return {
      httpClient: TestBed.inject(HttpClient),
      httpMock: TestBed.inject(HttpTestingController),
      authService: TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>,
      router: TestBed.inject(Router) as jasmine.SpyObj<Router>
    };
  }

  beforeEach(() => {
    // Create mock window with reload spy
    mockWindow = { reload: jasmine.createSpy('reload') };
    
    // Create mock document
    mockDocument = {
      cookie: '',
      defaultView: {
        location: { reload: mockWindow.reload }
      }
    };
    
    const testBed = setupTestBed();
    httpClient = testBed.httpClient;
    httpMock = testBed.httpMock;
    authService = testBed.authService;
    router = testBed.router;
  });

  afterEach(() => {
    httpMock.verify();
    mockDocument.cookie = '';
  });

  it('should add Authorization header when token exists', fakeAsync(() => {
    const mockToken = 'mock-jwt-token';
    authService.getAuthToken.and.returnValue(mockToken);

    let completed = false;
    httpClient.get('/api/test').subscribe(() => {
      completed = true;
    });

    const httpRequest = httpMock.expectOne('/api/test');
    expect(httpRequest.request.headers.has('Authorization')).toBeTrue();
    expect(httpRequest.request.headers.get('Authorization')).toBe(`Bearer ${mockToken}`);
    
    httpRequest.flush({});
    tick();
    expect(completed).toBeTrue();
  }));

  it('should not add Authorization header when token is null', fakeAsync(() => {
    authService.getAuthToken.and.returnValue(null);

    let completed = false;
    httpClient.get('/api/test').subscribe(() => {
      completed = true;
    });

    const httpRequest = httpMock.expectOne('/api/test');
    expect(httpRequest.request.headers.has('Authorization')).toBeFalse();
    
    httpRequest.flush({});
    tick();
    expect(completed).toBeTrue();
  }));

  it('should add CSRF token for POST requests when cookie exists', fakeAsync(() => {
    mockDocument.cookie = 'XSRF-TOKEN=test-csrf-token';
    authService.getAuthToken.and.returnValue(null);

    let completed = false;
    httpClient.post('/api/test', {}).subscribe(() => {
      completed = true;
    });

    const httpRequest = httpMock.expectOne('/api/test');
    expect(httpRequest.request.headers.has('X-XSRF-TOKEN')).toBeTrue();
    expect(httpRequest.request.headers.get('X-XSRF-TOKEN')).toBe('test-csrf-token');
    
    httpRequest.flush({});
    tick();
    expect(completed).toBeTrue();
  }));

  it('should not add CSRF token for GET requests even when cookie exists', fakeAsync(() => {
    mockDocument.cookie = 'XSRF-TOKEN=test-csrf-token';
    authService.getAuthToken.and.returnValue(null);

    let completed = false;
    httpClient.get('/api/test').subscribe(() => {
      completed = true;
    });

    const httpRequest = httpMock.expectOne('/api/test');
    expect(httpRequest.request.headers.has('X-XSRF-TOKEN')).toBeFalse();
    
    httpRequest.flush({});
    tick();
    expect(completed).toBeTrue();
  }));

  it('should handle CSRF validation failure', fakeAsync(() => {
    const csrfError = {
      status: 403,
      error: { message: 'Invalid CSRF token' }
    };
    
    authService.getAuthToken.and.returnValue('mock-token');

    let errorCaught = false;
    httpClient.post('/api/test', {}).subscribe({
      error: (error) => {
        expect(error.status).toBe(403);
        expect(mockWindow.reload).toHaveBeenCalled();
        errorCaught = true;
      }
    });

    const httpRequest = httpMock.expectOne('/api/test');
    httpRequest.flush(csrfError.error, { status: 403, statusText: 'Forbidden' });
    tick();
    expect(errorCaught).toBeTrue();
  }));

  it('should handle 401 response by logging out and redirecting to login', fakeAsync(() => {
    authService.getAuthToken.and.returnValue('mock-token');

    let errorCaught = false;
    httpClient.get('/api/test').subscribe({
      error: (error) => {
        expect(error.status).toBe(401);
        expect(authService.logout).toHaveBeenCalled();
        expect(router.navigate).toHaveBeenCalledWith(['/auth/login']);
        errorCaught = true;
      }
    });

    const httpRequest = httpMock.expectOne('/api/test');
    httpRequest.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    tick();
    expect(errorCaught).toBeTrue();
  }));

  it('should pass through non-401 errors without logging out', fakeAsync(() => {
    authService.getAuthToken.and.returnValue('mock-token');

    let errorCaught = false;
    httpClient.get('/api/test').subscribe({
      error: (error) => {
        expect(error.status).toBe(500);
        expect(authService.logout).not.toHaveBeenCalled();
        expect(router.navigate).not.toHaveBeenCalled();
        errorCaught = true;
      }
    });

    const httpRequest = httpMock.expectOne('/api/test');
    httpRequest.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });
    tick();
    expect(errorCaught).toBeTrue();
  }));

  describe('Server-side rendering', () => {
    beforeEach(() => {
      const testBed = setupTestBed('server');
      httpClient = testBed.httpClient;
      httpMock = testBed.httpMock;
      authService = testBed.authService;
      router = testBed.router;
    });

    it('should not add CSRF token in server environment', fakeAsync(() => {
      mockDocument.cookie = 'XSRF-TOKEN=test-csrf-token';
      authService.getAuthToken.and.returnValue(null);

      let completed = false;
      httpClient.post('/api/test', {}).subscribe(() => {
        completed = true;
      });

      const httpRequest = httpMock.expectOne('/api/test');
      expect(httpRequest.request.headers.has('X-XSRF-TOKEN')).toBeFalse();
      
      httpRequest.flush({});
      tick();
      expect(completed).toBeTrue();
    }));
  });
}); 
