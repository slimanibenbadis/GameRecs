import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { GameLibraryComponent } from './game-library.component';
import { GameLibraryService, PaginatedGameLibraryResponse } from '../../core/services/game-library.service';
import { of, throwError } from 'rxjs';
import { By } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { PaginatorModule } from 'primeng/paginator';
import { NO_ERRORS_SCHEMA } from '@angular/core';

describe('GameLibraryComponent', () => {
  let component: GameLibraryComponent;
  let fixture: ComponentFixture<GameLibraryComponent>;
  let mockService: jasmine.SpyObj<GameLibraryService>;

  beforeEach(async () => {
    const spy = jasmine.createSpyObj('GameLibraryService', ['getGameLibrary']);
    await TestBed.configureTestingModule({
      imports: [ GameLibraryComponent, FormsModule, PaginatorModule ],
      providers: [
        { provide: GameLibraryService, useValue: spy }
      ],
      schemas: [NO_ERRORS_SCHEMA] // To handle PrimeNG components without errors
    })
    .compileComponents();
    
    mockService = TestBed.inject(GameLibraryService) as jasmine.SpyObj<GameLibraryService>;
    fixture = TestBed.createComponent(GameLibraryComponent);
    component = fixture.componentInstance;
  });

  it('should show loading indicator while fetching library', () => {
    // Arrange: make the service return an observable that never completes
    mockService.getGameLibrary.and.returnValue(of().pipe());
    
    // Act: trigger ngOnInit
    component.ngOnInit();
    fixture.detectChanges();

    // Assert: loading indicator should be present
    const loadingEl = fixture.debugElement.query(By.css('.loading'));
    expect(loadingEl).toBeTruthy();
  });

  it('should render game library once data is fetched', fakeAsync(() => {
    const dummyLibrary: PaginatedGameLibraryResponse = {
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

    mockService.getGameLibrary.and.returnValue(of(dummyLibrary));
    component.ngOnInit();
    tick();
    fixture.detectChanges();

    // loading should be false and game list should be rendered
    expect(component.isLoading).toBeFalse();
    const gameTitleEl = fixture.debugElement.query(By.css('.game-title'));
    expect(gameTitleEl.nativeElement.textContent).toContain('Test Game');
  }));

  it('should display a "no results available" message when library is empty after filtering', fakeAsync(() => {
    // Simulate backend returning an empty library
    const emptyLibrary: PaginatedGameLibraryResponse = { 
      libraryId: 1, 
      games: [],
      currentPage: 0,
      totalPages: 0,
      totalElements: 0,
      pageSize: 10
    };
    mockService.getGameLibrary.and.returnValue(of(emptyLibrary));
    
    // Set filtering option that yields no results
    component.selectedFilterByGenre = 'UnknownGenre';
    component.fetchGameLibrary();
    tick();
    fixture.detectChanges();

    const emptyMessageEl = fixture.debugElement.query(By.css('.text-center'));
    expect(emptyMessageEl.nativeElement.textContent).toContain('Your game library is empty');
  }));

  it('should display an error message if API call fails', fakeAsync(() => {
    mockService.getGameLibrary.and.returnValue(throwError(() => ({ status: 404 })));
    component.ngOnInit();
    tick();
    fixture.detectChanges();

    expect(component.isLoading).toBeFalse();
    const errorEl = fixture.debugElement.query(By.css('.error'));
    expect(errorEl.nativeElement.textContent).toContain('Error loading game library');
  }));

  it('should trigger fetchGameLibrary with new page when pagination control is used', fakeAsync(() => {
    // Arrange
    const dummyLibrary = {
      libraryId: 1,
      games: [{ gameId: 42, igdbId: 101010, title: 'Test Game' }],
      currentPage: 0,
      totalPages: 3,
      totalElements: 25,
      pageSize: 10
    };
    mockService.getGameLibrary.and.returnValue(of(dummyLibrary));
    
    // Initialize component
    component.ngOnInit();
    tick();
    
    // Reset the spy to track the next call
    mockService.getGameLibrary.calls.reset();
    
    // Act: simulate page change event from PrimeNG paginator
    component.onPageChange({ page: 1, rows: 10 });
    tick();

    // Assert: ensure that getGameLibrary has been called with page = 1
    expect(mockService.getGameLibrary).toHaveBeenCalledWith('title', '', 1, 10);
    expect(component.currentPage).toEqual(0); // Will be updated after service response
  }));

  it('should update pagination metadata when receiving paginated response', fakeAsync(() => {
    // Arrange
    const paginatedResponse: PaginatedGameLibraryResponse = {
      libraryId: 1,
      games: [{ gameId: 42, igdbId: 101010, title: 'Test Game' }],
      currentPage: 2,
      totalPages: 5,
      totalElements: 45,
      pageSize: 10
    };
    mockService.getGameLibrary.and.returnValue(of(paginatedResponse));
    
    // Act
    component.fetchGameLibrary(2);
    tick();
    
    // Assert
    expect(component.currentPage).toBe(2);
    expect(component.totalPages).toBe(5);
    expect(component.totalElements).toBe(45);
    expect(component.pageSize).toBe(10);
  }));

  it('should handle undefined pagination metadata gracefully', fakeAsync(() => {
    // Arrange: create a response with missing pagination metadata
    const incompleteResponse = {
      libraryId: 1,
      games: [{ gameId: 42, igdbId: 101010, title: 'Test Game' }]
    } as PaginatedGameLibraryResponse;
    
    mockService.getGameLibrary.and.returnValue(of(incompleteResponse));
    
    // Act
    component.fetchGameLibrary();
    tick();
    
    // Assert: should use default values
    expect(component.currentPage).toBe(0);
    expect(component.totalPages).toBe(0);
    expect(component.totalElements).toBe(0);
    expect(component.pageSize).toBe(10);
  }));
}); 
