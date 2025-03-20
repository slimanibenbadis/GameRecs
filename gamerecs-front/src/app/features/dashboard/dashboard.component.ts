import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CarouselModule } from 'primeng/carousel';
import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

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
    TooltipModule
  ]
})
export class DashboardComponent {
  // Placeholder data for top rated games carousel
  topRatedGames = [
    { id: 1, title: 'Top Rated Game #1', imageUrl: 'assets/placeholders/placeholder.svg', rating: 4.9 },
    { id: 2, title: 'Top Rated Game #2', imageUrl: 'assets/placeholders/placeholder.svg', rating: 4.8 },
    { id: 3, title: 'Top Rated Game #3', imageUrl: 'assets/placeholders/placeholder.svg', rating: 4.7 },
    { id: 4, title: 'Top Rated Game #4', imageUrl: 'assets/placeholders/placeholder.svg', rating: 4.6 },
    { id: 5, title: 'Top Rated Game #5', imageUrl: 'assets/placeholders/placeholder.svg', rating: 4.5 }
  ];

  // Placeholder data for personalized game recommendations
  recommendedGames = [
    { id: 1, title: 'Recommended Game #1', imageUrl: 'assets/placeholders/placeholder.svg', description: 'Based on your interest in RPG games' },
    { id: 2, title: 'Recommended Game #2', imageUrl: 'assets/placeholders/placeholder.svg', description: 'Similar to games you rated highly' },
    { id: 3, title: 'Recommended Game #3', imageUrl: 'assets/placeholders/placeholder.svg', description: 'Popular in your region' },
    { id: 4, title: 'Recommended Game #4', imageUrl: 'assets/placeholders/placeholder.svg', description: 'New release in your favorite genre' },
    { id: 5, title: 'Recommended Game #5', imageUrl: 'assets/placeholders/placeholder.svg', description: 'Trending among similar players' }
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
    { id: 1, title: 'Library Recommendation #1', imageUrl: 'assets/placeholders/placeholder.svg', reason: 'You played 2 hours' },
    { id: 2, title: 'Library Recommendation #2', imageUrl: 'assets/placeholders/placeholder.svg', reason: 'You completed 25%' },
    { id: 3, title: 'Library Recommendation #3', imageUrl: 'assets/placeholders/placeholder.svg', reason: 'Recently added' },
    { id: 4, title: 'Library Recommendation #4', imageUrl: 'assets/placeholders/placeholder.svg', reason: 'You played 5 hours' },
    { id: 5, title: 'Library Recommendation #5', imageUrl: 'assets/placeholders/placeholder.svg', reason: 'You completed 40%' }
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
