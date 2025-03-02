import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { GameLibraryService, PaginatedGameLibraryResponse } from '../../core/services/game-library.service';
import { PaginatorModule } from 'primeng/paginator';

@Component({
  selector: 'app-game-library',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginatorModule],
  templateUrl: './game-library.component.html',
  styleUrls: ['./game-library.component.css']
})
export class GameLibraryComponent implements OnInit {

  library: PaginatedGameLibraryResponse | null = null;
  isLoading = false;
  errorMessage: string | null = null;
  
  selectedSortBy: string = 'title';
  selectedFilterByGenre: string = '';
  
  // Pagination properties
  currentPage: number = 0;
  pageSize: number = 10;
  totalPages: number = 0;
  totalElements: number = 0;

  constructor(private libraryService: GameLibraryService) { }

  ngOnInit(): void {
    this.fetchGameLibrary();
  }

  fetchGameLibrary(page: number = this.currentPage): void {
    this.isLoading = true;
    this.errorMessage = null;
    this.libraryService.getGameLibrary(this.selectedSortBy, this.selectedFilterByGenre, page, this.pageSize).subscribe({
      next: (data: PaginatedGameLibraryResponse) => {
        this.library = data;
        // Ensure pagination values are valid numbers
        this.currentPage = data.currentPage || 0;
        this.totalPages = data.totalPages || 0;
        this.totalElements = data.totalElements || 0;
        this.pageSize = data.pageSize || 10;
        this.isLoading = false;
      },
      error: (err: unknown) => {
        console.error('Error fetching game library', err);
        this.errorMessage = 'Error loading game library';
        this.isLoading = false;
      }
    });
  }

  onPageChange(event: any): void {
    // Check if page size has changed
    if (event.rows !== this.pageSize) {
      this.pageSize = event.rows;
      // Reset to first page when changing page size
      this.fetchGameLibrary(0);
    } 
    // Check if page index has changed
    else if (event.page !== this.currentPage) {
      this.fetchGameLibrary(event.page);
    }
  }
} 
