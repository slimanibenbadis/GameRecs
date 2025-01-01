import { TestBed } from '@angular/core/testing';
import { AppComponent } from './app.component';

describe('AppComponent', () => {
  let mediaQuery: { matches: boolean; addEventListener: Function; removeEventListener: Function };
  let eventListener: Function;

  beforeEach(async () => {
    // Reset DOM state
    document.documentElement.classList.remove('dark');

    // Setup mock
    mediaQuery = {
      matches: false,
      addEventListener: (event: string, listener: Function) => {
        eventListener = listener;
      },
      removeEventListener: () => {}
    };

    // Mock window.matchMedia
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: () => mediaQuery
    });

    await TestBed.configureTestingModule({
      imports: [AppComponent],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should apply dark mode class when system preference is dark', () => {
    mediaQuery.matches = true;
    
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    expect(document.documentElement.classList.contains('dark')).toBeTruthy();
  });

  it('should handle system preference changes for dark mode', () => {
    mediaQuery.matches = false;
    
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    expect(document.documentElement.classList.contains('dark')).toBeFalsy();

    // Simulate change to dark mode
    eventListener({ matches: true });
    expect(document.documentElement.classList.contains('dark')).toBeTruthy();

    // Simulate change back to light mode
    eventListener({ matches: false });
    expect(document.documentElement.classList.contains('dark')).toBeFalsy();
  });
});
