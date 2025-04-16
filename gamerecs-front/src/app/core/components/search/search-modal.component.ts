import { Component, ElementRef, EventEmitter, HostListener, OnDestroy, OnInit, Output, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PaginatorModule } from 'primeng/paginator';
import { GameService, GameSearchResponse } from '../../services/game.service';
import { Game } from '../../services/game-library.service';
import { Subject, Subscription, debounceTime, distinctUntilChanged, filter, takeUntil } from 'rxjs';

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
    PaginatorModule
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
  
  // Pagination properties
  currentPage = 0;
  totalPages = 0;
  totalElements = 0;
  pageSize = 50;
  currentQuery = '';
  
  private destroy$ = new Subject<void>();

  constructor(private gameService: GameService) {}

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
      if (query && query.length >= 2) {
        this.currentQuery = query;
        this.currentPage = 0; // Reset to first page on new search
        this.performSearch(query, this.currentPage, this.pageSize);
      } else {
        this.searchResults = [];
        this.currentPage = 0;
        this.totalPages = 0;
        this.totalElements = 0;
      }
    });
  }

  /**
   * Performs the actual search API call
   * @param query The search query string
   * @param page The page number (0-indexed)
   * @param size The page size
   */
  private performSearch(query: string, page: number = 0, size: number = 50): void {
    this.isLoading = true;
    
    // Log the search query for debugging
    console.log(`Searching for games with query: ${query}, page: ${page}, size: ${size}`);
    
    // Perform the search
    this.gameService.searchGames(query, page, size).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (response: GameSearchResponse) => {
        this.searchResults = response.games;
        this.currentPage = response.currentPage;
        this.totalPages = response.totalPages;
        this.totalElements = response.totalElements;
        this.pageSize = response.pageSize;
        this.isLoading = false;
        
        // After search completes, trigger IGDB update in the background
        // This is fire-and-forget, won't block UI
        if (page === 0) { // Only trigger IGDB update on first page
          this.gameService.triggerIgdbUpdate(query);
        }
      },
      error: (err) => {
        console.error('Error searching games:', err);
        this.isLoading = false;
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
} 
