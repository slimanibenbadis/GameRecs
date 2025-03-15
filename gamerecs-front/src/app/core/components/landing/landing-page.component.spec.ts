import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LandingPageComponent } from './landing-page.component';
import { RouterTestingModule } from '@angular/router/testing';
import { Router } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { BehaviorSubject } from 'rxjs';
import { AuthService } from '../../services/auth.service';

// PrimeNG Imports
import { ButtonModule } from 'primeng/button';

describe('LandingPageComponent', () => {
  let component: LandingPageComponent;
  let fixture: ComponentFixture<LandingPageComponent>;
  let router: Router;
  let authService: jasmine.SpyObj<AuthService>;
  let isAuthenticatedSubject: BehaviorSubject<boolean>;

  beforeEach(async () => {
    isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
    authService = jasmine.createSpyObj('AuthService', ['logout'], {
      isAuthenticated$: isAuthenticatedSubject.asObservable()
    });

    await TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        ButtonModule,
        NoopAnimationsModule,
        HttpClientTestingModule,
        LandingPageComponent
      ],
      providers: [
        { provide: AuthService, useValue: authService }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(LandingPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Navigation', () => {
    beforeEach(() => {
      // Ensure we're testing the unauthenticated state
      isAuthenticatedSubject.next(false);
      fixture.detectChanges();
    });
    
    it('should have login button with correct route', () => {
      const loginButton = fixture.nativeElement.querySelector('button[routerLink="/auth/login"]');
      expect(loginButton).toBeTruthy();
      expect(loginButton.textContent.trim()).toBe('Login');
    });

    it('should have sign up button with correct route', () => {
      const signUpButton = fixture.nativeElement.querySelector('button[routerLink="/auth/register"]');
      expect(signUpButton).toBeTruthy();
      expect(signUpButton.textContent.trim()).toBe('Sign Up');
    });

    it('should have get started button with correct route', () => {
      const getStartedButton = fixture.nativeElement.querySelector('button[routerLink="/auth/register"].cta-button');
      expect(getStartedButton).toBeTruthy();
      expect(getStartedButton.textContent.trim()).toBe('Get Started');
    });

    it('should have learn more button with correct route', () => {
      const learnMoreButton = fixture.nativeElement.querySelector('button[routerLink="/about"]');
      expect(learnMoreButton).toBeTruthy();
      expect(learnMoreButton.textContent.trim()).toBe('Learn More');
    });
  });

  describe('Content', () => {
    beforeEach(() => {
      // Ensure we're testing the unauthenticated state for nav elements
      isAuthenticatedSubject.next(false);
      fixture.detectChanges();
    });
    
    it('should display app name in navigation', () => {
      const appName = fixture.nativeElement.querySelector('.font-fira.font-bold');
      expect(appName.textContent.trim()).toBe('Gamer-Reco');
    });

    it('should display main heading with correct text', () => {
      const heading = fixture.nativeElement.querySelector('h1');
      expect(heading.textContent).toContain('Find Games You\'ll');
      expect(heading.textContent).toContain('Actually Love');
    });

    it('should display value proposition text', () => {
      const valueProposition = fixture.nativeElement.querySelector('p.text-xl');
      expect(valueProposition.textContent.trim()).toContain('Get highly personalized game suggestions');
    });
  });

  describe('UI Elements', () => {
    it('should have responsive navigation bar when not authenticated', () => {
      isAuthenticatedSubject.next(false);
      fixture.detectChanges();
      
      const nav = fixture.nativeElement.querySelector('nav');
      expect(nav.classList.contains('w-full')).toBeTruthy();
      expect(nav.classList.contains('flex')).toBeTruthy();
    });
    
    it('should not show landing page nav when authenticated', () => {
      isAuthenticatedSubject.next(true);
      fixture.detectChanges();
      
      const landingNav = fixture.nativeElement.querySelector('nav:not(app-nav-bar nav)');
      expect(landingNav).toBeNull();
    });
    
    it('should show nav-bar component when authenticated', () => {
      isAuthenticatedSubject.next(true);
      fixture.detectChanges();
      
      const navBarComponent = fixture.nativeElement.querySelector('app-nav-bar');
      expect(navBarComponent).toBeTruthy();
    });

    it('should have hero section with background image', () => {
      const heroSection = fixture.nativeElement.querySelector('.relative.flex-1');
      const backgroundImage = heroSection.querySelector('img[src="assets/images/hero-bg.jpg"]');
      expect(backgroundImage).toBeTruthy();
      expect(backgroundImage.classList.contains('object-cover')).toBeTruthy();
    });

    it('should have animated content container', () => {
      const contentContainer = fixture.nativeElement.querySelector('.animate-fadeIn');
      expect(contentContainer).toBeTruthy();
    });
  });

  describe('Console Logging', () => {
    it('should log initialization message', () => {
      spyOn(console, 'log');
      fixture = TestBed.createComponent(LandingPageComponent);
      expect(console.log).toHaveBeenCalledWith('[LandingPageComponent] Initializing landing page');
    });
  });

  describe('Authentication', () => {
    it('should update isAuthenticated when auth state changes', () => {
      expect(component.isAuthenticated).toBeFalse();
      
      isAuthenticatedSubject.next(true);
      fixture.detectChanges();
      
      expect(component.isAuthenticated).toBeTrue();
    });
    
    it('should clean up subscription on destroy', () => {
      // Create a subscription first
      component.ngOnInit();
      
      // Now spy on the unsubscribe method
      const unsubscribeSpy = spyOn(
        component['authSubscription'] as any,
        'unsubscribe'
      );
      
      component.ngOnDestroy();
      
      expect(unsubscribeSpy).toHaveBeenCalled();
    });
  });
}); 
