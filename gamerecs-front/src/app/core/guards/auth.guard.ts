import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { Observable } from 'rxjs';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {
    console.log('[AuthGuard] Initializing guard');
  }

  canActivate(): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
    console.log('[AuthGuard] Checking route access');
    
    if (this.authService.isAuthenticated()) {
      console.log('[AuthGuard] Access granted - user is authenticated');
      return true;
    }

    console.log('[AuthGuard] Access denied - redirecting to login');
    return this.router.createUrlTree(['/auth/login']);
  }
} 
