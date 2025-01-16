import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { DOCUMENT } from '@angular/common';
import { isPlatformBrowser } from '@angular/common';
import { PLATFORM_ID } from '@angular/core';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  console.log('[AuthInterceptor] Intercepting request:', req.url);
  
  const authService = inject(AuthService);
  const router = inject(Router);
  const platformId = inject(PLATFORM_ID);
  const isBrowser = isPlatformBrowser(platformId);
  
  // Only inject document if we're in a browser environment
  const document = isBrowser ? inject(DOCUMENT) : null;
  const token = authService.getAuthToken();
  
  // Clone the request to add headers
  let modifiedReq = req;

  // Add Authorization header if token exists
  if (token) {
    console.log('[AuthInterceptor] Adding auth token to request');
    modifiedReq = modifiedReq.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  // Add CSRF token only if we're in a browser environment and have access to document
  if (isBrowser && document) {
    const csrfToken = getCookie('XSRF-TOKEN', document);
    if (csrfToken && !['GET', 'HEAD', 'OPTIONS'].includes(req.method)) {
      console.log('[AuthInterceptor] Adding CSRF token to request');
      modifiedReq = modifiedReq.clone({
        setHeaders: {
          'X-XSRF-TOKEN': csrfToken
        }
      });
    }
  }

  return next(modifiedReq).pipe(
    catchError(error => {
      if (error.status === 401) {
        console.log('[AuthInterceptor] Token expired or invalid, redirecting to login');
        authService.logout();
        router.navigate(['/auth/login']);
      } else if (error.status === 403 && error.error?.message?.includes('CSRF')) {
        console.error('[AuthInterceptor] CSRF token validation failed');
        if (isBrowser && document?.defaultView) {
          document.defaultView.location.reload();
        }
      }
      return throwError(() => error);
    })
  );
};

function getCookie(name: string, document: Document): string | null {
  if (!document?.cookie) return null;
  const cookieValue = document.cookie
    .split('; ')
    .find(row => row.startsWith(name + '='));
  return cookieValue ? decodeURIComponent(cookieValue.split('=')[1]) : null;
} 
