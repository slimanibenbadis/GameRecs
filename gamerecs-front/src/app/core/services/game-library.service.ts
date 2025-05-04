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

  /**
   * Initiates the import of the user's Steam library.
   * @param steamId The user's 64-bit Steam ID.
   * @returns An Observable that completes when the request is sent.
   */
  importSteamLibrary(steamId: string): Observable<any> {
    // The backend expects the steamId as a request parameter
    const params = { steamId };
    // No body is needed for this POST request
    return this.http.post<any>('/api/game-library/import/steam', null, { params });
  }
} 
