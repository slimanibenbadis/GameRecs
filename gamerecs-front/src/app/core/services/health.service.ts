import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

interface IHealthResponse {
  status: string;
  components: {
    [key: string]: { status: string };
  };
}

@Injectable({
  providedIn: 'root'
})
export class HealthService {
  private readonly _healthEndpoint = '/api/actuator/health';

  constructor(
    private readonly _http: HttpClient
  ) {}

  /**
   * Checks the health status of the backend services
   * @returns Observable<boolean> - true if healthy, false if not
   */
  public checkHealth(): Observable<boolean> {
    return this._http
      .get<IHealthResponse>(this._healthEndpoint)
      .pipe(
        map((response: IHealthResponse) => response.status === 'UP')
      );
  }
} 