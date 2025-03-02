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

export interface PaginatedGameLibraryResponse {
  libraryId: number;
  games: Game[];
  currentPage: number;
  totalPages: number;
  totalElements: number;
  pageSize: number;
}

@Injectable({
  providedIn: 'root'
})
export class GameLibraryService {

  constructor(private http: HttpClient) { }

  getGameLibrary(sortBy?: string, filterByGenre?: string, page?: number, size?: number): Observable<PaginatedGameLibraryResponse> {
    const params: any = {};
    if (sortBy) {
      params.sortBy = sortBy;
    }
    if (filterByGenre) {
      params.filterByGenre = filterByGenre;
    }
    if (page !== undefined) {
      params.page = page;
    }
    if (size !== undefined) {
      params.size = size;
    }
    return this.http.get<PaginatedGameLibraryResponse>('/api/game-library/paginated', { params });
  }
} 
