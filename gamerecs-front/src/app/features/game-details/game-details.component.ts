import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { TabViewModule } from 'primeng/tabview';
import { ChipModule } from 'primeng/chip';
import { TooltipModule } from 'primeng/tooltip';
import { SkeletonModule } from 'primeng/skeleton';
import { RippleModule } from 'primeng/ripple';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageModule } from 'primeng/message';
import { GameService } from '../../core/services/game.service';
import { GameDto } from '../../models/game.dto';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-game-details',
  templateUrl: './game-details.component.html',
  styleUrl: './game-details.component.css',
  standalone: true,
  imports: [
    CommonModule,
    CardModule,
    ButtonModule,
    TagModule,
    TabViewModule,
    ChipModule,
    TooltipModule,
    SkeletonModule,
    RippleModule,
    ProgressSpinnerModule,
    MessageModule
  ]
})
export class GameDetailsComponent implements OnInit {
  gameId!: number;
  game: GameDto | null = null;
  loading = true;
  error: string | null = null;
  notFound = false;
  retryCount = 0;
  maxRetries = 1;

  constructor(
    private route: ActivatedRoute,
    public router: Router,
    private gameService: GameService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const idParam = params.get('id');
      if (!idParam) {
        this.error = 'Invalid game ID';
        this.loading = false;
        return;
      }

      const parsedId = +idParam;
      if (isNaN(parsedId) || parsedId <= 0) {
        this.error = 'Invalid game ID: must be a positive number';
        this.loading = false;
        return;
      }

      this.gameId = parsedId;
      this.loadGameDetails();
    });
  }

  loadGameDetails(): void {
    this.loading = true;
    this.error = null;
    this.notFound = false;

    this.gameService.getGameById(this.gameId).subscribe({
      next: (game) => {
        this.game = game;
        this.loading = false;
        this.retryCount = 0; // Reset retry count on success
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        
        // Handle 404 Not Found specifically
        if (err.status === 404) {
          this.notFound = true;
          this.error = `Game with ID ${this.gameId} could not be found`;
          return;
        }
        
        // Handle other error cases
        console.error('Error loading game details:', err);
        
        // Provide more specific error messages based on status code
        if (err.status === 0) {
          this.error = 'Network error. Please check your internet connection.';
        } else if (err.status === 401) {
          this.error = 'You need to be logged in to view this content.';
        } else if (err.status === 403) {
          this.error = 'You don\'t have permission to access this content.';
        } else if (err.status >= 500) {
          this.error = 'Server error. Please try again later.';
          
          // Attempt to retry if the error was a server error
          if (this.retryCount < this.maxRetries) {
            this.retryCount++;
            setTimeout(() => {
              this.loadGameDetails();
            }, 2000); // Wait 2 seconds before retrying
          }
        } else {
          // Use the error message from the API if available
          this.error = err.error?.message || 'Failed to load game details';
        }
      }
    });
  }

  retryLoading(): void {
    this.loadGameDetails();
  }

  handleImageError(event: Event): void {
    const imgElement = event.target as HTMLImageElement;
    if (imgElement) {
      imgElement.src = 'assets/placeholders/game-poster.svg';
    }
  }

  getBacklogStatusSeverity(status: string | null): 'success' | 'info' | 'warn' | 'danger' {
    if (!status) return 'info';
    
    switch (status) {
    case 'To Play':
      return 'info';
    case 'In Progress':
      return 'warn';
    case 'Completed':
      return 'success';
    case 'Abandoned':
      return 'danger';
    default:
      return 'info';
    }
  }

  /**
   * Returns a descriptive text for each backlog status for tooltips
   * @param status The backlog status
   * @returns A descriptive text explaining the status
   */
  getBacklogStatusDescription(status: string | null): string {
    if (!status) return '';
    
    switch (status) {
    case 'To Play':
      return 'You\'ve saved this game to play later';
    case 'In Progress':
      return 'You\'re currently playing this game';
    case 'Completed':
      return 'You\'ve finished playing this game';
    case 'Abandoned':
      return 'You\'ve stopped playing this game';
    default:
      return '';
    }
  }

  /**
   * Returns a CSS class for styling the backlog status container border
   * @param status The backlog status
   * @returns A tailwind border color class
   */
  getBacklogBorderColor(status: string | null): string {
    if (!status) return 'gray-500';
    
    switch (status) {
    case 'To Play':
      return 'blue-400';
    case 'In Progress':
      return 'yellow-400';
    case 'Completed':
      return 'green-400';
    case 'Abandoned':
      return 'red-400';
    default:
      return 'gray-500';
    }
  }

  /**
   * Returns a CSS class based on the PRI rating value
   * @param rating The PRI rating value (0-100)
   * @returns A CSS class for styling based on rating quality
   */
  getPriRatingClass(rating: number | null): string {
    if (!rating) return 'text-gray-500';
    
    if (rating >= 80) return 'text-green-400';
    if (rating >= 60) return 'text-blue-400';
    if (rating >= 40) return 'text-yellow-400';
    return 'text-red-400';
  }

  /**
   * Calculates percentage width for the PRI rating quality indicator
   * @param rating The PRI rating value (0-100)
   * @returns A percentage (0-100) representing the rating quality
   */
  getPriPercentage(rating: number | null): number {
    if (!rating) return 0;
    return rating; // The rating is already 0-100 scale
  }
} 
