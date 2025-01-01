import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { AuthService } from '../../services/auth.service';

// PrimeNG Imports
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { TextareaModule } from 'primeng/textarea';

@Component({
  selector: 'app-registration-form',
  templateUrl: './registration-form.component.html',
  styleUrls: ['./registration-form.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    InputTextModule,
    PasswordModule,
    ButtonModule,
    ToastModule,
    TextareaModule
  ],
  providers: [MessageService]
})
export class RegistrationFormComponent implements OnInit {
  registrationForm: FormGroup;
  loading = false;

  constructor(
    private fb: FormBuilder,
    private messageService: MessageService,
    private authService: AuthService
  ) {
    console.log('[RegistrationFormComponent] Initializing component');
    this.registrationForm = this.fb.group({
      username: ['', [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(20),
        Validators.pattern('^[a-zA-Z0-9_-]*$')
      ]],
      email: ['', [
        Validators.required,
        Validators.email,
        Validators.pattern('^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$')
      ]],
      password: ['', [
        Validators.required,
        Validators.minLength(8),
        Validators.pattern('^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,}$')
      ]],
      confirmPassword: ['', [Validators.required]],
      bio: ['', [Validators.maxLength(500)]],
      profilePictureUrl: ['', [Validators.pattern('^(https?:\\/\\/.*\\.(?:png|jpg|jpeg|gif))$')]]
    }, { validators: this.passwordMatchValidator });
  }

  ngOnInit(): void {
    console.log('[RegistrationFormComponent] Component initialized');
  }

  private passwordMatchValidator(g: FormGroup) {
    const password = g.get('password')?.value;
    const confirmPassword = g.get('confirmPassword')?.value;
    return password === confirmPassword ? null : { mismatch: true };
  }

  onSubmit(): void {
    if (this.registrationForm.valid) {
      console.log('[RegistrationFormComponent] Form submitted:', this.registrationForm.value);
      this.loading = true;

      const { confirmPassword, ...registrationData } = this.registrationForm.value;
      
      this.authService.registerUser(registrationData).subscribe({
        next: (response) => {
          console.log('[RegistrationFormComponent] Registration successful:', response);
          this.messageService.add({
            severity: 'success',
            summary: 'Registration Successful',
            detail: 'Your account has been created successfully!'
          });
          this.registrationForm.reset();
        },
        error: (error) => {
          console.error('[RegistrationFormComponent] Registration failed:', error);
          this.messageService.add({
            severity: 'error',
            summary: 'Registration Failed',
            detail: error.error?.message || 'An error occurred during registration. Please try again.'
          });
        },
        complete: () => {
          this.loading = false;
        }
      });
    } else {
      console.log('[RegistrationFormComponent] Form validation failed');
      this.messageService.add({
        severity: 'error',
        summary: 'Validation Error',
        detail: 'Please check all fields and try again.'
      });
    }
  }

  // Helper methods for validation messages
  getErrorMessage(controlName: string): string {
    const control = this.registrationForm.get(controlName);
    if (!control?.errors || !control.touched) return '';

    const errors = control.errors;
    console.log(`[RegistrationFormComponent] Form errors for ${controlName}:`, errors);

    switch (controlName) {
    case 'username':
      if (errors['required']) return 'Username is required';
      if (errors['minlength']) return 'Username must be at least 3 characters';
      if (errors['maxlength']) return 'Username cannot exceed 20 characters';
      if (errors['pattern']) return 'Username can only contain letters, numbers, underscores, and hyphens';
      break;
    case 'email':
      if (errors['required']) return 'Email is required';
      if (errors['email'] || errors['pattern']) return 'Please enter a valid email address';
      break;
    case 'password':
      if (errors['required']) return 'Password is required';
      if (errors['minlength']) return 'Password must be at least 8 characters';
      if (errors['pattern']) return 'Password must contain at least one uppercase letter, one lowercase letter, and one number';
      break;
    case 'bio':
      if (errors['maxlength']) return 'Bio cannot exceed 500 characters';
      break;
    case 'profilePictureUrl':
      if (errors['pattern']) return 'Please enter a valid image URL (http/https ending in .png, .jpg, .jpeg, or .gif)';
      break;
    }
    return '';
  }
} 
