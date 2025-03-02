import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { GameLibraryComponent } from './game-library.component';
import { GameLibraryService, GameLibrary } from '../../core/services/game-library.service';
import { of, throwError } from 'rxjs';
import { By } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';

describe('GameLibraryComponent', () => {
  let component: GameLibraryComponent;
  let fixture: ComponentFixture<GameLibraryComponent>;
  let mockService: jasmine.SpyObj<GameLibraryService>;

  beforeEach(async () => {
    const spy = jasmine.createSpyObj('GameLibraryService', ['getGameLibrary']);
    await TestBed.configureTestingModule({
      imports: [ GameLibraryComponent, FormsModule ],
      providers: [
        { provide: GameLibraryService, useValue: spy }
      ]
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
    const dummyLibrary: GameLibrary = {
      libraryId: 1,
      games: [{
          gameId: 42,
          igdbId: 101010,
          title: 'Test Game',
          coverImageUrl: 'http://example.com/cover.jpg'
      }]
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
    const emptyLibrary: GameLibrary = { libraryId: 1, games: [] };
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
}); 
