import { ComponentFixture, TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { SearchModalComponent } from './search-modal.component';
import { GameService, GameSearchResponse } from '../../services/game.service';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PaginatorModule } from 'primeng/paginator';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { NO_ERRORS_SCHEMA, ElementRef } from '@angular/core';
import { of, throwError } from 'rxjs';
import { MessageService } from 'primeng/api';
import { Game } from '../../services/game-library.service';
import { Router } from '@angular/router';

describe('SearchModalComponent', () => {
  let component: SearchModalComponent;
  let fixture: ComponentFixture<SearchModalComponent>;
  let mockGameService: jasmine.SpyObj<GameService>;
  let mockRouter: jasmine.SpyObj<Router>;
  
  const mockSearchResponse: GameSearchResponse = {
    games: [
      { gameId: 1, igdbId: 101, title: 'Test Game 1', coverImageUrl: 'http://example.com/cover1.jpg', releaseDate: '2023-01-01' },
      { gameId: 2, igdbId: 102, title: 'Test Game 2', coverImageUrl: 'http://example.com/cover2.jpg' }
    ],
    currentPage: 0,
    totalPages: 1,
    totalElements: 2,
    pageSize: 50,
    query: 'test'
  };

  beforeEach(async () => {
    mockGameService = jasmine.createSpyObj('GameService', [
      'searchGames', 
      'updateAndSearch', 
      'triggerIgdbUpdate'
    ]);

    await TestBed.configureTestingModule({
      imports: [
        CommonModule,
        ReactiveFormsModule,
        ButtonModule,
        InputTextModule,
        PaginatorModule,
        ProgressSpinnerModule,
        SearchModalComponent
      ],
      providers: [
        { provide: GameService, useValue: mockGameService },
        MessageService,
        { provide: Router, useValue: { navigate: jasmine.createSpy() } as jasmine.SpyObj<Router> },
      ],
      schemas: [NO_ERRORS_SCHEMA] // For any unrecognized elements
    }).compileComponents();

    fixture = TestBed.createComponent(SearchModalComponent);
    component = fixture.componentInstance;
    mockRouter = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Lifecycle methods', () => {
    it('should initialize correctly', () => {
      component.ngOnInit();
      expect(component.isVisible).toBeFalse();
      expect(component.searchControl).toBeInstanceOf(FormControl);
      expect(component.searchResults).toEqual([]);
      expect(component.isLoading).toBeFalse();
      expect(component.errorMessage).toBeNull();
      expect(component.showError).toBeFalse();
    });

    it('should clean up subscriptions on destroy', () => {
      spyOn(component['destroy$'], 'next');
      spyOn(component['destroy$'], 'complete');
      
      component.ngOnDestroy();
      
      expect(component['destroy$'].next).toHaveBeenCalled();
      expect(component['destroy$'].complete).toHaveBeenCalled();
    });
  });

  describe('Search functionality', () => {
    it('should setup search listener on init', fakeAsync(() => {
      // Arrange
      mockGameService.updateAndSearch.and.returnValue(of(mockSearchResponse));

      // Act
      component.ngOnInit();
      component.searchControl.setValue('test');
      tick(300); // Wait for debounce time
      
      // Assert
      expect(mockGameService.updateAndSearch).toHaveBeenCalledWith('test', 0, 50);
      expect(component.currentQuery).toBe('test');
      expect(component.searchResults).toEqual(mockSearchResponse.games);
      expect(component.currentPage).toBe(0);
      expect(component.totalPages).toBe(1);
      expect(component.totalElements).toBe(2);
      expect(component.isLoading).toBeFalse();
    }));

    it('should not search if query is too short', fakeAsync(() => {
      // Arrange
      component.ngOnInit();
      
      // Act
      component.searchControl.setValue('t');
      tick(300); // Wait for debounce time
      
      // Assert
      expect(mockGameService.updateAndSearch).not.toHaveBeenCalled();
      expect(component.searchResults).toEqual([]);
      expect(component.currentPage).toBe(0);
      expect(component.totalPages).toBe(0);
      expect(component.totalElements).toBe(0);
    }));

    it('should clear results when query is empty', fakeAsync(() => {
      // Arrange
      component.searchResults = [{ gameId: 1, igdbId: 101, title: 'Test Game' }];
      component.totalElements = 1;
      component.ngOnInit();
      
      // Act
      component.searchControl.setValue('');
      tick(300); // Wait for debounce time
      
      // Assert
      expect(mockGameService.updateAndSearch).not.toHaveBeenCalled();
      expect(component.searchResults).toEqual([]);
      expect(component.totalElements).toBe(0);
    }));

    it('should sanitize search query', fakeAsync(() => {
      // Arrange
      mockGameService.updateAndSearch.and.returnValue(of(mockSearchResponse));
      component.ngOnInit();
      
      // Act
      component.searchControl.setValue('test<script>');
      tick(300); // Wait for debounce time
      
      // Assert
      expect(mockGameService.updateAndSearch).toHaveBeenCalledWith('testscript', 0, 50);
      expect(component.currentQuery).toBe('testscript');
    }));

    it('should handle page changes', () => {
      // Arrange
      component.currentQuery = 'test';
      mockGameService.searchGames.and.returnValue(of({
        ...mockSearchResponse,
        currentPage: 1
      }));
      
      // Act
      component.onPageChange({ page: 1, rows: 50 });
      
      // Assert
      expect(mockGameService.searchGames).toHaveBeenCalledWith('test', 1, 50);
    });
  });

  describe('Search API interactions', () => {
    it('should use updateAndSearch for first page and searchGames for pagination', fakeAsync(() => {
      // Arrange
      mockGameService.updateAndSearch.and.returnValue(of(mockSearchResponse));
      mockGameService.searchGames.and.returnValue(of({
        ...mockSearchResponse,
        currentPage: 1
      }));
      
      // Act - First search with page 0
      component.ngOnInit();
      component.searchControl.setValue('test');
      tick(300);
      
      // Assert - Should use updateAndSearch for first page
      expect(mockGameService.updateAndSearch).toHaveBeenCalledWith('test', 0, 50);
      expect(mockGameService.searchGames).not.toHaveBeenCalled();
      
      // Act - Page change
      component.onPageChange({ page: 1, rows: 50 });
      
      // Assert - Should use searchGames for pagination
      expect(mockGameService.searchGames).toHaveBeenCalledWith('test', 1, 50);
    }));

    it('should fall back to regular search when updateAndSearch fails', fakeAsync(() => {
      // Arrange
      mockGameService.updateAndSearch.and.returnValue(
        throwError(() => new Error('API error'))
      );
      mockGameService.searchGames.and.returnValue(of(mockSearchResponse));
      mockGameService.triggerIgdbUpdate.and.returnValue(of({ message: 'Update initiated', data: [] }));
      
      // Act
      component.ngOnInit();
      component.searchControl.setValue('test');
      tick(300);
      
      // Assert
      expect(mockGameService.updateAndSearch).toHaveBeenCalledWith('test', 0, 50);
      expect(mockGameService.searchGames).toHaveBeenCalledWith('test', 0, 50);
      expect(mockGameService.triggerIgdbUpdate).toHaveBeenCalledWith('test');
      expect(component.searchResults).toEqual(mockSearchResponse.games);
      expect(component.errorMessage).toBe('IGDB update failed. Showing local results only.');
      expect(component.showError).toBeTrue();
    }));

    it('should handle error during fallback search', fakeAsync(() => {
      // Arrange
      mockGameService.updateAndSearch.and.returnValue(
        throwError(() => new Error('API error'))
      );
      mockGameService.searchGames.and.returnValue(
        throwError(() => ({ status: 400 }))
      );
      
      // Act
      component.ngOnInit();
      component.searchControl.setValue('test');
      tick(300);
      
      // Assert
      expect(mockGameService.updateAndSearch).toHaveBeenCalledWith('test', 0, 50);
      expect(mockGameService.searchGames).toHaveBeenCalledWith('test', 0, 50);
      expect(component.isLoading).toBeFalse();
      expect(component.errorMessage).toBe('Invalid search query. Please try a different search term.');
      expect(component.showError).toBeTrue();
    }));

    it('should handle service unavailable error during search', fakeAsync(() => {
      // Arrange
      mockGameService.updateAndSearch.and.returnValue(
        throwError(() => ({ status: 503 }))
      );
      // Important: Always mock the fallback search method too
      mockGameService.searchGames.and.returnValue(of(mockSearchResponse));
      mockGameService.triggerIgdbUpdate.and.returnValue(of({ message: 'Update initiated', data: [] }));
      
      // Act
      component.ngOnInit();
      component.searchControl.setValue('test');
      tick(300);
      
      // Assert
      expect(component.errorMessage).toBe('IGDB update failed. Showing local results only.');
      expect(component.isLoading).toBeFalse();
    }));

    it('should handle generic error during search', fakeAsync(() => {
      // Arrange
      mockGameService.updateAndSearch.and.returnValue(
        throwError(() => ({ status: 500 }))
      );
      // Important: Always mock the fallback search method too
      mockGameService.searchGames.and.returnValue(of(mockSearchResponse));
      mockGameService.triggerIgdbUpdate.and.returnValue(of({ message: 'Update initiated', data: [] }));
      
      // Act
      component.ngOnInit();
      component.searchControl.setValue('test');
      tick(300);
      
      // Assert
      expect(component.errorMessage).toBe('IGDB update failed. Showing local results only.');
      expect(component.isLoading).toBeFalse();
    }));

    it('should handle error during IGDB update in fallback path', fakeAsync(() => {
      // Arrange
      mockGameService.updateAndSearch.and.returnValue(
        throwError(() => new Error('API error'))
      );
      mockGameService.searchGames.and.returnValue(of(mockSearchResponse));
      mockGameService.triggerIgdbUpdate.and.returnValue(
        throwError(() => new Error('Update error'))
      );
      
      // Act
      component.ngOnInit();
      component.searchControl.setValue('test');
      tick(300);
      
      // Assert - Should still show search results despite IGDB update error
      expect(mockGameService.updateAndSearch).toHaveBeenCalledWith('test', 0, 50);
      expect(mockGameService.searchGames).toHaveBeenCalledWith('test', 0, 50);
      expect(mockGameService.triggerIgdbUpdate).toHaveBeenCalledWith('test');
      expect(component.searchResults).toEqual(mockSearchResponse.games);
    }));

    it('should handle generic error during fallback search', fakeAsync(() => {
      // Arrange
      mockGameService.updateAndSearch.and.returnValue(
        throwError(() => new Error('API error'))
      );
      mockGameService.searchGames.and.returnValue(
        throwError(() => ({ status: 500 })) // Simulate a generic error
      );
      
      // Act
      component.ngOnInit();
      component.searchControl.setValue('test');
      tick(300);
      
      // Assert
      expect(mockGameService.updateAndSearch).toHaveBeenCalledWith('test', 0, 50);
      expect(mockGameService.searchGames).toHaveBeenCalledWith('test', 0, 50);
      expect(component.isLoading).toBeFalse();
      expect(component.errorMessage).toBe('Failed to search for games. Please try again later.');
      expect(component.showError).toBeTrue();
    }));

    it('should handle error during pagination search', fakeAsync(() => {
      // Arrange
      component.currentQuery = 'test'; // Set a current query to simulate pagination
      mockGameService.searchGames.and.returnValue(
        throwError(() => ({ status: 500 })) // Simulate a generic error during pagination
      );
      
      // Act
      component.onPageChange({ page: 1, rows: 50 }); // Trigger pagination search
      tick(); // Allow observable to complete

      // Assert
      expect(mockGameService.searchGames).toHaveBeenCalledWith('test', 1, 50);
      expect(component.isLoading).toBeFalse();
      expect(component.errorMessage).toBe('Failed to load results. Please try again later.');
      expect(component.showError).toBeTrue();
    }));

    it('should handle 400 error during pagination search', fakeAsync(() => {
      // Arrange
      component.currentQuery = 'test'; // Set a current query to simulate pagination
      mockGameService.searchGames.and.returnValue(
        throwError(() => ({ status: 400 })) // Simulate a 400 error during pagination
      );
      
      // Act
      component.onPageChange({ page: 1, rows: 50 }); // Trigger pagination search
      tick(); // Allow observable to complete

      // Assert
      expect(mockGameService.searchGames).toHaveBeenCalledWith('test', 1, 50);
      expect(component.isLoading).toBeFalse();
      expect(component.errorMessage).toBe('Invalid search query. Please try a different search term.');
      expect(component.showError).toBeTrue();
    }));

    it('should handle 503 error during pagination search', fakeAsync(() => {
      // Arrange
      component.currentQuery = 'test'; // Set a current query to simulate pagination
      mockGameService.searchGames.and.returnValue(
        throwError(() => ({ status: 503 })) // Simulate a 503 error during pagination
      );
      
      // Act
      component.onPageChange({ page: 1, rows: 50 }); // Trigger pagination search
      tick(); // Allow observable to complete

      // Assert
      expect(mockGameService.searchGames).toHaveBeenCalledWith('test', 1, 50);
      expect(component.isLoading).toBeFalse();
      expect(component.errorMessage).toBe('Game service is currently unavailable. Please try again later.');
      expect(component.showError).toBeTrue();
    }));
  });

  describe('Error handling', () => {
    it('should show error message and auto-hide after timeout', fakeAsync(() => {
      // Arrange
      mockGameService.updateAndSearch.and.returnValue(
        throwError(() => ({ status: 404 }))
      );
      // Important: Always mock the fallback search method too
      mockGameService.searchGames.and.returnValue(of(mockSearchResponse));
      mockGameService.triggerIgdbUpdate.and.returnValue(of({ message: 'Update initiated', data: [] }));
      
      // Act
      component.ngOnInit();
      component.searchControl.setValue('test');
      tick(300);
      
      // Assert - Error should be shown
      expect(component.showError).toBeTrue();
      expect(component.errorMessage).toBe('IGDB update failed. Showing local results only.');
      expect(component.isLoading).toBeFalse();
      
      // Advance timer to test auto-hiding
      tick(5000);
      
      // Assert - Error should be hidden after timeout
      expect(component.showError).toBeFalse();
      expect(component.errorMessage).toBeNull();
    }));

    it('should allow manual dismissal of error message', () => {
      // Arrange
      component.errorMessage = 'Test error';
      component.showError = true;
      
      // Act
      component.dismissError();
      
      // Assert
      expect(component.showError).toBeFalse();
      expect(component.errorMessage).toBeNull();
    });
  });

  describe('Modal behavior', () => {
    it('should show modal and focus search input', fakeAsync(() => {
      // Arrange
      const mockElementRef = {
        nativeElement: {
          querySelector: jasmine.createSpy().and.returnValue({
            focus: jasmine.createSpy()
          })
        }
      };
      component.modalContent = mockElementRef as unknown as ElementRef;
      
      // Act
      component.show();
      tick();
      
      // Assert
      expect(component.isVisible).toBeTrue();
      expect(mockElementRef.nativeElement.querySelector).toHaveBeenCalledWith('input');
      expect(mockElementRef.nativeElement.querySelector().focus).toHaveBeenCalled();
    }));

    it('should handle case when input element is not found', fakeAsync(() => {
      // Arrange
      const mockElementRef = {
        nativeElement: {
          querySelector: jasmine.createSpy().and.returnValue(null)
        }
      };
      component.modalContent = mockElementRef as unknown as ElementRef;
      
      // Act
      component.show();
      tick();
      
      // Assert - Should not throw error
      expect(component.isVisible).toBeTrue();
      expect(mockElementRef.nativeElement.querySelector).toHaveBeenCalledWith('input');
    }));

    it('should close modal and reset search state', () => {
      // Arrange
      component.isVisible = true;
      component.searchResults = mockSearchResponse.games;
      component.currentPage = 1;
      component.totalPages = 2;
      component.totalElements = 10;
      component.errorMessage = 'Test error';
      component.showError = true;
      spyOn(component.closeModal, 'emit');
      
      // Act
      component.close();
      
      // Assert
      expect(component.isVisible).toBeFalse();
      expect(component.searchControl.value).toBe('');
      expect(component.searchResults).toEqual([]);
      expect(component.currentPage).toBe(0);
      expect(component.totalPages).toBe(0);
      expect(component.totalElements).toBe(0);
      expect(component.errorMessage).toBeNull();
      expect(component.showError).toBeFalse();
      expect(component.closeModal.emit).toHaveBeenCalled();
    });

    it('should close modal when Escape key is pressed', () => {
      // Arrange
      spyOn(component, 'close');
      
      // Act
      component.onEscapePressed();
      
      // Assert
      expect(component.close).toHaveBeenCalled();
    });

    it('should close modal when clicking on backdrop', () => {
      // Arrange
      spyOn(component, 'close');
      // Use the same object reference for both target and currentTarget
      // JavaScript compares objects by reference, not value
      const targetElement = {};
      const mockEvent = {
        target: targetElement,
        currentTarget: targetElement
      } as MouseEvent;
      
      // Act
      component.closeOnBackdropClick(mockEvent);
      
      // Assert
      expect(component.close).toHaveBeenCalled();
    });

    it('should not close modal when clicking on modal content', () => {
      // Arrange
      spyOn(component, 'close');
      const mockEvent = {
        target: {},
        currentTarget: { something: 'else' }
      } as unknown as MouseEvent;
      
      // Act
      component.closeOnBackdropClick(mockEvent);
      
      // Assert
      expect(component.close).not.toHaveBeenCalled();
    });
  });

  describe('Navigation', () => {
    it('should navigate to game details page and close modal', () => {
      // Arrange
      const mockGame = { gameId: 123, igdbId: 456, title: 'Navigated Game', coverImageUrl: 'url', releaseDate: 'date' } as Game;
      const mockEvent = { stopPropagation: jasmine.createSpy() } as unknown as Event;
      spyOn(component, 'close');

      // Act
      component.navigateToGame(mockGame, mockEvent);

      // Assert
      expect(mockEvent.stopPropagation).toHaveBeenCalled();
      expect(component.close).toHaveBeenCalled();
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/games', mockGame.gameId]);
    });
  });
}); 
