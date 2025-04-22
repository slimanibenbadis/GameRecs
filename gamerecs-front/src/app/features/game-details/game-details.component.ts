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
} 
