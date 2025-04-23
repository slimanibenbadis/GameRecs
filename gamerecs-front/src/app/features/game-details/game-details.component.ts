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
import { GameService } from '../../core/services/game.service';
import { GameDto } from '../../models/game.dto';

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
    RippleModule
  ]
})
export class GameDetailsComponent implements OnInit {
  gameId!: number;
  game: GameDto | null = null;
  loading = true;
  error: string | null = null;

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

      this.gameId = +idParam;
      this.loadGameDetails();
    });
  }

  loadGameDetails(): void {
    this.loading = true;
    this.error = null;

    this.gameService.getGameById(this.gameId).subscribe({
      next: (game) => {
        this.game = game;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading game details:', err);
        this.error = 'Failed to load game details';
        this.loading = false;
      }
    });
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
