import { Routes } from '@angular/router';
import { HealthComponent } from './core/components/health/health.component';
import { RegistrationFormComponent } from './core/components/auth/registration-form.component';
import { EmailVerificationComponent } from './core/components/auth/email-verification.component';
import { LoginFormComponent } from './core/components/auth/login-form.component';
import { LandingPageComponent } from './core/components/landing/landing-page.component';
import { AuthGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { 
    path: 'health', 
    component: HealthComponent,
    canActivate: [AuthGuard]
  },
  { 
    path: 'auth', 
    children: [
      { path: 'login', component: LoginFormComponent },
      { path: 'register', component: RegistrationFormComponent },
      { path: 'verify-email', component: EmailVerificationComponent }
    ]
  },
  { path: 'verify', component: EmailVerificationComponent },
  { path: '', component: LandingPageComponent, pathMatch: 'full' },
  { path: '**', redirectTo: '/' }
];
