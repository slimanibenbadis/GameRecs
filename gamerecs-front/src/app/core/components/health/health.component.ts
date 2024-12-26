import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HealthService, IHealthResponse } from '../../services/health.service';

@Component({
  selector: 'app-health',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="p-4">
      <h2 class="text-xl font-bold mb-4">System Health Status</h2>
      <div class="space-y-2">
        <div class="flex items-center">
          <span class="font-semibold mr-2">Overall Status:</span>
          <span [class]="health?.status === 'UP' ? 'text-green-600' : 'text-red-600'">
            {{ health?.status || 'Checking...' }}
          </span>
        </div>
        
        <div *ngIf="health?.components" class="mt-4">
          <h3 class="text-lg font-semibold mb-2">Components:</h3>
          <div class="space-y-2">
            <div class="flex items-center">
              <span class="font-medium mr-2">Frontend:</span>
              <span [class]="health?.components?.['frontend']?.status === 'UP' ? 'text-green-600' : 'text-red-600'">
                {{ health?.components?.['frontend']?.status || 'Unknown' }}
              </span>
            </div>
            
            <div class="flex items-center">
              <span class="font-medium mr-2">Backend:</span>
              <span [class]="health?.components?.['backend']?.status === 'UP' ? 'text-green-600' : 'text-red-600'">
                {{ health?.components?.['backend']?.status || 'Unknown' }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class HealthComponent implements OnInit {
  health?: IHealthResponse;

  constructor(private _healthService: HealthService) {
    console.log('[HealthComponent] Initializing health check component');
  }

  ngOnInit(): void {
    console.log('[HealthComponent] Checking system health status');
    this._healthService.checkHealth().subscribe({
      next: (response: IHealthResponse) => {
        console.log('[HealthComponent] Health check response:', response);
        this.health = response;
      },
      error: (error: Error) => {
        console.error('[HealthComponent] Health check failed:', error);
        this.health = {
          status: 'DOWN',
          components: {
            frontend: { status: 'DOWN' },
            backend: { status: 'DOWN' }
          }
        };
      }
    });
  }
} 
