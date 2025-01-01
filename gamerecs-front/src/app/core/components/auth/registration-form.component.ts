import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MessageService } from 'primeng/api';

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
    private messageService: MessageService
  ) {
    console.log('[RegistrationFormComponent] Initializing component');
    this.registrationForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(20)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]],
      bio: [''],
      profilePictureUrl: ['']
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
      // TODO: Implement actual registration logic with UserService
      // For now, just show a success message
      this.messageService.add({
        severity: 'success',
        summary: 'Registration Successful',
        detail: 'Your account has been created successfully!'
      });
      this.loading = false;
    } else {
      console.log('[RegistrationFormComponent] Form validation failed');
      this.messageService.add({
        severity: 'error',
        summary: 'Validation Error',
        detail: 'Please check all fields and try again.'
      });
    }
  }
} 
