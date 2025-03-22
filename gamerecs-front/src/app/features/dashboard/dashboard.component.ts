import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { CarouselModule } from 'primeng/carousel';
import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { SkeletonModule } from 'primeng/skeleton';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    CarouselModule,
    CardModule,
    TableModule,
    ButtonModule,
    TagModule,
    TooltipModule,
    SkeletonModule
  ]
})
export class DashboardComponent implements OnInit {
  // Loading state
  loading = true;

  // Placeholder data for top rated games carousel
  topRatedGames = [
    { id: 1, title: 'Top Rated Game #1', imageUrl: 'assets/placeholders/game-poster.svg', rating: 4.9 },
    { id: 2, title: 'Top Rated Game #2', imageUrl: 'assets/placeholders/game-poster.svg', rating: 4.8 },
    { id: 3, title: 'Top Rated Game #3', imageUrl: 'assets/placeholders/game-poster.svg', rating: 4.7 },
    { id: 4, title: 'Top Rated Game #4', imageUrl: 'assets/placeholders/game-poster.svg', rating: 4.6 },
    { id: 5, title: 'Top Rated Game #5', imageUrl: 'assets/placeholders/game-poster.svg', rating: 4.5 }
  ];

  // Placeholder data for personalized game recommendations
  recommendedGames = [
    { id: 1, title: 'Recommended Game #1', imageUrl: 'assets/placeholders/game-poster.svg', description: 'Based on your interest in RPG games', platform: 'PC, PS5' },
    { id: 2, title: 'Recommended Game #2', imageUrl: 'assets/placeholders/game-poster.svg', description: 'Similar to games you rated highly', platform: 'Xbox, PC' },
    { id: 3, title: 'Recommended Game #3', imageUrl: 'assets/placeholders/game-poster.svg', description: 'Popular in your region', platform: 'Nintendo Switch' },
    { id: 4, title: 'Recommended Game #4', imageUrl: 'assets/placeholders/game-poster.svg', description: 'New release in your favorite genre', platform: 'PS5, Xbox' },
    { id: 5, title: 'Recommended Game #5', imageUrl: 'assets/placeholders/game-poster.svg', description: 'Trending among similar players', platform: 'PC' }
  ];

  // Placeholder data for user backlog table
  backlogGames = [
    { id: 1, title: 'Backlog Game #1', status: 'To Play', platform: 'PC', addedDate: '2023-12-01' },
    { id: 2, title: 'Backlog Game #2', status: 'In Progress', platform: 'PlayStation', addedDate: '2023-11-15' },
    { id: 3, title: 'Backlog Game #3', status: 'On Hold', platform: 'Xbox', addedDate: '2023-10-22' },
    { id: 4, title: 'Backlog Game #4', status: 'To Play', platform: 'Nintendo Switch', addedDate: '2023-09-30' },
    { id: 5, title: 'Backlog Game #5', status: 'In Progress', platform: 'PC', addedDate: '2023-08-15' }
  ];

  // Placeholder data for top library recommendations
  libraryRecommendations = [
    { id: 1, title: 'Library Recommendation #1', imageUrl: 'assets/placeholders/game-poster.svg', reason: 'You played 2 hours', platform: 'PC' },
    { id: 2, title: 'Library Recommendation #2', imageUrl: 'assets/placeholders/game-poster.svg', reason: 'You completed 25%', platform: 'PS5' },
    { id: 3, title: 'Library Recommendation #3', imageUrl: 'assets/placeholders/game-poster.svg', reason: 'Recently added', platform: 'Xbox Series X' },
    { id: 4, title: 'Library Recommendation #4', imageUrl: 'assets/placeholders/game-poster.svg', reason: 'You played 5 hours', platform: 'PC, PS5' },
    { id: 5, title: 'Library Recommendation #5', imageUrl: 'assets/placeholders/game-poster.svg', reason: 'You completed 40%', platform: 'Nintendo Switch' }
  ];

  // Carousel configuration
  carouselResponsiveOptions = [
    {
      breakpoint: '1024px',
      numVisible: 3,
      numScroll: 3
    },
    {
      breakpoint: '768px',
      numVisible: 2,
      numScroll: 2
    },
    {
      breakpoint: '560px',
      numVisible: 1,
      numScroll: 1
    }
  ];

  constructor(private router: Router) {}

  ngOnInit(): void {
    // Simulate loading data - in real app, this would be API calls
    setTimeout(() => {
      this.loading = false;
    }, 1500);
  }

  /**
   * Navigate to game details page
   * @param gameId The ID of the game to navigate to
   */
  navigateToGameDetails(gameId: number): void {
    // In a real implementation, this would navigate to a game details page
    console.log(`Navigating to game details for game ID: ${gameId}`);
    // this.router.navigate(['/games', gameId]);
    
    // For now, just show an alert
    alert(`Game details page for ID: ${gameId} will be available soon!`);
  }

  getStatusSeverity(status: string): 'success' | 'info' | 'warn' | 'danger' {
    switch (status) {
    case 'To Play':
      return 'info';
    case 'In Progress':
      return 'warn';
    case 'On Hold':
      return 'danger';
    default:
      return 'success';
    }
  }
} 
