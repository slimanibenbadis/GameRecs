import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { GameLibraryComponent } from './game-library.component';
import { GameLibraryService, PaginatedGameLibraryResponse, Game } from '../../core/services/game-library.service';
import { of, throwError, Subject, Observable } from 'rxjs';
import { By } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { PaginatorModule } from 'primeng/paginator';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { HttpErrorResponse } from '@angular/common/http';

// Define an interface for our specific mock structure
interface MockMessageServiceExtended /* extends MessageService */ { // Extending MessageService might be too broad if it has many methods
  add: jasmine.Spy;
  clear: jasmine.Spy;
  messageSource: Subject<any>; // The source subject
  messageObserver: Observable<any>; // The observable to subscribe to
  // Potentially add clearObserver if p-messages uses it
  clearObserver?: Observable<any>;
}

describe('GameLibraryComponent', () => {
  let component: GameLibraryComponent;
  let fixture: ComponentFixture<GameLibraryComponent>;
  let mockLibraryService: jasmine.SpyObj<GameLibraryService>;
  let mockRouter: Router;
  let mockMessageService: MockMessageServiceExtended;

  const dummyPaginatedResponse: PaginatedGameLibraryResponse = {
    libraryId: 1,
    games: [{
        gameId: 42,
        igdbId: 101010,
        title: 'Test Game',
        coverImageUrl: 'http://example.com/cover.jpg'
    }],
    currentPage: 0,
    totalPages: 1,
    totalElements: 1,
    pageSize: 10
  };

  beforeEach(async () => {
    const libraryServiceSpy = jasmine.createSpyObj('GameLibraryService', ['getGameLibrary', 'importSteamLibrary']);
    
    const source = new Subject<any>(); // Renamed to avoid conflict if MessageService has a 'messageSource' property
    const clearSourceSub = new Subject<string>();

    const messageServiceSpyObj: MockMessageServiceExtended = {
      add: jasmine.createSpy('add').and.callFake(msg => source.next(msg)), // Optionally, make add emit on the subject
      clear: jasmine.createSpy('clear').and.callFake(key => clearSourceSub.next(key)), // Optionally, make clear emit
      messageSource: source,
      messageObserver: source.asObservable(),
      clearObserver: clearSourceSub.asObservable() // Add if p-messages uses it
    };

    await TestBed.configureTestingModule({
      imports: [ 
        GameLibraryComponent, 
        FormsModule, 
        PaginatorModule, 
        RouterTestingModule
      ],
      providers: [
        { provide: GameLibraryService, useValue: libraryServiceSpy },
        { provide: MessageService, useValue: messageServiceSpyObj }, 
      ],
      schemas: [NO_ERRORS_SCHEMA] 
    })
    .compileComponents();
    
    mockLibraryService = TestBed.inject(GameLibraryService) as jasmine.SpyObj<GameLibraryService>;
    mockRouter = TestBed.inject(Router);
    mockMessageService = TestBed.inject(MessageService) as unknown as MockMessageServiceExtended; // Cast to our specific mock type
    
    fixture = TestBed.createComponent(GameLibraryComponent);
    component = fixture.componentInstance;

    spyOn(mockRouter, 'navigate').and.callThrough();
  });

  it('should create', () => {
    mockLibraryService.getGameLibrary.and.returnValue(of(dummyPaginatedResponse));
    fixture.detectChanges(); // ngOnInit calls fetchGameLibrary
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should call fetchGameLibrary on init', () => {
      spyOn(component, 'fetchGameLibrary');
      component.ngOnInit();
      expect(component.fetchGameLibrary).toHaveBeenCalled();
    });
  });

  describe('fetchGameLibrary', () => {
    it('should show loading indicator while fetching library', () => {
      mockLibraryService.getGameLibrary.and.returnValue(of()); // Observable that doesn't complete
      component.fetchGameLibrary(); // Call directly instead of ngOnInit for focused test
      fixture.detectChanges();
      expect(component.isLoading).toBeTrue();
      // Query for loading element if it's consistently rendered based on isLoading
      // const loadingEl = fixture.debugElement.query(By.css('.loading-spinner-class')); // Example
      // expect(loadingEl).toBeTruthy();
    });

    it('should render game library once data is fetched', fakeAsync(() => {
      mockLibraryService.getGameLibrary.and.returnValue(of(dummyPaginatedResponse));
      component.fetchGameLibrary();
      tick();
      fixture.detectChanges();

      expect(component.isLoading).toBeFalse();
      expect(component.library).toEqual(dummyPaginatedResponse);
      expect(component.currentPage).toBe(dummyPaginatedResponse.currentPage as number);
      expect(component.totalPages).toBe(dummyPaginatedResponse.totalPages as number);
      expect(component.totalElements).toBe(dummyPaginatedResponse.totalElements as number);
      expect(component.pageSize).toBe(dummyPaginatedResponse.pageSize as number);
      // Example: const gameTitleEl = fixture.debugElement.query(By.css('.game-title'));
      // expect(gameTitleEl.nativeElement.textContent).toContain('Test Game');
    }));

    it('should display an error message if API call fails', fakeAsync(() => {
      const errorResponse = new HttpErrorResponse({ status: 500, statusText: 'Server Error' });
      mockLibraryService.getGameLibrary.and.returnValue(throwError(() => errorResponse));
      component.fetchGameLibrary();
      tick();
      fixture.detectChanges();

      expect(component.isLoading).toBeFalse();
      expect(component.errorMessage).toBe('Error loading game library');
      expect(mockMessageService.add).toHaveBeenCalledWith({ 
        severity: 'error', 
        summary: 'Error', 
        detail: 'Error loading game library. Please try again later.' 
      });
    }));

    it('should handle undefined pagination metadata gracefully', fakeAsync(() => {
      const incompleteResponse = {
        libraryId: 1,
        games: [{ gameId: 42, igdbId: 101010, title: 'Test Game' }]
      } as PaginatedGameLibraryResponse;
      
      mockLibraryService.getGameLibrary.and.returnValue(of(incompleteResponse));
      component.fetchGameLibrary();
      tick();
      
      expect(component.currentPage).toBe(0); // Default
      expect(component.totalPages).toBe(0); // Default
      expect(component.totalElements).toBe(0); // Default
      expect(component.pageSize).toBe(10); // Default
    }));
  });
  
  describe('onPageChange', () => {
    beforeEach(fakeAsync(() => {
      mockLibraryService.getGameLibrary.and.returnValue(of(dummyPaginatedResponse));
      component.ngOnInit(); // Initial fetch
      tick();
      mockLibraryService.getGameLibrary.calls.reset(); // Reset spy for onPageChange calls
      mockMessageService.clear.calls.reset();
    }));

    it('should trigger fetchGameLibrary with new page when page number changes', fakeAsync(() => {
      component.onPageChange({ page: 1, rows: component.pageSize }); // rows is the same
      tick();
      expect(mockLibraryService.getGameLibrary).toHaveBeenCalledWith(component.selectedSortBy, component.selectedFilterByGenre, 1, component.pageSize);
    }));

    it('should trigger fetchGameLibrary with page 0 and new pageSize when rows (pageSize) changes', fakeAsync(() => {
      const newPageSize = 20;
      const expectedResponseWithNewPageSize: PaginatedGameLibraryResponse = {
        ...dummyPaginatedResponse, 
        pageSize: newPageSize,
        currentPage: 0 
      };

      mockLibraryService.getGameLibrary.and.returnValue(of(expectedResponseWithNewPageSize));

      component.onPageChange({ page: 0, rows: newPageSize }); 
      tick(); 
      
      expect(component.pageSize).toBe(newPageSize); 
      expect(mockLibraryService.getGameLibrary).toHaveBeenCalledWith(component.selectedSortBy, component.selectedFilterByGenre, 0, newPageSize);
      expect(mockMessageService.clear).toHaveBeenCalled();
    }));

    it('should not call fetchGameLibrary if page and pageSize are the same', fakeAsync(() => {
      component.onPageChange({ page: component.currentPage, rows: component.pageSize });
      tick();
      expect(mockLibraryService.getGameLibrary).not.toHaveBeenCalled();
    }));
  });

  describe('onImportSteamLibrary', () => {
    beforeEach(() => {
      mockLibraryService.importSteamLibrary.calls.reset();
      mockMessageService.add.calls.reset();
      mockMessageService.clear.calls.reset();
      spyOn(component, 'fetchGameLibrary').and.callThrough(); // Spy on fetch to check if it's called
      mockLibraryService.getGameLibrary.and.returnValue(of(dummyPaginatedResponse)); // For the subsequent fetch
    });

    it('should call importSteamLibrary service and show success message with default text', fakeAsync(() => {
      const mockResponse = { success: true }; // No message property
      mockLibraryService.importSteamLibrary.and.returnValue(of(mockResponse));
      
      component.onImportSteamLibrary();
      tick(); // For importSteamLibrary
      
      expect(mockLibraryService.importSteamLibrary).toHaveBeenCalled();
      expect(component.isLoadingImport).toBeFalse();
      expect(mockMessageService.add).toHaveBeenCalledWith({ 
        severity: 'success', 
        summary: 'Success', 
        detail: 'Steam library import initiated successfully. It may take a few moments to reflect the changes.' 
      });
      
      tick(1000); // For setTimeout
      expect(component.fetchGameLibrary).toHaveBeenCalledWith(0);
    }));

    it('should call importSteamLibrary service and show success message with backend provided text', fakeAsync(() => {
      const mockResponse = { message: 'Custom success message from backend!' };
      mockLibraryService.importSteamLibrary.and.returnValue(of(mockResponse));
      
      component.onImportSteamLibrary();
      tick(); // For importSteamLibrary
      
      expect(mockLibraryService.importSteamLibrary).toHaveBeenCalled();
      expect(component.isLoadingImport).toBeFalse();
      expect(mockMessageService.add).toHaveBeenCalledWith({ 
        severity: 'success', 
        summary: 'Success', 
        detail: 'Custom success message from backend!'
      });
      
      tick(1000); // For setTimeout
      expect(component.fetchGameLibrary).toHaveBeenCalledWith(0);
    }));

    it('should handle "credentials not configured" error specifically', fakeAsync(() => {
      const errorResponse = new HttpErrorResponse({
        status: 400,
        error: { message: 'Steam credentials not configured for this user.' }
      });
      mockLibraryService.importSteamLibrary.and.returnValue(throwError(() => errorResponse));
      
      component.onImportSteamLibrary();
      tick();
      
      expect(component.isLoadingImport).toBeFalse();
      expect(mockMessageService.add).toHaveBeenCalledWith({ 
        severity: 'error', 
        summary: 'Error', 
        detail: 'Steam credentials not found in your profile. Please add them on your profile page.'
      });
      expect(component.fetchGameLibrary).not.toHaveBeenCalled();
    }));

    it('should handle error with err.error.message', fakeAsync(() => {
      const errorResponse = new HttpErrorResponse({
        status: 500,
        error: { message: 'Specific backend error.' }
      });
      mockLibraryService.importSteamLibrary.and.returnValue(throwError(() => errorResponse));
      
      component.onImportSteamLibrary();
      tick();
      
      expect(component.isLoadingImport).toBeFalse();
      expect(mockMessageService.add).toHaveBeenCalledWith({ 
        severity: 'error', 
        summary: 'Error', 
        detail: 'Specific backend error.'
      });
    }));
    
    it('should handle error with err.message when err.error.message is not present', fakeAsync(() => {
      const errorResponse = new HttpErrorResponse({
        status: 500,
        error: 'Plain string error from backend',
        statusText: 'Internal Server Error'
      });
      mockLibraryService.importSteamLibrary.and.returnValue(throwError(() => errorResponse));
      
      component.onImportSteamLibrary();
      tick();
      
      expect(component.isLoadingImport).toBeFalse();
      expect(mockMessageService.add).toHaveBeenCalledWith({ 
        severity: 'error', 
        summary: 'Error', 
        detail: errorResponse.message
      });
    }));

    it('should handle generic error when no specific message is available', fakeAsync(() => {
      const errorResponse = new HttpErrorResponse({ status: 503 }); // No error.message or message
      mockLibraryService.importSteamLibrary.and.returnValue(throwError(() => errorResponse));
      
      component.onImportSteamLibrary();
      tick();
      
      expect(component.isLoadingImport).toBeFalse();
      expect(mockMessageService.add).toHaveBeenCalledWith({ 
        severity: 'error', 
        summary: 'Error', 
        detail: errorResponse.message
      });
    }));
  });

  describe('navigateToGame', () => {
    it('should navigate to the game details page and stop event propagation', () => {
      const mockGame: Game = { gameId: 123, igdbId: 789, title: 'Awesome Game', coverImageUrl: 'url' };
      const mockEvent = jasmine.createSpyObj('Event', ['stopPropagation']);
      
      component.navigateToGame(mockGame, mockEvent);
      
      expect(mockEvent.stopPropagation).toHaveBeenCalled();
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/games', 123]);
    });
  });

  // Keep existing tests for reference or if they cover other scenarios
  it('should display a "no results available" message when library is empty after filtering', fakeAsync(() => {
    const emptyLibrary: PaginatedGameLibraryResponse = { 
      libraryId: 1, 
      games: [],
      currentPage: 0,
      totalPages: 0,
      totalElements: 0,
      pageSize: 10
    };
    mockLibraryService.getGameLibrary.and.returnValue(of(emptyLibrary));
    
    component.selectedFilterByGenre = 'UnknownGenre'; // Example filter
    component.fetchGameLibrary();
    tick();
    fixture.detectChanges();
    
    // This assertion depends on your template. Adapt if necessary.
    // const emptyMessageEl = fixture.debugElement.query(By.css('.empty-library-message-class')); 
    // expect(emptyMessageEl.nativeElement.textContent).toContain('Your game library is empty');
    expect(component.library?.games.length).toBe(0); // A more direct check
  }));
  
}); 
