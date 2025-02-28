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

  getGameLibrary(): Observable<GameLibrary> {
    return this.http.get<GameLibrary>('/api/game-library');
  }
} 
