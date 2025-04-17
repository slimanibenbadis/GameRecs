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

export interface IgdbUpdateResponse {
  message: string;
  data: any[];
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
   * This is an asynchronous operation that doesn't block the UI
   * 
   * @param query The search query string to use for the IGDB update
   * @returns Observable with the update acknowledgement response
   */
  triggerIgdbUpdate(query: string): Observable<IgdbUpdateResponse> {
    console.log(`Triggering IGDB update for query: ${query}`);
    return this.http.post<IgdbUpdateResponse>('/api/igdb/update', null, { 
      params: { query } 
    });
  }
  
  /**
   * Combined operation that first updates IGDB data, then searches with updated results
   * This ensures search results include the most up-to-date game data by running operations sequentially
   * 
   * @param query The search query string
   * @param page The page number (0-indexed)
   * @param size The number of results per page
   * @returns An Observable of GameSearchResponse with fresh results after IGDB update
   */
  updateAndSearch(query: string, page: number = 0, size: number = 50): Observable<GameSearchResponse> {
    console.log(`Performing combined IGDB update and search for query: ${query}`);
    return this.http.post<GameSearchResponse>('/api/igdb/update-and-search', null, {
      params: { query, page, size }
    });
  }
} 
