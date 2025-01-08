import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

// PrimeNG Imports
import { ButtonModule } from 'primeng/button';

@Component({
  selector: 'app-landing-page',
  templateUrl: './landing-page.component.html',
  styleUrls: ['./landing-page.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ButtonModule
  ]
})
export class LandingPageComponent {
  constructor() {
    console.log('[LandingPageComponent] Initializing landing page');
  }
} 
