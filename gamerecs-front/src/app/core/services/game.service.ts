import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { Game } from './game-library.service';

export interface GameSearchResponse {
  games: Game[];
  currentPage: number;
  totalPages: number;
  totalElements: number;
  pageSize: number;
  query: string;
}

@Injectable({
  providedIn: 'root'
})
export class GameService {
  
  constructor(private http: HttpClient) { }
  
  /**
   * Searches for games based on a query string with pagination support
   * 
   * @param query The search query string
   * @param page The page number (0-indexed)
   * @param size The number of results per page
   * @returns An Observable of GameSearchResponse containing search results and pagination info
   */
  searchGames(query: string, page: number = 0, size: number = 50): Observable<GameSearchResponse> {
    return this.http.get<GameSearchResponse>('/api/games/search', { 
      params: { query, page, size }
    });
  }
  
  /**
   * Triggers a background update from IGDB API for the given query
   * This is a "fire-and-forget" operation that doesn't block the UI
   * 
   * @param query The search query string to use for the IGDB update
   */
  triggerIgdbUpdate(query: string): void {
    console.log(`Triggering IGDB update for query: ${query}`);
    this.http.post('/api/igdb/update', null, { 
      params: { query } 
    }).subscribe({
      next: () => console.log(`IGDB update successfully triggered for query: ${query}`),
      error: (err) => console.error(`Error triggering IGDB update for query: ${query}`, err)
    });
  }
} 
