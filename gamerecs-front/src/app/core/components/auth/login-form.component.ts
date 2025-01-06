import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { AuthService, ILoginRequest, ILoginResponse } from '../../services/auth.service';
import { environment } from '../../../../environments/environment';
import { Toast } from 'primeng/toast';
import { Router, RouterModule } from '@angular/router';

// PrimeNG Imports
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { CheckboxModule } from 'primeng/checkbox';

@Component({
  selector: 'app-login-form',
  templateUrl: './login-form.component.html',
  styleUrls: ['./login-form.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    InputTextModule,
    PasswordModule,
    ButtonModule,
    ToastModule,
    CheckboxModule
  ],
  providers: [MessageService]
})
export class LoginFormComponent implements OnInit {
  @ViewChild('toast') toast!: Toast;
  
  loginForm: FormGroup;
  loading = false;
  environment = environment;

  constructor(
    private fb: FormBuilder,
    private messageService: MessageService,
    private authService: AuthService,
    private router: Router
  ) {
    console.log('[LoginFormComponent] Initializing component');
    this.loginForm = this.fb.group({
      username: ['', [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(20),
        Validators.pattern('^[a-zA-Z0-9_-]*$')
      ]],
      password: ['', [
        Validators.required,
        Validators.minLength(8),
        Validators.pattern('^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,}$')
      ]],
      rememberMe: [false]
    });
  }

  ngOnInit(): void {
    console.log('[LoginFormComponent] Component initialized');
  }

  onSubmit(): void {
    if (this.loginForm.valid) {
      console.log('[LoginFormComponent] Form submitted:', this.loginForm.value);
      this.loading = true;

      const loginData: ILoginRequest = {
        username: this.loginForm.get('username')?.value,
        password: this.loginForm.get('password')?.value,
        rememberMe: this.loginForm.get('rememberMe')?.value
      };
      
      this.authService.login(loginData).subscribe({
        next: (response: ILoginResponse) => {
          console.log('[LoginFormComponent] Login successful');
          this.messageService.add({
            severity: 'success',
            summary: 'Welcome Back!',
            detail: `Successfully logged in as ${response.username}`,
            life: 3000
          });
          // Navigate to home page after successful login
          this.router.navigate(['/']);
        },
        error: (error: any) => {
          console.error('[LoginFormComponent] Login error:', error);
          let errorMessage = 'An error occurred during login. Please try again.';
          let errorLife = 5000;
          
          if (error instanceof Error) {
            errorMessage = error.message;
          } else if (error?.error instanceof ErrorEvent || error?.status === 0) {
            errorMessage = 'Unable to connect to the server. Please check your internet connection.';
            errorLife = 0; // Keep message until user dismisses it
          } else if (error?.status === 401) {
            if (error?.error?.message === 'Account is disabled') {
              errorMessage = 'Your account is not verified. Please check your email for the verification link.';
            } else {
              errorMessage = 'Invalid username or password. Please try again.';
            }
          } else if (error?.error?.message) {
            errorMessage = error.error.message;
          } else if (typeof error === 'string') {
            errorMessage = error;
          }

          this.messageService.add({
            severity: 'error',
            summary: 'Login Failed',
            detail: errorMessage,
            life: errorLife,
            closable: true,
            sticky: errorLife === 0
          });
          this.loading = false;
        },
        complete: () => {
          console.log('[LoginFormComponent] Login request completed');
          this.loading = false;
        }
      });
    } else {
      console.log('[LoginFormComponent] Form validation failed');
      this.messageService.add({
        severity: 'warn',
        summary: 'Invalid Form',
        detail: 'Please fill in all required fields correctly.',
        life: 5000
      });
    }
  }

  // Helper methods for validation messages
  getErrorMessage(controlName: string): string {
    const control = this.loginForm.get(controlName);
    if (!control?.errors || !control.touched) return '';

    const errors = control.errors;
    console.log(`[LoginFormComponent] Form errors for ${controlName}:`, errors);

    switch (controlName) {
    case 'username':
      if (errors['required']) return 'Username is required';
      if (errors['minlength']) return 'Username must be at least 3 characters';
      if (errors['maxlength']) return 'Username cannot exceed 20 characters';
      if (errors['pattern']) return 'Username can only contain letters, numbers, underscores, and hyphens';
      break;
    case 'password':
      if (errors['required']) return 'Password is required';
      if (errors['minlength']) return 'Password must be at least 8 characters';
      if (errors['pattern']) return 'Password must contain at least one uppercase letter, one lowercase letter, and one number';
      break;
    }
    return '';
  }
} 
