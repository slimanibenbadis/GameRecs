import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Game, GameLibraryService, PaginatedGameLibraryResponse } from '../../core/services/game-library.service';
import { PaginatorModule } from 'primeng/paginator';
import { Router, RouterLink } from '@angular/router';
import { MessageService } from 'primeng/api';
import { MessagesModule } from 'primeng/messages';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

@Component({
  selector: 'app-game-library',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule, 
    PaginatorModule, 
    MessagesModule,
    InputTextModule,
    ButtonModule,
    ProgressSpinnerModule,
    RouterLink
  ],
  templateUrl: './game-library.component.html',
  styleUrls: ['./game-library.component.css'],
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

  // Steam Import properties
  isLoadingImport: boolean = false;

  constructor(
    private libraryService: GameLibraryService,
    private router: Router,
    private messageService: MessageService
  ) { }

  ngOnInit(): void {
    this.fetchGameLibrary();
  }

  fetchGameLibrary(page: number = this.currentPage): void {
    this.isLoading = true;
    this.errorMessage = null;
    this.messageService.clear();
    this.libraryService.getGameLibrary(this.selectedSortBy, this.selectedFilterByGenre, page, this.pageSize).subscribe({
      next: (data: PaginatedGameLibraryResponse) => {
        this.library = data;
        this.currentPage = data.currentPage ?? 0;
        this.totalPages = data.totalPages ?? 0;
        this.totalElements = data.totalElements ?? 0;
        this.pageSize = data.pageSize ?? 10;
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error('Error fetching game library', err);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Error loading game library. Please try again later.' });
        this.errorMessage = 'Error loading game library';
        this.isLoading = false;
      }
    });
  }

  onPageChange(event: any): void {
    if (event.rows !== this.pageSize) {
      this.pageSize = event.rows;
      this.fetchGameLibrary(0);
    } 
    else if (event.page !== this.currentPage) {
      this.fetchGameLibrary(event.page);
    }
  }

  /**
   * Handles the Steam library import request.
   */
  onImportSteamLibrary(): void {
    this.messageService.clear();
    this.isLoadingImport = true;
    // Optionally, check if profile indicates credentials are set via ProfileService if complex UX is desired
    // For now, let backend handle it.

    this.libraryService.importSteamLibrary().subscribe({ // No argument
      next: (response: any) => {
        this.isLoadingImport = false;
        const messageDetail = response?.message || 'Steam library import initiated successfully. It may take a few moments to reflect the changes.';
        this.messageService.add({ severity: 'success', summary: 'Success', detail: messageDetail });
        setTimeout(() => this.fetchGameLibrary(0), 1000); 
      },
      error: (err: any) => {
        this.isLoadingImport = false;
        console.error('Error importing Steam library', err);
        let detail = err?.error?.message || err?.message || 'Import failed. Please ensure your Steam credentials are set in your profile and try again later.';
        if (err.status === 400 && err.error?.message?.toLowerCase().includes('credentials not configured')) {
          detail = 'Steam credentials not found in your profile. Please add them on your profile page.';
        }
        this.messageService.add({ severity: 'error', summary: 'Error', detail: detail });
      }
    });
  }

  /**
   * Navigates to the game details page for the selected game
   * @param gameId The ID of the game to view details for
   */
  navigateToGame(game: Game, event: Event): void {
    event.stopPropagation();
    this.router.navigate(['/games', game.gameId]);
  }
} 
