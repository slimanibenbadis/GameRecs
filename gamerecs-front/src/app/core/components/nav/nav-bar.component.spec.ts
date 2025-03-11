import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { BehaviorSubject } from 'rxjs';
import { NavBarComponent } from './nav-bar.component';
import { AuthService } from '../../services/auth.service';
import { ButtonModule } from 'primeng/button';
import { MenubarModule } from 'primeng/menubar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('NavBarComponent', () => {
  let component: NavBarComponent;
  let fixture: ComponentFixture<NavBarComponent>;
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
  });

  it('should call logout when logout button is clicked', () => {
    isAuthenticatedSubject.next(true);
    fixture.detectChanges();
    
    const logoutButton = fixture.nativeElement.querySelector('p-button');
    logoutButton.click();
    
    expect(authService.logout).toHaveBeenCalled();
  });

  it('should clean up subscription on destroy', () => {
    const unsubscribeSpy = spyOn(
      component['authSubscription'] as any,
      'unsubscribe'
    );
    
    component.ngOnDestroy();
    
    expect(unsubscribeSpy).toHaveBeenCalled();
  });
}); 
