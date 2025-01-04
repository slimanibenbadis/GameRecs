import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { AuthService } from '../../services/auth.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

// PrimeNG Imports
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';

@Component({
  selector: 'app-email-verification',
  template: `
    <div class="flex flex-col items-center justify-center min-h-screen bg-surface-0 dark:bg-surface-900 p-4">
      <div class="w-full max-w-lg p-6 bg-surface-0 dark:bg-surface-800 rounded-lg shadow-lg text-center">
        <h2 class="text-2xl font-fira font-bold mb-6 text-surface-900 dark:text-surface-0">Email Verification</h2>
        
        <div *ngIf="loading" class="flex flex-col items-center gap-4">
          <p-progressSpinner [style]="{width: '50px', height: '50px'}"></p-progressSpinner>
          <p class="text-surface-700 dark:text-surface-200">Verifying your email...</p>
        </div>

        <div *ngIf="!loading" class="space-y-4">
          <div *ngIf="verified" class="text-green-600 dark:text-green-400">
            <i class="pi pi-check-circle text-3xl mb-2"></i>
            <p class="text-lg">Your email has been successfully verified!</p>
          </div>

          <div *ngIf="error" class="text-red-600 dark:text-red-400">
            <i class="pi pi-times-circle text-3xl mb-2"></i>
            <p class="text-lg">{{ errorMessage }}</p>
          </div>

          <button pButton 
                  [label]="verified ? 'Go to Login' : 'Back to Registration'"
                  class="p-button-primary w-full mt-4"
                  (click)="navigateToAuth()">
          </button>
        </div>
      </div>
    </div>
    <p-toast></p-toast>
  `,
  standalone: true,
  imports: [
    CommonModule,
    ProgressSpinnerModule,
    ButtonModule,
    ToastModule
  ],
  providers: [MessageService]
})
export class EmailVerificationComponent implements OnInit, OnDestroy {
  loading = true;
  verified = false;
  error = false;
  errorMessage = '';
  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private messageService: MessageService
  ) {
    console.log('[EmailVerificationComponent] Initializing component');
  }

  ngOnInit(): void {
    console.log('[EmailVerificationComponent] Component initialized');
    
    this.route.queryParams
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        const token = params['token'];
        if (!token) {
          console.error('[EmailVerificationComponent] No token provided');
          this.handleError('Invalid verification link. Please request a new one.');
          return;
        }

        this.verifyEmail(token);
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private verifyEmail(token: string): void {
    this.loading = true;
    this.error = false;
    this.verified = false;
    
    console.log('[EmailVerificationComponent] Attempting to verify email with token');
    
    this.authService.verifyEmail(token)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          console.log('[EmailVerificationComponent] Verification response:', response);
          this.loading = false;
          
          if (response.verified) {
            this.verified = true;
            this.messageService.add({
              severity: 'success',
              summary: 'Verification Successful',
              detail: response.message
            });
          } else {
            this.handleError(response.message || 'Email verification failed');
          }
        },
        error: (error) => {
          console.error('[EmailVerificationComponent] Verification failed:', error);
          this.handleError(error.message || 'Email verification failed. Please try again.');
        }
      });
  }

  private handleError(message: string): void {
    console.log('[EmailVerificationComponent] Handling error:', message);
    this.loading = false;
    this.verified = false;
    this.error = true;
    this.errorMessage = message;
    
    this.messageService.add({
      severity: 'error',
      summary: 'Verification Failed',
      detail: message,
      life: 5000
    });
  }

  navigateToAuth(): void {
    console.log('[EmailVerificationComponent] Navigating to auth page');
    // TODO: Update this path once login component is implemented
    this.router.navigate(['/auth/register']);
  }
} 
