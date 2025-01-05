import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
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
  username: string;
  email: string;
  emailVerified: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly _apiUrl = '/api/auth';
  private readonly _usersUrl = '/api/users';
  private readonly _authTokenKey = 'auth_token';
  private readonly _currentUserKey = 'current_user';
  private _isAuthenticated: BehaviorSubject<boolean>;
  private _currentUser: BehaviorSubject<ILoginResponse | null>;

  constructor(private _http: HttpClient) {
    console.log('[AuthService] Initializing auth service');
    // Initialize BehaviorSubjects after checking token and loading user
    const hasToken = this.hasValidToken();
    const storedUser = this.loadStoredUser();
    this._isAuthenticated = new BehaviorSubject<boolean>(hasToken);
    this._currentUser = new BehaviorSubject<ILoginResponse | null>(storedUser);
  }

  get isAuthenticated$(): Observable<boolean> {
    return this._isAuthenticated.asObservable();
  }

  get currentUser$(): Observable<ILoginResponse | null> {
    return this._currentUser.asObservable();
  }

  get currentUserValue(): ILoginResponse | null {
    return this._currentUser.value;
  }

  isAuthenticated(): boolean {
    console.log('[AuthService] Checking authentication status');
    return this.hasValidToken();
  }

  registerUser(user: IUserRegistration): Observable<IRegistrationResponse> {
    console.log('[AuthService] Registering new user:', { username: user.username, email: user.email });
    
    return this._http.post<IRegistrationResponse>(`${this._usersUrl}/register`, user)
      .pipe(
        tap(response => console.log('[AuthService] User registration successful:', { userId: response.userId, username: response.username })),
        catchError((error) => this.handleError(error))
      );
  }

  verifyEmail(token: string): Observable<IVerificationResponse> {
    console.log('[AuthService] Verifying email with token');
    
    return this._http.get<IVerificationResponse>(`${this._usersUrl}/verify?token=${token}`)
      .pipe(
        tap(response => console.log('[AuthService] Email verification response:', response)),
        catchError((error) => this.handleError(error))
      );
  }

  login(loginData: ILoginRequest): Observable<ILoginResponse> {
    console.log('[AuthService] Attempting login for user:', loginData.username);
    
    return this._http.post<ILoginResponse>(`${this._apiUrl}/login`, loginData)
      .pipe(
        tap(response => {
          console.log('[AuthService] Login successful for user:', response.username);
          this.storeAuthData(response, loginData.rememberMe);
          this._isAuthenticated.next(true);
          this._currentUser.next(response);
        }),
        catchError((error) => this.handleError(error))
      );
  }

  logout(): void {
    console.log('[AuthService] Logging out user');
    localStorage.removeItem(this._authTokenKey);
    localStorage.removeItem(this._currentUserKey);
    sessionStorage.removeItem(this._authTokenKey);
    sessionStorage.removeItem(this._currentUserKey);
    this._isAuthenticated.next(false);
    this._currentUser.next(null);
  }

  getAuthToken(): string | null {
    return localStorage.getItem(this._authTokenKey) || sessionStorage.getItem(this._authTokenKey);
  }

  private storeAuthData(response: ILoginResponse, rememberMe: boolean): void {
    const storage = rememberMe ? localStorage : sessionStorage;
    storage.setItem(this._authTokenKey, response.token);
    storage.setItem(this._currentUserKey, JSON.stringify(response));
  }

  private hasValidToken(): boolean {
    const token = this.getAuthToken();
    if (!token) {
      console.log('[AuthService] No token found');
      return false;
    }
    console.log('[AuthService] Valid token found');
    return true;
  }

  private loadStoredUser(): ILoginResponse | null {
    try {
      const userStr = localStorage.getItem(this._currentUserKey) || sessionStorage.getItem(this._currentUserKey);
      if (!userStr) return null;
      
      const user = JSON.parse(userStr) as ILoginResponse;
      // Validate parsed user object has required properties according to ILoginResponse
      if (!user.username || !user.email || typeof user.emailVerified !== 'boolean') {
        console.warn('[AuthService] Stored user data is invalid');
        return null;
      }
      return user;
    } catch (e) {
      console.error('[AuthService] Error parsing stored user:', e);
      return null;
    }
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
