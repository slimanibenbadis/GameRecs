import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Router, ActivatedRoute } from '@angular/router';
import { GoogleCallbackComponent } from './google-callback.component';
import { AuthService, IGoogleAuthResponse } from '../../services/auth.service';
import { of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';
import { HttpErrorResponse } from '@angular/common/http';
import { By } from '@angular/platform-browser';

describe('GoogleCallbackComponent', () => {
  let component: GoogleCallbackComponent;
  let fixture: ComponentFixture<GoogleCallbackComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let activatedRouteSpy: { queryParams: any };

  const mockGoogleResponse: IGoogleAuthResponse = {
    token: 'mock-token',
    username: 'testuser',
    email: 'test@example.com',
    emailVerified: true,
    googleId: 'mock-google-id'
  };

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['handleGoogleToken', 'handleGoogleCallback']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    activatedRouteSpy = {
      queryParams: of({})
    };

    await TestBed.configureTestingModule({
      imports: [GoogleCallbackComponent],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: activatedRouteSpy }
      ]
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(GoogleCallbackComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should handle JWT token in URL params', fakeAsync(() => {
    // Arrange
    activatedRouteSpy.queryParams = of({ token: 'valid-jwt-token' });
    
    // Act
    fixture.detectChanges();
    tick();

    // Assert
    expect(authServiceSpy.handleGoogleToken).toHaveBeenCalledWith('valid-jwt-token');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/health']);
    expect(component.isLoading).toBeFalse();
    expect(component.error).toBeNull();
  }));

  it('should handle missing token and code', fakeAsync(() => {
    // Arrange
    activatedRouteSpy.queryParams = of({});
    
    // Act
    fixture.detectChanges();
    tick();

    // Assert
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/auth/login']);
    expect(component.error).toBe('Authentication failed. Please try again.');
    expect(component.isLoading).toBeTrue();
  }));

  it('should handle Google callback code successfully', fakeAsync(() => {
    // Arrange
    activatedRouteSpy.queryParams = of({ code: 'valid-auth-code' });
    authServiceSpy.handleGoogleCallback.and.returnValue(of(mockGoogleResponse));
    
    // Act
    fixture.detectChanges();
    tick();

    // Assert
    expect(authServiceSpy.handleGoogleCallback).toHaveBeenCalledWith('valid-auth-code');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/health']);
    expect(component.isLoading).toBeFalse();
    expect(component.error).toBeNull();
  }));

  it('should handle Google callback error', fakeAsync(() => {
    // Arrange
    activatedRouteSpy.queryParams = of({ code: 'invalid-auth-code' });
    authServiceSpy.handleGoogleCallback.and.returnValue(throwError(() => new Error('Auth Error')));
    
    // Act
    fixture.detectChanges();
    tick();

    // Assert
    expect(authServiceSpy.handleGoogleCallback).toHaveBeenCalledWith('invalid-auth-code');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/auth/login']);
    expect(component.isLoading).toBeFalse();
    expect(component.error).toBe('Authentication failed. Please try again.');
  }));

  it('should handle Google callback failure', fakeAsync(() => {
    // Arrange
    activatedRouteSpy.queryParams = of({ code: 'invalid-auth-code' });
    authServiceSpy.handleGoogleCallback.and.returnValue(throwError(() => new Error('Invalid code')));
    
    // Act
    fixture.detectChanges();
    tick();

    // Assert
    expect(authServiceSpy.handleGoogleCallback).toHaveBeenCalledWith('invalid-auth-code');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/auth/login']);
    expect(component.isLoading).toBeFalse();
    expect(component.error).toBe('Authentication failed. Please try again.');
  }));

  it('should show loading state during callback processing', fakeAsync(() => {
    // Arrange
    activatedRouteSpy.queryParams = of({ code: 'valid-auth-code' });
    let resolveCallback: any;
    const callbackPromise = new Promise(resolve => resolveCallback = resolve);
    authServiceSpy.handleGoogleCallback.and.returnValue(of(mockGoogleResponse).pipe(
      delay(100)
    ));
    
    // Act
    fixture.detectChanges();
    
    // Assert - Check loading state
    expect(component.isLoading).toBeTrue();
    const loadingElement = fixture.debugElement.query(By.css('.animate-spin'));
    expect(loadingElement).toBeTruthy();
    
    // Complete the callback
    tick(100);
    fixture.detectChanges();
    
    expect(component.isLoading).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/health']);
  }));

  it('should handle network errors during callback', fakeAsync(() => {
    // Arrange
    activatedRouteSpy.queryParams = of({ code: 'valid-auth-code' });
    authServiceSpy.handleGoogleCallback.and.returnValue(throwError(() => new HttpErrorResponse({
      error: new ErrorEvent('Network error'),
      status: 0
    })));
    
    // Act
    fixture.detectChanges();
    tick();

    // Assert
    expect(component.error).toBe('Authentication failed. Please try again.');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/auth/login']);
    expect(component.isLoading).toBeFalse();
  }));

  it('should handle server errors during callback', fakeAsync(() => {
    // Arrange
    activatedRouteSpy.queryParams = of({ code: 'valid-auth-code' });
    authServiceSpy.handleGoogleCallback.and.returnValue(throwError(() => new HttpErrorResponse({
      error: { message: 'Internal server error' },
      status: 500
    })));
    
    // Act
    fixture.detectChanges();
    tick();

    // Assert
    expect(component.error).toBe('Authentication failed. Please try again.');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/auth/login']);
    expect(component.isLoading).toBeFalse();
  }));
}); 
