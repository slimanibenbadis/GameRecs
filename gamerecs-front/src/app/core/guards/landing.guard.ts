import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { Observable } from 'rxjs';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class LandingGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {
    console.log('[LandingGuard] Initializing guard');
  }

  canActivate(): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
    console.log('[LandingGuard] Checking if user should access landing page');
    
    if (this.authService.isAuthenticated()) {
      console.log('[LandingGuard] User is authenticated, redirecting to dashboard');
      return this.router.createUrlTree(['/dashboard']);
    }

    console.log('[LandingGuard] User is not authenticated, allowing access to landing page');
    return true;
  }
} 
