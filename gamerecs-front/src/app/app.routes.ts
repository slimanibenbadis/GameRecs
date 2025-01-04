import { Routes } from '@angular/router';
import { HealthComponent } from './core/components/health/health.component';
import { RegistrationFormComponent } from './core/components/auth/registration-form.component';
import { EmailVerificationComponent } from './core/components/auth/email-verification.component';

export const routes: Routes = [
  { path: 'health', component: HealthComponent },
  { 
    path: 'auth', 
    children: [
      { path: 'register', component: RegistrationFormComponent },
      { path: 'verify-email', component: EmailVerificationComponent }
    ]
  },
  { path: '', redirectTo: '/auth/register', pathMatch: 'full' }, // Redirect root to registration for now
  { path: '**', redirectTo: '/auth/register' } // Catch all route
];
