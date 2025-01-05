import { Routes } from '@angular/router';
import { HealthComponent } from './core/components/health/health.component';
import { RegistrationFormComponent } from './core/components/auth/registration-form.component';
import { EmailVerificationComponent } from './core/components/auth/email-verification.component';
import { LoginFormComponent } from './core/components/auth/login-form.component';

export const routes: Routes = [
  { path: 'health', component: HealthComponent },
  { 
    path: 'auth', 
    children: [
      { path: 'login', component: LoginFormComponent },
      { path: 'register', component: RegistrationFormComponent },
      { path: 'verify-email', component: EmailVerificationComponent }
    ]
  },
  { path: 'verify', component: EmailVerificationComponent },
  { path: '', redirectTo: '/auth/login', pathMatch: 'full' },
  { path: '**', redirectTo: '/auth/login' }
];
