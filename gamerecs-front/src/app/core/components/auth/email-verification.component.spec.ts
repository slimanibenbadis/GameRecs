import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { EmailVerificationComponent } from './email-verification.component';
import { ActivatedRoute, Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { AuthService } from '../../services/auth.service';
import { of, throwError } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

// PrimeNG Imports
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';

describe('EmailVerificationComponent', () => {
  let component: EmailVerificationComponent;
  let fixture: ComponentFixture<EmailVerificationComponent>;
  let authService: jasmine.SpyObj<AuthService>;
  let messageService: jasmine.SpyObj<MessageService>;
  let router: jasmine.SpyObj<Router>;
  let activatedRoute: { queryParams: any };

  beforeEach(async () => {
    const authServiceSpy = jasmine.createSpyObj('AuthService', ['verifyEmail']);
    const messageServiceSpy = jasmine.createSpyObj('MessageService', ['add', 'clear']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    activatedRoute = {
      queryParams: of({ token: 'valid-token' })
    };

    await TestBed.configureTestingModule({
      imports: [
        EmailVerificationComponent,
        NoopAnimationsModule,
        RouterTestingModule,
        ProgressSpinnerModule,
        ButtonModule,
        ToastModule
      ],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: MessageService, useValue: messageServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: activatedRoute }
      ]
    }).compileComponents();

    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    messageService = TestBed.inject(MessageService) as jasmine.SpyObj<MessageService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(EmailVerificationComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    fixture.destroy();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should handle successful email verification', fakeAsync(() => {
    const mockResponse = { verified: true, message: 'Your email has been successfully verified!' };
    authService.verifyEmail.and.returnValue(of(mockResponse));
    
    const messageService = fixture.debugElement.injector.get(MessageService);
    spyOn(messageService, 'add');

    fixture.detectChanges(); // Trigger ngOnInit
    tick(); // Wait for async operations
    tick(100); // Wait for setTimeout
    fixture.detectChanges();

    expect(authService.verifyEmail).toHaveBeenCalledWith('valid-token');
    expect(component.loading).toBeFalse();
    expect(component.verified).toBeTrue();
    expect(component.error).toBeFalse();
    expect(messageService.add).toHaveBeenCalledWith({
      key: 'verificationToast',
      severity: 'success',
      summary: 'Success',
      detail: mockResponse.message,
      life: 5000
    });
  }));

  it('should handle verification failure from backend', fakeAsync(() => {
    const mockResponse = { verified: false, message: 'Token expired' };
    authService.verifyEmail.and.returnValue(of(mockResponse));

    const messageService = fixture.debugElement.injector.get(MessageService);
    spyOn(messageService, 'add');
    spyOn(console, 'log');
    
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component.loading).toBeFalse();
    expect(component.verified).toBeFalse();
    expect(component.error).toBeTrue();
    expect(component.errorMessage).toBe(mockResponse.message);
    expect(messageService.add).toHaveBeenCalledWith({
      key: 'verificationToast',
      severity: 'error',
      summary: 'Verification Failed',
      detail: mockResponse.message,
      life: 5000
    });

    // Verify logging
    expect(console.log).toHaveBeenCalledWith(
      '[EmailVerificationComponent] Verification response:',
      mockResponse
    );
    expect(console.log).toHaveBeenCalledWith(
      '[EmailVerificationComponent] Handling error:',
      mockResponse.message
    );
  }));

  it('should handle error during verification', fakeAsync(() => {
    const errorMessage = 'Network error';
    authService.verifyEmail.and.returnValue(throwError(() => new Error(errorMessage)));

    const messageService = fixture.debugElement.injector.get(MessageService);
    spyOn(messageService, 'add');

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component.loading).toBeFalse();
    expect(component.verified).toBeFalse();
    expect(component.error).toBeTrue();
    expect(component.errorMessage).toBe(errorMessage);
    expect(messageService.add).toHaveBeenCalledWith({
      key: 'verificationToast',
      severity: 'error',
      summary: 'Verification Failed',
      detail: errorMessage,
      life: 5000
    });
  }));

  it('should handle missing token', fakeAsync(() => {
    // Reset the component with empty query params
    fixture.destroy();
    activatedRoute.queryParams = of({});
    fixture = TestBed.createComponent(EmailVerificationComponent);
    component = fixture.componentInstance;
    
    const messageService = fixture.debugElement.injector.get(MessageService);
    spyOn(messageService, 'add');
    
    // Trigger initialization and wait for async operations
    fixture.detectChanges();
    tick(0); // Allow queryParams subscription to execute
    fixture.detectChanges();
    
    expect(component.loading).toBeFalse();
    expect(component.error).toBeTrue();
    expect(component.errorMessage).toBe('Invalid verification link. Please request a new one.');
    expect(messageService.add).toHaveBeenCalledWith({
      key: 'verificationToast',
      severity: 'error',
      summary: 'Verification Failed',
      detail: 'Invalid verification link. Please request a new one.',
      life: 5000
    });
  }));

  it('should navigate to registration page on button click', () => {
    component.navigateToAuth();
    expect(router.navigate).toHaveBeenCalledWith(['/auth/login']);
  });
}); 
