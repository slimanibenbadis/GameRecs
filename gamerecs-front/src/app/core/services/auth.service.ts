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

export interface IApiError {
  timestamp: string;
  status: number;
  message: string;
  errors?: { [key: string]: string };
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
        catchError(this.handleError)
      );
  }

  private handleError(error: HttpErrorResponse) {
    console.error('[AuthService] An error occurred:', error);

    let errorMessage = 'An unknown error occurred';
    
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
      
      try {
        const apiError = error.error as IApiError;
        if (apiError.errors && Object.keys(apiError.errors).length > 0) {
          // If we have field-specific errors, join them
          errorMessage = Object.values(apiError.errors).join('. ');
        } else {
          errorMessage = apiError.message || 'An error occurred during registration';
        }
      } catch (e) {
        console.error('[AuthService] Error parsing error response:', e);
        errorMessage = 'An error occurred during registration';
      }
    }

    return throwError(() => new Error(errorMessage));
  }
} 
