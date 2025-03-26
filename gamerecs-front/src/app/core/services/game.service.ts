import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Game } from './game-library.service';

@Injectable({
  providedIn: 'root'
})
export class GameService {
  
  constructor(private http: HttpClient) { }
  
  /**
   * Searches for games based on a query string
   * 
   * @param query The search query string
   * @returns An Observable of Game array containing search results
   */
  searchGames(query: string): Observable<Game[]> {
    return this.http.get<Game[]>('/api/igdb/search', { 
      params: { query }
    });
  }
} 
