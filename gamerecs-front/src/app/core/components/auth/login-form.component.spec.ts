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
        error: { message: 'Account is disabled' },
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
        detail: 'Your account is not verified. Please check your email for the verification link.',
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
        detail: 'Invalid username or password. Please try again.',
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
}); 
