import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-google-callback',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="flex items-center justify-center min-h-screen bg-background">
      <div *ngIf="isLoading" class="text-center" data-cy="google-callback-loading">
        <div class="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary mx-auto mb-4"></div>
        <p class="text-text">Processing your login...</p>
      </div>
      <div *ngIf="error" class="text-red-400">
        {{ error }}
      </div>
    </div>
  `
})
export class GoogleCallbackComponent implements OnInit {
  isLoading = true;
  error: string | null = null;

  constructor(
    private _authService: AuthService,
    private _router: Router,
    private _route: ActivatedRoute
  ) {
    console.log('[GoogleCallbackComponent] Component initialized');
  }

  ngOnInit(): void {
    console.log('[GoogleCallbackComponent] Initializing component');
    this._route.queryParams.subscribe(params => {
      console.log('[GoogleCallbackComponent] Received params:', params);
      const token = params['token'];
      const code = params['code'];
      
      if (token) {
        console.log('[GoogleCallbackComponent] Received JWT token');
        // Store the token and update auth state
        this._authService.handleGoogleToken(token);
        this.isLoading = false;
        this._router.navigate(['/profile']);
        return;
      }
      
      if (!code) {
        console.warn('[GoogleCallbackComponent] No auth code or token found in URL');
        this.error = 'Authentication failed. Please try again.';
        this._router.navigate(['/auth/login']);
        return;
      }

      this._authService.handleGoogleCallback(code).subscribe({
        next: () => {
          console.log('[GoogleCallbackComponent] Google OAuth login successful');
          this.isLoading = false;
          this._router.navigate(['/profile']);
        },
        error: (error) => {
          console.error('[GoogleCallbackComponent] Error during Google OAuth callback:', error);
          this.error = 'Authentication failed. Please try again.';
          this.isLoading = false;
          this._router.navigate(['/auth/login']);
        }
      });
    });
  }
} 
