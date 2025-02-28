import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GameLibraryService, GameLibrary } from '../../core/services/game-library.service';

@Component({
  selector: 'app-game-library',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './game-library.component.html',
  styleUrls: ['./game-library.component.css']
})
export class GameLibraryComponent implements OnInit {

  library: GameLibrary | null = null;
  isLoading = false;
  errorMessage: string | null = null;

  constructor(private libraryService: GameLibraryService) { }

  ngOnInit(): void {
    this.fetchGameLibrary();
  }

  fetchGameLibrary(): void {
    this.isLoading = true;
    this.errorMessage = null;
    this.libraryService.getGameLibrary().subscribe({
      next: (data: GameLibrary) => {
        this.library = data;
        this.isLoading = false;
      },
      error: (err: unknown) => {
        console.error('Error fetching game library', err);
        this.errorMessage = 'Error loading game library';
        this.isLoading = false;
      }
    });
  }
} 
