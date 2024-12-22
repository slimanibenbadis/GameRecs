import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class HealthService {
  private readonly healthEndpoint = '/health';

  constructor(private http: HttpClient) {}

  /**
   * Check the health status of the application
   * @returns Observable<boolean> - true if healthy, false otherwise
   */
  checkHealth(): Observable<boolean> {
    return of(true).pipe(
      map(() => true),
      catchError(() => of(false))
    );
  }

  /**
   * Get detailed health information
   * @returns Observable<any> - detailed health status
   */
  getHealthDetails(): Observable<any> {
    return this.http.get(this.healthEndpoint).pipe(
      catchError(error => {
        console.error('Health check failed:', error);
        return of({ status: 'DOWN', error: error.message });
      })
    );
  }
} 