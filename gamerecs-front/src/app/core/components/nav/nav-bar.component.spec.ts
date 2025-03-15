import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { BehaviorSubject } from 'rxjs';
import { NavBarComponent } from './nav-bar.component';
import { AuthService } from '../../services/auth.service';
import { ButtonModule } from 'primeng/button';
import { MenubarModule } from 'primeng/menubar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';

describe('NavBarComponent', () => {
  let component: NavBarComponent;
  let fixture: ComponentFixture<NavBarComponent>;
  let authService: jasmine.SpyObj<AuthService>;
  let router: Router;
  let isAuthenticatedSubject: BehaviorSubject<boolean>;

  beforeEach(async () => {
    isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
    authService = jasmine.createSpyObj('AuthService', ['logout'], {
      isAuthenticated$: isAuthenticatedSubject.asObservable()
    });

    await TestBed.configureTestingModule({
      imports: [
        RouterTestingModule.withRoutes([
          { path: 'auth/login', component: class {} },
          { path: 'library', component: class {} },
          { path: 'profile', component: class {} }
        ]),
        HttpClientTestingModule,
        ButtonModule,
        MenubarModule,
        NoopAnimationsModule,
        NavBarComponent
      ],
      providers: [
        { provide: AuthService, useValue: authService }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));

    fixture = TestBed.createComponent(NavBarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should not display navigation when user is not authenticated', () => {
    isAuthenticatedSubject.next(false);
    fixture.detectChanges();
    
    const navElement = fixture.nativeElement.querySelector('nav');
    expect(navElement).toBeNull();
  });

  it('should display navigation when user is authenticated', () => {
    isAuthenticatedSubject.next(true);
    fixture.detectChanges();
    
    const navElement = fixture.nativeElement.querySelector('nav');
    expect(navElement).toBeTruthy();
  });

  it('should initialize menu items when authenticated', () => {
    isAuthenticatedSubject.next(true);
    fixture.detectChanges();
    
    expect(component.menuItems.length).toBe(5);
    expect(component.menuItems.map(item => item.label)).toEqual([
      'Home',
      'Game Library',
      'Recommendations',
      'Backlog',
      'Profile'
    ]);
    
    // Check that unimplemented pages use empty string as routerLink
    expect(component.menuItems[0].routerLink).toBe('');
    expect(component.menuItems[1].routerLink).toBe('/library');
    expect(component.menuItems[2].routerLink).toBe('');
    expect(component.menuItems[3].routerLink).toBe('');
    expect(component.menuItems[4].routerLink).toBe('/profile');
  });

  it('should call logout and redirect to login page when logout button is clicked', () => {
    isAuthenticatedSubject.next(true);
    fixture.detectChanges();
    
    component.logout();
    
    expect(authService.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/auth/login']);
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
