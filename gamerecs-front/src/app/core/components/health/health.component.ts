import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HealthService } from '../../services/health.service';

@Component({
  selector: 'app-health',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="health-status">
      {{ healthStatus }}
    </div>
  `,
  styles: [`
    .health-status {
      display: none;
    }
  `]
})
export class HealthComponent {
  healthStatus = 'UP';

  constructor(private healthService: HealthService) {
    this.checkHealth();
  }

  private checkHealth(): void {
    this.healthService.checkHealth().subscribe(
      isHealthy => {
        this.healthStatus = isHealthy ? 'UP' : 'DOWN';
      }
    );
  }
} 