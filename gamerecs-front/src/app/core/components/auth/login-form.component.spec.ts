import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { LoginFormComponent } from './login-form.component';
import { AuthService, ILoginResponse } from '../../services/auth.service';
import { MessageService } from 'primeng/api';
import { ReactiveFormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { Router } from '@angular/router';
import { By } from '@angular/platform-browser';

// PrimeNG Imports
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { CheckboxModule } from 'primeng/checkbox';

describe('LoginFormComponent', () => {
  let component: LoginFormComponent;
  let fixture: ComponentFixture<LoginFormComponent>;
  let authService: jasmine.SpyObj<AuthService>;
  let messageService: jasmine.SpyObj<MessageService>;
  let router: Router;

  const mockLoginResponse: ILoginResponse = {
    token: 'mock-token',
    username: 'testuser',
    email: 'test@example.com',
    emailVerified: true
  };

  beforeEach(async () => {
    const authServiceSpy = jasmine.createSpyObj('AuthService', ['login']);
    const messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);

    await TestBed.configureTestingModule({
      imports: [
        ReactiveFormsModule,
        InputTextModule,
        PasswordModule,
        ButtonModule,
        ToastModule,
        CheckboxModule,
        LoginFormComponent,
        NoopAnimationsModule,
        RouterTestingModule
      ],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: MessageService, useValue: messageServiceSpy }
      ]
    }).compileComponents();

    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    messageService = TestBed.inject(MessageService) as jasmine.SpyObj<MessageService>;
    router = TestBed.inject(Router);
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(LoginFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Form Validation', () => {
    it('should initialize with invalid form', () => {
      expect(component.loginForm.valid).toBeFalsy();
    });

    it('should validate required fields', () => {
      const form = component.loginForm;
      expect(form.get('username')?.errors?.['required']).toBeTruthy();
      expect(form.get('password')?.errors?.['required']).toBeTruthy();
    });

    it('should validate username format', () => {
      const usernameControl = component.loginForm.get('username');
      usernameControl?.setValue('u$er');
      expect(usernameControl?.errors?.['pattern']).toBeTruthy();

      usernameControl?.setValue('validuser123');
      expect(usernameControl?.errors).toBeNull();
    });

    it('should validate username length', () => {
      const usernameControl = component.loginForm.get('username');
      usernameControl?.setValue('ab');
      expect(usernameControl?.errors?.['minlength']).toBeTruthy();

      usernameControl?.setValue('a'.repeat(21));
      expect(usernameControl?.errors?.['maxlength']).toBeTruthy();

      usernameControl?.setValue('validuser');
      expect(usernameControl?.errors).toBeNull();
    });

    it('should validate password format', () => {
      const passwordControl = component.loginForm.get('password');
      passwordControl?.setValue('weak');
      expect(passwordControl?.errors?.['pattern']).toBeTruthy();

      passwordControl?.setValue('StrongPass123');
      expect(passwordControl?.errors).toBeNull();
    });

    it('should validate password length', () => {
      const passwordControl = component.loginForm.get('password');
      passwordControl?.setValue('Weak1');
      expect(passwordControl?.errors?.['minlength']).toBeTruthy();

      passwordControl?.setValue('StrongPass123');
      expect(passwordControl?.errors).toBeNull();
    });
  });

  describe('Form Submission', () => {
    beforeEach(() => {
      // Set valid form values
      component.loginForm.setValue({
        username: 'testuser',
        password: 'StrongPass123',
        rememberMe: false
      });
    });

    it('should call login when form is valid', fakeAsync(() => {
      const messageService = fixture.debugElement.injector.get(MessageService);
      spyOn(messageService, 'add');
      spyOn(router, 'navigate');
      authService.login.and.returnValue(of(mockLoginResponse));
      
      component.onSubmit();
      fixture.detectChanges();
      tick();
      fixture.detectChanges();

      expect(authService.login).toHaveBeenCalledWith({
        username: 'testuser',
        password: 'StrongPass123',
        rememberMe: false
      });
      expect(messageService.add).toHaveBeenCalledWith({
        severity: 'success',
        summary: 'Welcome Back!',
        detail: `Successfully logged in as ${mockLoginResponse.username}`,
        life: 3000
      });
      expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
      expect(component.loading).toBeFalse();
    }));

    it('should handle network error', fakeAsync(() => {
      const messageService = fixture.debugElement.injector.get(MessageService);
      spyOn(messageService, 'add');
      const networkError = new HttpErrorResponse({
        error: new ErrorEvent('Network error'),
        status: 0
      });
      authService.login.and.returnValue(throwError(() => networkError));

      component.onSubmit();
      fixture.detectChanges();
      tick();
      fixture.detectChanges();

      expect(messageService.add).toHaveBeenCalledWith({
        severity: 'error',
        summary: 'Login Failed',
        detail: 'Unable to connect to the server. Please check your internet connection.',
        life: 0,
        closable: true,
        sticky: true
      });
      expect(component.loading).toBeFalse();
    }));

    it('should handle unverified account error', fakeAsync(() => {
      const messageService = fixture.debugElement.injector.get(MessageService);
      spyOn(messageService, 'add');
      const unverifiedError = new HttpErrorResponse({
        error: { message: 'Please verify your email before logging in' },
        status: 401
      });
      authService.login.and.returnValue(throwError(() => unverifiedError));

      component.onSubmit();
      fixture.detectChanges();
      tick();
      fixture.detectChanges();

      expect(messageService.add).toHaveBeenCalledWith({
        severity: 'error',
        summary: 'Login Failed',
        detail: 'Please verify your email before logging in',
        life: 5000,
        closable: true,
        sticky: false
      });
      expect(component.loading).toBeFalse();
    }));

    it('should handle invalid credentials error', fakeAsync(() => {
      const messageService = fixture.debugElement.injector.get(MessageService);
      spyOn(messageService, 'add');
      const credentialsError = new HttpErrorResponse({
        error: { message: 'Invalid credentials' },
        status: 401
      });
      authService.login.and.returnValue(throwError(() => credentialsError));

      component.onSubmit();
      fixture.detectChanges();
      tick();
      fixture.detectChanges();

      expect(messageService.add).toHaveBeenCalledWith({
        severity: 'error',
        summary: 'Login Failed',
        detail: 'Invalid credentials',
        life: 5000,
        closable: true,
        sticky: false
      });
      expect(component.loading).toBeFalse();
    }));

    it('should handle invalid form submission', () => {
      const messageService = fixture.debugElement.injector.get(MessageService);
      spyOn(messageService, 'add');
      component.loginForm.get('username')?.setValue('');
      component.onSubmit();
      expect(authService.login).not.toHaveBeenCalled();
      expect(messageService.add).toHaveBeenCalledWith({
        severity: 'warn',
        summary: 'Invalid Form',
        detail: 'Please fill in all required fields correctly.',
        life: 5000
      });
    });
  });

  describe('Google Login', () => {
    beforeEach(() => {
      authService.initiateGoogleLogin = jasmine.createSpy('initiateGoogleLogin');
    });

    it('should call initiateGoogleLogin when Google login button is clicked', fakeAsync(() => {
      component.onGoogleLogin();
      fixture.detectChanges();
      tick();

      expect(authService.initiateGoogleLogin).toHaveBeenCalled();
      expect(component.loading).toBeTrue();
    }));

    it('should handle errors during Google login initiation', fakeAsync(() => {
      const messageService = fixture.debugElement.injector.get(MessageService);
      spyOn(messageService, 'add');
      authService.initiateGoogleLogin.and.throwError('Test error');

      component.onGoogleLogin();
      fixture.detectChanges();
      tick();

      expect(messageService.add).toHaveBeenCalledWith({
        severity: 'error',
        summary: 'Login Failed',
        detail: 'Failed to initiate Google login. Please try again.',
        life: 5000
      });
      expect(component.loading).toBeFalse();
    }));
  });

  describe('Error Messages', () => {
    beforeEach(() => {
      // Reset form before each test
      component.loginForm.reset();
    });

    it('should return empty string for untouched controls', () => {
      expect(component.getErrorMessage('username')).toBe('');
      expect(component.getErrorMessage('password')).toBe('');
    });

    it('should return appropriate username error messages', () => {
      const control = component.loginForm.get('username');
      control?.markAsTouched();

      control?.setErrors({ required: true });
      expect(component.getErrorMessage('username')).toBe('Username is required');

      control?.setErrors({ minlength: true });
      expect(component.getErrorMessage('username')).toBe('Username must be at least 3 characters');

      control?.setErrors({ maxlength: true });
      expect(component.getErrorMessage('username')).toBe('Username cannot exceed 20 characters');

      control?.setErrors({ pattern: true });
      expect(component.getErrorMessage('username')).toBe('Username can only contain letters, numbers, underscores, and hyphens');
    });

    it('should return appropriate password error messages', () => {
      const control = component.loginForm.get('password');
      control?.markAsTouched();

      control?.setErrors({ required: true });
      expect(component.getErrorMessage('password')).toBe('Password is required');

      control?.setErrors({ minlength: true });
      expect(component.getErrorMessage('password')).toBe('Password must be at least 8 characters');

      control?.setErrors({ pattern: true });
      expect(component.getErrorMessage('password')).toBe('Password must contain at least one uppercase letter, one lowercase letter, and one number');
    });

    it('should return empty string for unknown control name', () => {
      expect(component.getErrorMessage('nonexistentControl')).toBe('');
    });

    it('should return empty string for control with no errors', () => {
      const control = component.loginForm.get('username');
      control?.markAsTouched();
      control?.setErrors(null);
      expect(component.getErrorMessage('username')).toBe('');
    });
  });
}); 
