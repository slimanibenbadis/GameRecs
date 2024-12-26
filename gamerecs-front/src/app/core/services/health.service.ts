import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, map } from 'rxjs';

export interface IHealthResponse {
  status: string;
  components?: {
    [key: string]: {
      status: string;
      details?: any;
    };
  };
}

@Injectable({
  providedIn: 'root'
})
export class HealthService {
  private readonly _backendHealthEndpoint = '/actuator/health';
  private readonly _frontendHealthEndpoint = '/health';

  constructor(private _http: HttpClient) {
    console.log('[HealthService] Initializing health service');
  }

  checkHealth(): Observable<IHealthResponse> {
    console.log('[HealthService] Checking health status');
    
    return forkJoin({
      frontend: this._http.get<IHealthResponse>(this._frontendHealthEndpoint),
      backend: this._http.get<IHealthResponse>(this._backendHealthEndpoint)
    }).pipe(
      map(({ frontend, backend }) => {
        console.log('[HealthService] Frontend health:', frontend);
        console.log('[HealthService] Backend health:', backend);
        
        return {
          status: frontend.status === 'UP' && backend.status === 'UP' ? 'UP' : 'DOWN',
          components: {
            frontend: { status: frontend.status },
            backend: { 
              status: backend.status,
              details: backend.components 
            }
          }
        };
      })
    );
  }
} 
