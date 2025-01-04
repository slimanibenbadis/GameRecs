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
  templateUrl: './email-verification.component.html',
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
          
          if (response.message?.toLowerCase().includes('success')) {
            this.verified = true;
            this.error = false;
            this.messageService.add({
              severity: 'success',
              summary: 'Success',
              detail: 'Your email has been successfully verified!'
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
