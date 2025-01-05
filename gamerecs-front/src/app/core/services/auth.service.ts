import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';

export interface IUserRegistration {
  username: string;
  email: string;
  password: string;
  bio?: string;
  profilePictureUrl?: string;
}

export interface IRegistrationResponse {
  userId: string;
  username: string;
  email: string;
  bio?: string;
  profilePictureUrl?: string;
  joinDate: string;
}

export interface IVerificationResponse {
  message: string;
  verified: boolean;
}

export interface IApiError {
  timestamp: string;
  status: number;
  message: string;
  errors?: { [key: string]: string };
}

export interface ILoginRequest {
  username: string;
  password: string;
  rememberMe: boolean;
}

export interface ILoginResponse {
  token: string;
  userId: string;
  username: string;
  email: string;
  bio?: string;
  profilePictureUrl?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly _apiUrl = '/api/users';

  constructor(private _http: HttpClient) {
    console.log('[AuthService] Initializing auth service');
  }

  registerUser(user: IUserRegistration): Observable<IRegistrationResponse> {
    console.log('[AuthService] Registering new user:', { username: user.username, email: user.email });
    
    return this._http.post<IRegistrationResponse>(`${this._apiUrl}/register`, user)
      .pipe(
        tap(response => console.log('[AuthService] User registration successful:', { userId: response.userId, username: response.username })),
        catchError((error) => this.handleError(error))
      );
  }

  verifyEmail(token: string): Observable<IVerificationResponse> {
    console.log('[AuthService] Verifying email with token');
    
    return this._http.get<IVerificationResponse>(`${this._apiUrl}/verify?token=${token}`)
      .pipe(
        tap(response => console.log('[AuthService] Email verification response:', response)),
        catchError((error) => this.handleError(error))
      );
  }

  login(loginData: ILoginRequest): Observable<ILoginResponse> {
    console.log('[AuthService] Attempting login for user:', loginData.username);
    
    return this._http.post<ILoginResponse>(`${this._apiUrl}/login`, loginData)
      .pipe(
        tap(response => console.log('[AuthService] Login successful for user:', response.username)),
        catchError((error) => this.handleError(error))
      );
  }

  private handleError(error: HttpErrorResponse) {
    console.error('[AuthService] An error occurred:', error);

    let errorMessage: string;

    if (error.error instanceof ErrorEvent) {
      // Client-side error
      console.error('[AuthService] Client error:', error.error.message);
      errorMessage = error.error.message;
    } else {
      // Server-side error
      console.error(
        `[AuthService] Server error - Backend returned code ${error.status}, ` +
        `body was:`, error.error
      );
      
      // Handle 401 Unauthorized specifically
      if (error.status === 401) {
        errorMessage = 'Invalid username or password';
      } else {
        try {
          const apiError = error.error as IApiError;
          if (apiError?.errors && Object.keys(apiError.errors).length > 0) {
            // If we have field-specific errors, join them
            errorMessage = Object.values(apiError.errors).join('. ');
          } else {
            // Determine context based on URL
            const context = this.getOperationContext(error.url);
            errorMessage = apiError?.message || `An error occurred during ${context}`;
          }
        } catch (e) {
          console.error('[AuthService] Error parsing error response:', e);
          const context = this.getOperationContext(error.url);
          errorMessage = `An error occurred during ${context}`;
        }
      }
    }

    return throwError(() => new Error(errorMessage));
  }

  private getOperationContext(url: string | null): string {
    if (!url) return 'the operation';
    
    if (url.includes('/register')) return 'registration';
    if (url.includes('/login')) return 'login';
    if (url.includes('/verify')) return 'email verification';
    
    return 'the operation';
  }
} 
