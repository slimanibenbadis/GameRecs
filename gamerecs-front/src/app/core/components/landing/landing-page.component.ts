import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';

// PrimeNG Imports
import { ButtonModule } from 'primeng/button';

// App Imports
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-landing-page',
  templateUrl: './landing-page.component.html',
  styleUrls: ['./landing-page.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ButtonModule,
  ]
})
export class LandingPageComponent implements OnInit, OnDestroy {
  isAuthenticated = false;
  private authSubscription?: Subscription;

  constructor(private authService: AuthService) {
    console.log('[LandingPageComponent] Initializing landing page');
  }

  ngOnInit(): void {
    // Subscribe to authentication state changes
    this.authSubscription = this.authService.isAuthenticated$.subscribe(
      isAuth => {
        this.isAuthenticated = isAuth;
        console.log('[LandingPageComponent] Authentication state:', this.isAuthenticated);
      }
    );
  }

  ngOnDestroy(): void {
    // Clean up subscription to prevent memory leaks
    if (this.authSubscription) {
      this.authSubscription.unsubscribe();
    }
  }
} 
