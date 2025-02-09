import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { RegistrationFormComponent } from './registration-form.component';
import { AuthService, IRegistrationResponse } from '../../services/auth.service';
import { MessageService } from 'primeng/api';
import { ReactiveFormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterModule } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

// PrimeNG Imports
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { TextareaModule } from 'primeng/textarea';

describe('RegistrationFormComponent', () => {
  let component: RegistrationFormComponent;
  let fixture: ComponentFixture<RegistrationFormComponent>;
  let authService: jasmine.SpyObj<AuthService>;
  let messageService: jasmine.SpyObj<MessageService>;

  const mockRegistrationResponse: IRegistrationResponse = {
    userId: '123',
    username: 'testuser',
    email: 'test@example.com',
    joinDate: new Date().toISOString()
  };

  beforeEach(async () => {
    const authServiceSpy = jasmine.createSpyObj('AuthService', ['registerUser']);
    const messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);

    await TestBed.configureTestingModule({
      imports: [
        ReactiveFormsModule,
        InputTextModule,
        PasswordModule,
        ButtonModule,
        ToastModule,
        TextareaModule,
        RegistrationFormComponent,
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
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(RegistrationFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Form Validation', () => {
    it('should initialize with invalid form', () => {
      expect(component.registrationForm.valid).toBeFalsy();
    });

    it('should validate required fields', () => {
      const form = component.registrationForm;
      expect(form.get('username')?.errors?.['required']).toBeTruthy();
      expect(form.get('email')?.errors?.['required']).toBeTruthy();
      expect(form.get('password')?.errors?.['required']).toBeTruthy();
      expect(form.get('confirmPassword')?.errors?.['required']).toBeTruthy();
    });

    it('should validate username format', () => {
      const usernameControl = component.registrationForm.get('username');
      usernameControl?.setValue('u$er');
      expect(usernameControl?.errors?.['pattern']).toBeTruthy();

      usernameControl?.setValue('validuser123');
      expect(usernameControl?.errors).toBeNull();
    });

    it('should validate email format', () => {
      const emailControl = component.registrationForm.get('email');
      emailControl?.setValue('invalid-email');
      expect(emailControl?.errors?.['email']).toBeTruthy();

      emailControl?.setValue('valid@email.com');
      expect(emailControl?.errors).toBeNull();
    });

    it('should validate password format', () => {
      const passwordControl = component.registrationForm.get('password');
      passwordControl?.setValue('weak');
      expect(passwordControl?.errors?.['pattern']).toBeTruthy();

      passwordControl?.setValue('StrongPass123');
      expect(passwordControl?.errors).toBeNull();
    });

    it('should validate password match', () => {
      const form = component.registrationForm;
      form.get('password')?.setValue('StrongPass123');
      form.get('confirmPassword')?.setValue('DifferentPass123');
      
      expect(form.hasError('mismatch')).toBeTruthy();
      expect(form.get('confirmPassword')?.errors?.['mismatch']).toBeTruthy();

      form.get('confirmPassword')?.setValue('StrongPass123');
      expect(form.hasError('mismatch')).toBeFalsy();
      expect(form.get('confirmPassword')?.errors).toBeNull();
    });

    it('should validate optional bio length', () => {
      const bioControl = component.registrationForm.get('bio');
      const longBio = 'a'.repeat(501);
      bioControl?.setValue(longBio);
      expect(bioControl?.errors?.['maxlength']).toBeTruthy();

      bioControl?.setValue('Valid bio');
      expect(bioControl?.errors).toBeNull();
    });

    it('should validate optional profile picture URL format', () => {
      const urlControl = component.registrationForm.get('profilePictureUrl');
      urlControl?.setValue('invalid-url');
      expect(urlControl?.errors?.['pattern']).toBeTruthy();

      urlControl?.setValue('https://example.com/image.jpg');
      expect(urlControl?.errors).toBeNull();
    });
  });

  describe('Form Submission', () => {
    beforeEach(() => {
      // Set valid form values
      component.registrationForm.setValue({
        username: 'testuser',
        email: 'test@example.com',
        password: 'StrongPass123',
        confirmPassword: 'StrongPass123',
        bio: 'Test bio',
        profilePictureUrl: 'https://example.com/image.jpg'
      });
    });

    it('should call registerUser when form is valid', fakeAsync(() => {
      const messageService = fixture.debugElement.injector.get(MessageService);
      spyOn(messageService, 'add');
      authService.registerUser.and.returnValue(of(mockRegistrationResponse));
      
      component.onSubmit();
      fixture.detectChanges();
      tick();
      fixture.detectChanges();

      expect(authService.registerUser).toHaveBeenCalledWith({
        username: 'testuser',
        email: 'test@example.com',
        password: 'StrongPass123',
        bio: 'Test bio',
        profilePictureUrl: 'https://example.com/image.jpg'
      });
      expect(messageService.add).toHaveBeenCalledWith({
        severity: 'success',
        summary: 'Registration Successful',
        detail: 'Your account has been created! Please check your email inbox for a verification link. The link will expire in 24 hours.'
      });
      expect(component.loading).toBeFalse();
    }));

    it('should handle case-insensitive username conflict error', fakeAsync(() => {
      const messageService = fixture.debugElement.injector.get(MessageService);
      spyOn(messageService, 'add');
      const errorResponse = new HttpErrorResponse({
        error: {
          message: 'Username already exists',
          errors: { username: 'Username already exists (case-insensitive)' }
        },
        status: 400
      });
      authService.registerUser.and.returnValue(throwError(() => errorResponse));
      
      // Set username with different case
      component.registrationForm.patchValue({ username: 'TestUser' });
      
      component.onSubmit();
      fixture.detectChanges();
      tick();
      fixture.detectChanges();

      expect(messageService.add).toHaveBeenCalledWith({
        severity: 'error',
        summary: 'Registration Failed',
        detail: 'An error occurred during registration. Please try again.',
        life: 5000
      });
      expect(component.loading).toBeFalse();
    }));

    it('should normalize username to lowercase before submission', fakeAsync(() => {
      authService.registerUser.and.returnValue(of(mockRegistrationResponse));
      
      // Set username with mixed case
      component.registrationForm.patchValue({ username: 'TestUser' });
      
      component.onSubmit();
      fixture.detectChanges();
      tick();
      fixture.detectChanges();

      // Verify the username was normalized to lowercase in the service call
      expect(authService.registerUser).toHaveBeenCalledWith({
        username: 'testuser', // Should be lowercase
        email: 'test@example.com',
        password: 'StrongPass123',
        bio: 'Test bio',
        profilePictureUrl: 'https://example.com/image.jpg'
      });
    }));

    it('should handle registration error', fakeAsync(() => {
      const messageService = fixture.debugElement.injector.get(MessageService);
      spyOn(messageService, 'add');
      const errorResponse = new HttpErrorResponse({
        error: {
          message: 'Registration failed',
          errors: { email: 'Email already exists' }
        },
        status: 400
      });
      authService.registerUser.and.returnValue(throwError(() => errorResponse));
      
      component.onSubmit();
      fixture.detectChanges();
      tick();
      fixture.detectChanges();

      expect(messageService.add).toHaveBeenCalledWith({
        severity: 'error',
        summary: 'Registration Failed',
        detail: 'An error occurred during registration. Please try again.',
        life: 5000
      });
      expect(component.loading).toBeFalse();
    }));

    it('should handle Error instance in registration error', fakeAsync(() => {
      const messageService = fixture.debugElement.injector.get(MessageService);
      spyOn(messageService, 'add');
      const errorInstance = new Error('Custom error message');
      authService.registerUser.and.returnValue(throwError(() => errorInstance));
      
      component.onSubmit();
      fixture.detectChanges();
      tick();
      fixture.detectChanges();

      expect(messageService.add).toHaveBeenCalledWith({
        severity: 'error',
        summary: 'Registration Failed',
        detail: 'Custom error message',
        life: 5000
      });
      expect(component.loading).toBeFalse();
    }));

    it('should not submit if form is invalid', () => {
      const messageService = fixture.debugElement.injector.get(MessageService);
      spyOn(messageService, 'add');
      component.registrationForm.get('email')?.setValue('invalid-email');
      component.onSubmit();
      expect(authService.registerUser).not.toHaveBeenCalled();
    });
  });

  describe('Error Messages', () => {
    beforeEach(() => {
      // Reset form before each test
      component.registrationForm.reset();
    });

    it('should return empty string for untouched controls', () => {
      expect(component.getErrorMessage('username')).toBe('');
      expect(component.getErrorMessage('email')).toBe('');
      expect(component.getErrorMessage('password')).toBe('');
      expect(component.getErrorMessage('bio')).toBe('');
      expect(component.getErrorMessage('profilePictureUrl')).toBe('');
    });

    it('should return appropriate username error messages', () => {
      const control = component.registrationForm.get('username');
      control?.markAsTouched();

      control?.setErrors({ required: true });
      expect(component.getErrorMessage('username')).toBe('Username is required');

      control?.setErrors({ minlength: true });
      expect(component.getErrorMessage('username')).toBe('Username must be at least 3 characters');

      control?.setErrors({ maxlength: true });
      expect(component.getErrorMessage('username')).toBe('Username cannot exceed 20 characters');

      control?.setErrors({ pattern: true });
      expect(component.getErrorMessage('username')).toBe('Username can only contain letters, numbers, underscores, and hyphens (case-insensitive)');
    });

    it('should return appropriate email error messages', () => {
      const control = component.registrationForm.get('email');
      control?.markAsTouched();

      control?.setErrors({ required: true });
      expect(component.getErrorMessage('email')).toBe('Email is required');

      control?.setErrors({ email: true });
      expect(component.getErrorMessage('email')).toBe('Please enter a valid email address');

      control?.setErrors({ pattern: true });
      expect(component.getErrorMessage('email')).toBe('Please enter a valid email address');
    });

    it('should return appropriate password error messages', () => {
      const control = component.registrationForm.get('password');
      control?.markAsTouched();

      control?.setErrors({ required: true });
      expect(component.getErrorMessage('password')).toBe('Password is required');

      control?.setErrors({ minlength: true });
      expect(component.getErrorMessage('password')).toBe('Password must be at least 8 characters');

      control?.setErrors({ pattern: true });
      expect(component.getErrorMessage('password')).toBe('Password must contain at least one uppercase letter, one lowercase letter, and one number');
    });

    it('should return appropriate bio error messages', () => {
      const control = component.registrationForm.get('bio');
      control?.markAsTouched();

      control?.setErrors({ maxlength: true });
      expect(component.getErrorMessage('bio')).toBe('Bio cannot exceed 500 characters');
    });

    it('should return appropriate profile picture URL error messages', () => {
      const control = component.registrationForm.get('profilePictureUrl');
      control?.markAsTouched();

      control?.setErrors({ pattern: true });
      expect(component.getErrorMessage('profilePictureUrl')).toBe('Please enter a valid image URL (http/https ending in .png, .jpg, .jpeg, or .gif)');
    });
  });
}); 
