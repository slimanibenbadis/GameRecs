import { Component, ElementRef, EventEmitter, HostListener, OnDestroy, OnInit, Output, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PaginatorModule } from 'primeng/paginator';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { GameService, GameSearchResponse } from '../../services/game.service';
import { Game } from '../../services/game-library.service';
import { Subject, Subscription, debounceTime, distinctUntilChanged, filter, takeUntil } from 'rxjs';
import { Router } from '@angular/router';

/**
 * SearchModalComponent provides a modal interface for game search functionality.
 * It includes a search input field and placeholders for future features like
 * search suggestions, filters, and external library imports.
 * 
 * Features:
 * - Responsive design (90% width on mobile, 50% on desktop)
 * - Keyboard navigation (Escape to close)
 * - Focus trapping
 * - Accessibility support
 * - Smooth animations
 * - Live search with debouncing
 * - Pagination support
 */
@Component({
  selector: 'app-search-modal',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ButtonModule,
    InputTextModule,
    PaginatorModule,
    ProgressSpinnerModule
  ],
  templateUrl: './search-modal.component.html',
  styleUrls: ['./search-modal.component.css']
})
export class SearchModalComponent implements OnInit, OnDestroy {
  @Output() closeModal = new EventEmitter<void>();
  @ViewChild('modalContent') modalContent!: ElementRef;

  isVisible = false;
  searchControl = new FormControl('');
  searchResults: Game[] = [];
  isLoading = false;
  
  // Error handling properties
  errorMessage: string | null = null;
  showError = false;
  
  // Pagination properties
  currentPage = 0;
  totalPages = 0;
  totalElements = 0;
  pageSize = 50;
  currentQuery = '';
  
  private destroy$ = new Subject<void>();

