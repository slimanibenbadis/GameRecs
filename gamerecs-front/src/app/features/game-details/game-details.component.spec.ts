import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { BehaviorSubject, of, throwError, Subject } from 'rxjs';
import { GameDetailsComponent } from './game-details.component';
import { GameService } from '../../core/services/game.service';
import { GameDto } from '../../models/game.dto';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';

describe('GameDetailsComponent', () => {
  let component: GameDetailsComponent;
  let fixture: ComponentFixture<GameDetailsComponent>;
  let gameService: jasmine.SpyObj<GameService>;
  let activatedRoute: jasmine.SpyObj<ActivatedRoute>;
  let router: jasmine.SpyObj<Router>;
  let paramMap: BehaviorSubject<any>;

  const mockGameDto: GameDto = {
    id: 123,
    title: 'Test Game',
    description: 'This is a test game description',
    releaseDate: '2023-01-01',
    coverImageUrl: 'https://example.com/image.jpg',
    genres: [{ id: 1, name: 'Action' }, { id: 2, name: 'Adventure' }],
    platforms: [{ id: 1, name: 'PC' }, { id: 2, name: 'PlayStation 5' }],
    publishers: [{ id: 1, name: 'Test Publisher' }],
    developers: [{ id: 1, name: 'Test Developer' }],
    userRating: 85,
    percentileRank: 75,
    predictedRating: 80,
    backlogStatus: 'In Progress'
  };

  beforeEach(async () => {
    const gameServiceSpy = jasmine.createSpyObj('GameService', ['getGameById']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    
    // Create a BehaviorSubject for paramMap to allow changing it during tests
    paramMap = new BehaviorSubject(convertToParamMap({ id: '123' }));
    
    // Mock ActivatedRoute with the BehaviorSubject
    const activatedRouteSpy = jasmine.createSpyObj('ActivatedRoute', [], {
      paramMap: paramMap.asObservable()
    });

    await TestBed.configureTestingModule({
      imports: [
        BrowserAnimationsModule,
        GameDetailsComponent
      ],
      providers: [
        { provide: GameService, useValue: gameServiceSpy },
        { provide: ActivatedRoute, useValue: activatedRouteSpy },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    gameService = TestBed.inject(GameService) as jasmine.SpyObj<GameService>;
    activatedRoute = TestBed.inject(ActivatedRoute) as jasmine.SpyObj<ActivatedRoute>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    
    // Set up default success response
    gameService.getGameById.and.returnValue(of(mockGameDto));
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(GameDetailsComponent);
    component = fixture.componentInstance;
    
    // Reset spies before each test
    gameService.getGameById.calls.reset();
    
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Initialization', () => {
    it('should load game details on init', fakeAsync(() => {
      tick();
      
      expect(gameService.getGameById).toHaveBeenCalledWith(123);
      expect(component.game).toEqual(mockGameDto);
      expect(component.loading).toBeFalse();
      expect(component.error).toBeNull();
    }));

    it('should handle invalid game ID string', fakeAsync(() => {
      // Create a new component to avoid interference from previous tests
      gameService.getGameById.calls.reset();
      
      // Update the paramMap with invalid ID before component initialization
      paramMap.next(convertToParamMap({ id: 'invalid' }));
      
      // Re-create the component
      fixture = TestBed.createComponent(GameDetailsComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
      tick();
      
      expect(component.error).toBe('Invalid game ID: must be a positive number');
      expect(component.loading).toBeFalse();
      expect(gameService.getGameById).not.toHaveBeenCalled();
    }));

    it('should handle missing game ID', fakeAsync(() => {
      // Create a new component to avoid interference from previous tests
      gameService.getGameById.calls.reset();
      
      // Update the paramMap with missing ID before component initialization
      paramMap.next(convertToParamMap({}));
      
      // Re-create the component
      fixture = TestBed.createComponent(GameDetailsComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
      tick();
      
      expect(component.error).toBe('Invalid game ID');
      expect(component.loading).toBeFalse();
      expect(gameService.getGameById).not.toHaveBeenCalled();
    }));

    it('should handle negative game ID', fakeAsync(() => {
      // Create a new component to avoid interference from previous tests
      gameService.getGameById.calls.reset();
      
      // Update the paramMap with negative ID before component initialization
      paramMap.next(convertToParamMap({ id: '-5' }));
      
      // Re-create the component
      fixture = TestBed.createComponent(GameDetailsComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
      tick();
      
      expect(component.error).toBe('Invalid game ID: must be a positive number');
      expect(component.loading).toBeFalse();
      expect(gameService.getGameById).not.toHaveBeenCalled();
    }));

    it('should handle zero game ID', fakeAsync(() => {
      // Create a new component to avoid interference from previous tests
      gameService.getGameById.calls.reset();
      
      // Update the paramMap with zero ID before component initialization
      paramMap.next(convertToParamMap({ id: '0' }));
      
      // Re-create the component
      fixture = TestBed.createComponent(GameDetailsComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
      tick();
      
      expect(component.error).toBe('Invalid game ID: must be a positive number');
      expect(component.loading).toBeFalse();
      expect(gameService.getGameById).not.toHaveBeenCalled();
    }));
  });

  describe('Error Handling', () => {
    it('should handle 404 not found error', fakeAsync(() => {
      // Create a new component to avoid interference from previous tests
      gameService.getGameById.calls.reset();
      
      gameService.getGameById.and.returnValue(
        throwError(() => new HttpErrorResponse({
          error: { message: 'Game not found' },
          status: 404,
          statusText: 'Not Found'
        }))
      );
      
      // Re-create the component
      fixture = TestBed.createComponent(GameDetailsComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
      tick();
      
      expect(component.notFound).toBeTrue();
      expect(component.error).toBe('Game with ID 123 could not be found');
      expect(component.loading).toBeFalse();
    }));

    it('should handle network error (status 0)', fakeAsync(() => {
      // Create a new component to avoid interference from previous tests
      gameService.getGameById.calls.reset();
      
      gameService.getGameById.and.returnValue(
        throwError(() => new HttpErrorResponse({
          error: new Error('Network Error'),
          status: 0
        }))
      );
      
      // Re-create the component
      fixture = TestBed.createComponent(GameDetailsComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
      tick();
      
      expect(component.error).toBe('Network error. Please check your internet connection.');
      expect(component.loading).toBeFalse();
      expect(component.notFound).toBeFalse();
    }));

    it('should handle unauthorized error (status 401)', fakeAsync(() => {
      // Create a new component to avoid interference from previous tests
      gameService.getGameById.calls.reset();
      
      gameService.getGameById.and.returnValue(
        throwError(() => new HttpErrorResponse({
          error: { message: 'Unauthorized' },
          status: 401
        }))
      );
      
      // Re-create the component
      fixture = TestBed.createComponent(GameDetailsComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
      tick();
      
      expect(component.error).toBe('You need to be logged in to view this content.');
      expect(component.loading).toBeFalse();
    }));

    it('should handle forbidden error (status 403)', fakeAsync(() => {
      // Create a new component to avoid interference from previous tests  
      gameService.getGameById.calls.reset();
      
      gameService.getGameById.and.returnValue(
        throwError(() => new HttpErrorResponse({
          error: { message: 'Forbidden' },
          status: 403
        }))
      );
      
      // Re-create the component
      fixture = TestBed.createComponent(GameDetailsComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
      tick();
      
      expect(component.error).toBe('You don\'t have permission to access this content.');
      expect(component.loading).toBeFalse();
    }));

    it('should handle server error (status 500) and retry', fakeAsync(() => {
      // Create a new component to avoid interference from previous tests
      gameService.getGameById.calls.reset();
      
      gameService.getGameById.and.returnValue(
        throwError(() => new HttpErrorResponse({
          error: { message: 'Internal Server Error' },
          status: 500
        }))
      );
      
      // Re-create the component
      fixture = TestBed.createComponent(GameDetailsComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
      tick();
      
      expect(component.error).toBe('Server error. Please try again later.');
      expect(component.loading).toBeFalse();
      expect(component.retryCount).toBe(1);
      
      // Clear calls to start fresh for the retry count
      gameService.getGameById.calls.reset();
      
      // Second attempt should happen after 2 seconds
      tick(2000);
      expect(gameService.getGameById).toHaveBeenCalledTimes(1);
    }));

    it('should not retry more than maxRetries times', fakeAsync(() => {
      // Create a new component to avoid interference from previous tests
      gameService.getGameById.calls.reset();
      
      gameService.getGameById.and.returnValue(
        throwError(() => new HttpErrorResponse({
          error: { message: 'Internal Server Error' },
          status: 500
        }))
      );
      
      // Re-create the component with maxRetries set to 0
      fixture = TestBed.createComponent(GameDetailsComponent);
      component = fixture.componentInstance;
      component.maxRetries = 0; // No retries allowed
      fixture.detectChanges();
      tick();
      
      expect(component.error).toBe('Server error. Please try again later.');
      expect(component.loading).toBeFalse();
      
      // Clear calls to start fresh for checking if retry happens
      gameService.getGameById.calls.reset();
      
      // Wait for potential retry
      tick(2000);
      expect(gameService.getGameById).not.toHaveBeenCalled();
    }));

    it('should handle error with custom API message', fakeAsync(() => {
      // Create a new component to avoid interference from previous tests
      gameService.getGameById.calls.reset();
      
      gameService.getGameById.and.returnValue(
        throwError(() => new HttpErrorResponse({
          error: { message: 'Custom API error message' },
          status: 400
        }))
      );
      
      // Re-create the component
      fixture = TestBed.createComponent(GameDetailsComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
      tick();
      
      expect(component.error).toBe('Custom API error message');
      expect(component.loading).toBeFalse();
    }));

    it('should call loadGameDetails when retryLoading is called', () => {
      // Create a fresh component
      fixture = TestBed.createComponent(GameDetailsComponent);
      component = fixture.componentInstance;
      
      // Spy on the loadGameDetails method
      spyOn(component, 'loadGameDetails');
      
      // Call retryLoading
      component.retryLoading();
      
      // Verify loadGameDetails was called
      expect(component.loadGameDetails).toHaveBeenCalled();
    });
    
    it('should set loading state in loadGameDetails', () => {
      // Create a fresh component
      fixture = TestBed.createComponent(GameDetailsComponent);
      component = fixture.componentInstance;
      component.gameId = 123; // Required for the service call
      
      // Spy on the GameService but don't immediately return a value
      // We want to verify the loading state BEFORE the service resolves
      const serviceDeferred = new Subject<GameDto>();
      gameService.getGameById.and.returnValue(serviceDeferred.asObservable());
      
      // Set initial state and call method
      component.loading = false;
      component.loadGameDetails();
      
      // Verify loading state was set to true
      expect(component.loading).toBeTrue();
      
      // Now complete the service call
      serviceDeferred.next(mockGameDto);
      serviceDeferred.complete();
      
      // After service resolves, loading should be false again
      expect(component.loading).toBeFalse();
    });
  });

  describe('Image Handling', () => {
    it('should handle image errors and set fallback image', () => {
      const imgElement = document.createElement('img');
      component.handleImageError({ target: imgElement } as unknown as Event);
      expect(imgElement.src).toContain('assets/placeholders/game-poster.svg');
      expect(imgElement.alt).toBe('Game cover placeholder');
    });
  });

  describe('Screen Size Detection', () => {
    it('should set screenSize to sm for small screens', () => {
      spyOnProperty(window, 'innerWidth').and.returnValue(500);
      component.checkScreenSize();
      expect(component.screenSize).toBe('sm');
    });

    it('should set screenSize to md for medium screens', () => {
      spyOnProperty(window, 'innerWidth').and.returnValue(800);
      component.checkScreenSize();
      expect(component.screenSize).toBe('md');
    });

    it('should set screenSize to lg for large screens', () => {
      spyOnProperty(window, 'innerWidth').and.returnValue(1200);
      component.checkScreenSize();
      expect(component.screenSize).toBe('lg');
    });
  });

  describe('Backlog Status Methods', () => {
    it('should return correct severity for To Play status', () => {
      expect(component.getBacklogStatusSeverity('To Play')).toBe('info');
    });

    it('should return correct severity for In Progress status', () => {
      expect(component.getBacklogStatusSeverity('In Progress')).toBe('warn');
    });

    it('should return correct severity for Completed status', () => {
      expect(component.getBacklogStatusSeverity('Completed')).toBe('success');
    });

    it('should return correct severity for Abandoned status', () => {
      expect(component.getBacklogStatusSeverity('Abandoned')).toBe('danger');
    });

    it('should return info as default severity for null status', () => {
      expect(component.getBacklogStatusSeverity(null)).toBe('info');
    });

    it('should return info as default severity for unknown status', () => {
      expect(component.getBacklogStatusSeverity('Unknown Status' as any)).toBe('info');
    });

    it('should return correct description for To Play status', () => {
      expect(component.getBacklogStatusDescription('To Play')).toBe('You\'ve saved this game to play later');
    });

    it('should return correct description for In Progress status', () => {
      expect(component.getBacklogStatusDescription('In Progress')).toBe('You\'re currently playing this game');
    });

    it('should return correct description for Completed status', () => {
      expect(component.getBacklogStatusDescription('Completed')).toBe('You\'ve finished playing this game');
    });

    it('should return correct description for Abandoned status', () => {
      expect(component.getBacklogStatusDescription('Abandoned')).toBe('You\'ve stopped playing this game');
    });

    it('should return empty string as description for null status', () => {
      expect(component.getBacklogStatusDescription(null)).toBe('');
    });

    it('should return empty string as description for unknown status', () => {
      expect(component.getBacklogStatusDescription('Unknown Status' as any)).toBe('');
    });

    it('should return correct border color for To Play status', () => {
      expect(component.getBacklogBorderColor('To Play')).toBe('blue-400');
    });

    it('should return correct border color for In Progress status', () => {
      expect(component.getBacklogBorderColor('In Progress')).toBe('yellow-400');
    });

    it('should return correct border color for Completed status', () => {
      expect(component.getBacklogBorderColor('Completed')).toBe('green-400');
    });

    it('should return correct border color for Abandoned status', () => {
      expect(component.getBacklogBorderColor('Abandoned')).toBe('red-400');
    });

    it('should return gray-500 as border color for null status', () => {
      expect(component.getBacklogBorderColor(null)).toBe('gray-500');
    });

    it('should return gray-500 as border color for unknown status', () => {
      expect(component.getBacklogBorderColor('Unknown Status' as any)).toBe('gray-500');
    });
  });

  describe('PRI Rating Methods', () => {
    it('should return correct class for PRI rating >= 80', () => {
      expect(component.getPriRatingClass(85)).toBe('text-green-400');
    });

    it('should return correct class for PRI rating >= 60 and < 80', () => {
      expect(component.getPriRatingClass(75)).toBe('text-blue-400');
    });

    it('should return correct class for PRI rating >= 40 and < 60', () => {
      expect(component.getPriRatingClass(45)).toBe('text-yellow-400');
    });

    it('should return correct class for PRI rating < 40', () => {
      expect(component.getPriRatingClass(35)).toBe('text-red-400');
    });

    it('should return text-gray-500 class for null rating', () => {
      expect(component.getPriRatingClass(null)).toBe('text-gray-500');
    });

    it('should return correct percentage for PRI rating', () => {
      expect(component.getPriPercentage(75)).toBe(75);
    });

    it('should return 0 for null PRI rating percentage', () => {
      expect(component.getPriPercentage(null)).toBe(0);
    });
  });
}); 
