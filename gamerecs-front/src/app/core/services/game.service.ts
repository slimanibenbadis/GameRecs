import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, retry, map } from 'rxjs/operators';
import { Game } from './game-library.service';
import { MessageService } from 'primeng/api';
import { GameDto } from '../../models/game.dto';

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
  
  constructor(
    private http: HttpClient,
    private messageService: MessageService
  ) { }
  
  /**
   * Retrieves a game by its ID
   * 
   * @param id The ID of the game to retrieve
   * @returns An Observable of GameDto containing the game details
   */
  getGameById(id: number): Observable<GameDto> {
    return this.http.get<GameDto>(`/api/games/${id}`)
      .pipe(
        catchError(err => {
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: 'Could not load game'
          });
          return throwError(() => err);
        })
      );
  }
  
  /**
   * Searches for games based on a query string with pagination support
   * 
   * @param query The search query string
   * @param page The page number (0-indexed)
   * @param size The number of results per page
   * @returns An Observable of GameSearchResponse containing search results and pagination info
   */
  searchGames(query: string, page: number = 0, size: number = 50): Observable<GameSearchResponse> {
    // Input validation
    if (!query || query.trim().length < 2) {
      console.error('Invalid search query:', query);
      return throwError(() => new Error('Search query must be at least 2 characters long'));
    }
    
    // Sanitize and validate pagination parameters
    const validatedPage = Math.max(0, page); // Ensure page is not negative
    const validatedSize = Math.min(100, Math.max(1, size)); // Ensure size is between 1 and 100
    
    // Create HttpParams object
    const params = new HttpParams()
      .set('query', query.trim())
      .set('page', validatedPage.toString())
      .set('size', validatedSize.toString());
    
    return this.http.get<GameSearchResponse>('/api/games/search', { params })
      .pipe(
        retry(1), // Retry once before failing
        catchError(this.handleError('searchGames', query))
      );
  }
  
  /**
   * Triggers a background update from IGDB API for the given query
   * This is an asynchronous operation that doesn't block the UI
   * 
   * @param query The search query string to use for the IGDB update
   * @returns Observable with the update acknowledgement response
   */
  triggerIgdbUpdate(query: string): Observable<IgdbUpdateResponse> {
    // Input validation
    if (!query || query.trim().length < 2) {
      console.error('Invalid update query:', query);
      return throwError(() => new Error('Update query must be at least 2 characters long'));
    }
    
    console.log(`Triggering IGDB update for query: ${query}`);
    
    // Create HttpParams object with sanitized query
    const params = new HttpParams().set('query', query.trim());
    
    return this.http.post<IgdbUpdateResponse>('/api/igdb/update', null, { params })
      .pipe(
        catchError(this.handleError('triggerIgdbUpdate', query))
      );
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
    // Input validation
    if (!query || query.trim().length < 2) {
      console.error('Invalid search query:', query);
      return throwError(() => new Error('Search query must be at least 2 characters long'));
    }
    
    // Sanitize and validate pagination parameters
    const validatedPage = Math.max(0, page); // Ensure page is not negative
    const validatedSize = Math.min(100, Math.max(1, size)); // Ensure size is between 1 and 100
    
    console.log(`Performing combined IGDB update and search for query: ${query}`);
    
    // Create HttpParams object with sanitized parameters
    const params = new HttpParams()
      .set('query', query.trim())
      .set('page', validatedPage.toString())
      .set('size', validatedSize.toString());
    
    return this.http.post<GameSearchResponse>('/api/igdb/update-and-search', null, { params })
      .pipe(
        catchError(this.handleError('updateAndSearch', query))
      );
  }
  
  /**
   * Generic error handler for HTTP requests
   * 
   * @param operation The name of the operation that failed
   * @param context Additional context for the error (e.g., the search query)
   * @returns An error handler function that returns an Observable with an Error
   */
  private handleError(operation: string, context: string) {
    return (error: HttpErrorResponse): Observable<never> => {
      // Log to console
      const errorMessage = this.getErrorMessage(error);
      console.error(`${operation} failed for "${context}":`, errorMessage, error);
      
      // Create a new error with the formatted message
      const formattedError = new HttpErrorResponse({
        error: error.error,
        headers: error.headers,
        status: error.status,
        statusText: error.statusText,
        url: error.url || undefined
      });
      
      // Set the formatted message
      Object.defineProperty(formattedError, 'message', { value: errorMessage });
      
      // The returned observable will be caught by the component
      return throwError(() => formattedError);
    };
  }
  
  /**
   * Extracts a user-friendly error message from an HttpErrorResponse
   * 
   * @param error The HttpErrorResponse
   * @returns A user-friendly error message
   */
  private getErrorMessage(error: HttpErrorResponse): string {
    if (error.error instanceof ErrorEvent) {
      // Client-side or network error
      return `Client error: ${error.error.message}`;
    }
    
    // Backend returned an unsuccessful response code
    if (error.error && typeof error.error === 'object') {
      // Try to extract message from the error object
      if (error.error.message) {
        return error.error.message;
      }
      
      // If there are field-specific errors, join them
      if (error.error.errors && typeof error.error.errors === 'object') {
        return Object.values(error.error.errors).join('. ');
      }
    }
    
    // Default error message based on status code
    switch (error.status) {
    case 400: return 'Invalid request parameters';
    case 401: return 'Authentication required';
    case 403: return 'Access denied';
    case 404: return 'Resource not found';
    case 500: return 'Server error';
    case 503: return 'Service unavailable';
    default: return `Error ${error.status}: ${error.statusText || 'Unknown error'}`;
    }
  }
} 
