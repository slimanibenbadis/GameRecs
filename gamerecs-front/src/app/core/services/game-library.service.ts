import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Game {
  gameId: number;
  igdbId: number;
  title: string;
  description?: string;
  releaseDate?: string;
  coverImageUrl?: string;
  updatedAt?: string;
}

export interface GameLibrary {
  libraryId: number;
  games: Game[];
}

@Injectable({
  providedIn: 'root'
})
export class GameLibraryService {

  constructor(private http: HttpClient) { }

  getGameLibrary(sortBy?: string, filterByGenre?: string): Observable<GameLibrary> {
    const params: any = {};
    if (sortBy) {
      params.sortBy = sortBy;
    }
    if (filterByGenre) {
      params.filterByGenre = filterByGenre;
    }
    return this.http.get<GameLibrary>('/api/game-library', { params });
  }
} 
