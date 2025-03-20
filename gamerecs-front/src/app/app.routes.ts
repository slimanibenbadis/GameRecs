import { Routes } from '@angular/router';
import { HealthComponent } from './core/components/health/health.component';
import { RegistrationFormComponent } from './core/components/auth/registration-form.component';
import { EmailVerificationComponent } from './core/components/auth/email-verification.component';
import { LoginFormComponent } from './core/components/auth/login-form.component';
import { LandingPageComponent } from './core/components/landing/landing-page.component';
import { GoogleCallbackComponent } from './core/components/auth/google-callback.component';
import { AuthGuard } from './core/guards/auth.guard';
import { ProfileComponent } from './features/profile/profile.component';
import { GameLibraryComponent } from './features/game-library/game-library.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';

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
      { path: 'verify-email', component: EmailVerificationComponent },
      { path: 'google/callback', component: GoogleCallbackComponent }
    ]
  },
  { path: 'verify', component: EmailVerificationComponent },
  { 
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [AuthGuard]
  },
  { 
    path: 'profile',
    component: ProfileComponent,
    canActivate: [AuthGuard]
  },
  { 
    path: 'library',
    component: GameLibraryComponent,
    canActivate: [AuthGuard]
  },
  { path: '', component: LandingPageComponent, pathMatch: 'full' },
  { path: '**', redirectTo: '/' }
];