  constructor(
    private gameService: GameService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.setupSearchListener();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Sets up the search input listener with debounce and filtering
   */
  private setupSearchListener(): void {
    this.searchControl.valueChanges.pipe(
      debounceTime(300), // Wait for 300ms pause in events
      distinctUntilChanged(), // Only emit when the current value is different from the last
      filter(query => !query || query.length >= 2), // Only proceed if query is empty or has at least 2 characters
      takeUntil(this.destroy$)
    ).subscribe(query => {
      // Clear any previous error messages when starting a new search
      this.clearError();
      
      if (query && query.length >= 2) {
        // Sanitize the query to avoid XSS (even though we're not rendering it as HTML)
        const sanitizedQuery = this.sanitizeSearchQuery(query);
        this.currentQuery = sanitizedQuery;
        this.currentPage = 0; // Reset to first page on new search
        this.performSearch(sanitizedQuery, this.currentPage, this.pageSize);
      } else {
        this.searchResults = [];
        this.currentPage = 0;
        this.totalPages = 0;
        this.totalElements = 0;
      }
    });
  }

  /**
   * Sanitizes the search query to prevent injection attacks
   * @param query The raw search query from user input
   * @returns A sanitized query string
   */
  private sanitizeSearchQuery(query: string): string {
    // Basic sanitization - trim whitespace and remove potentially harmful characters
    return query.trim().replace(/[<>{}()]/g, '');
  }

  /**
   * Displays an error message to the user
   * @param message The error message to display
   */
  private showErrorMessage(message: string): void {
    this.errorMessage = message;
    this.showError = true;
    
    // Auto-hide error after 5 seconds
    setTimeout(() => {
      this.clearError();
    }, 5000);
  }
  
  /**
   * Clears the current error message
   */
  private clearError(): void {
    this.errorMessage = null;
    this.showError = false;
  }

  /**
   * Performs the actual search API call
   * @param query The search query string
   * @param page The page number (0-indexed)
   * @param size The page size
   */
  private performSearch(query: string, page: number = 0, size: number = 50): void {
    this.isLoading = true;
    this.clearError();
    
    // Log the search query for debugging
    console.log(`Searching for games with query: ${query}, page: ${page}, size: ${size}`);
    
    // Flag to track which search method is being used
    const isUsingCombinedSearch = (page === 0);
    
    // Determine which search method to use
    const searchMethod = isUsingCombinedSearch
      ? this.gameService.updateAndSearch(query, page, size) // For first page, update IGDB data first
      : this.gameService.searchGames(query, page, size);    // For pagination, just search local DB
    
    // Perform the search
    searchMethod.pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (response: GameSearchResponse) => {
        this.searchResults = response.games;
        this.currentPage = response.currentPage;
        this.totalPages = response.totalPages;
        this.totalElements = response.totalElements;
        this.pageSize = response.pageSize;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error searching games:', err);
        
        // If the updateAndSearch method failed and this is the first page,
        // fall back to the regular search method
        if (isUsingCombinedSearch) {
          console.log('Falling back to regular search after update-and-search failed');
          
          // Show a non-intrusive message about the fallback
          this.showErrorMessage('IGDB update failed. Showing local results only.');
          
          this.gameService.searchGames(query, page, size).pipe(
            takeUntil(this.destroy$)
          ).subscribe({
            next: (response: GameSearchResponse) => {
              this.searchResults = response.games;
              this.currentPage = response.currentPage;
              this.totalPages = response.totalPages;
              this.totalElements = response.totalElements;
              this.pageSize = response.pageSize;
              
              // After regular search completes, try to trigger IGDB update in the background
              // This is asynchronous and won't block UI
              this.gameService.triggerIgdbUpdate(query).pipe(
                takeUntil(this.destroy$)
              ).subscribe({
                next: (updateResponse) => {
                  console.log(`IGDB update initiated successfully for query: ${query}`);
                  console.log(`Response: ${updateResponse.message}`);
                },
                error: (updateErr) => {
                  console.error(`Error triggering IGDB update for query: ${query}`, updateErr);
                  // Don't show another error message to avoid overwhelming the user
                }
              });
              
              this.isLoading = false;
            },
            error: (fallbackErr) => {
              console.error('Error with fallback search:', fallbackErr);
              this.isLoading = false;
              
              // Format the error message based on the HTTP status
              let errorMsg = 'Failed to search for games. Please try again later.';
              
              if (fallbackErr.status === 400) {
                errorMsg = 'Invalid search query. Please try a different search term.';
              } else if (fallbackErr.status === 503) {
                errorMsg = 'Game service is currently unavailable. Please try again later.';
              }
              
              this.showErrorMessage(errorMsg);
            }
          });
        } else {
          this.isLoading = false;
          
          // Format the error message based on the HTTP status
          let errorMsg = 'Failed to load results. Please try again later.';
          
          if (err.status === 400) {
            errorMsg = 'Invalid search query. Please try a different search term.';
          } else if (err.status === 503) {
            errorMsg = 'Game service is currently unavailable. Please try again later.';
          }
          
          this.showErrorMessage(errorMsg);
        }
      }
    });
  }

  /**
   * Handles page change events from the paginator
   * @param event The page event containing page number
   */
  onPageChange(event: any): void {
    this.performSearch(this.currentQuery, event.page, this.pageSize);
  }

  /**
   * Manually dismisses the error message
   */
  dismissError(): void {
    this.clearError();
  }

  /**
   * Handles the escape key press event to close the modal
   */
  @HostListener('document:keydown.escape')
  onEscapePressed() {
    this.close();
  }

  /**
   * Shows the modal and focuses the search input
   */
  show() {
    this.isVisible = true;
    // Clear any previous errors when opening the modal
    this.clearError();
    // Focus the input when modal opens
    setTimeout(() => {
      const input = this.modalContent.nativeElement.querySelector('input');
      if (input) {
        input.focus();
      }
    });
  }

  /**
   * Closes the modal and resets the search query
   */
  close() {
    this.isVisible = false;
    this.searchControl.setValue('');
    this.searchResults = [];
    this.currentPage = 0;
    this.totalPages = 0;
    this.totalElements = 0;
    this.clearError();
    this.closeModal.emit();
  }

  /**
   * Handles clicks on the modal backdrop to close the modal
   * @param event The mouse event from the click
   */
  closeOnBackdropClick(event: MouseEvent) {
    if (event.target === event.currentTarget) {
      this.close();
    }
  }

  /**
   * Navigates to the game details page for the selected game
   * @param game The game to view details for
   * @param event The mouse event
   */
  navigateToGame(game: Game, event: MouseEvent): void {
    event.stopPropagation();
    this.close();
    this.router.navigate(['/games', game.gameId]);
  }
} 
