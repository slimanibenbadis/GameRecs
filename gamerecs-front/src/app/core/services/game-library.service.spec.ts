import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { GameLibraryService, GameLibrary, Game, PaginatedGameLibraryResponse } from '../services/game-library.service';

describe('GameLibraryService', () => {
  let service: GameLibraryService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [GameLibraryService]
    });
    service = TestBed.inject(GameLibraryService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should fetch the game library data', () => {
    const dummyLibrary: PaginatedGameLibraryResponse = {
      libraryId: 1,
      games: [{
        gameId: 42,
        igdbId: 101010,
        title: 'Test Game',
        description: 'A fun game',
        releaseDate: '2023-10-01',
        coverImageUrl: 'http://example.com/cover.jpg',
        updatedAt: '2023-10-01T10:00:00'
      }],
      currentPage: 0,
      totalPages: 1,
      totalElements: 1,
      pageSize: 10
    };

    service.getGameLibrary().subscribe((library: PaginatedGameLibraryResponse) => {
      expect(library.libraryId).toBe(1);
      expect(library.games.length).toBe(1);
      expect(library.games[0].title).toBe('Test Game');
      expect(library.currentPage).toBe(0);
      expect(library.totalPages).toBe(1);
    });

    const req = httpMock.expectOne('/api/game-library/paginated');
    expect(req.request.method).toBe('GET');
    req.flush(dummyLibrary);
  });

  it('should attach query parameters when provided', () => {
    const dummyLibrary: PaginatedGameLibraryResponse = {
      libraryId: 1,
      games: [],
      currentPage: 0,
      totalPages: 0,
      totalElements: 0,
      pageSize: 10
    };
    
    service.getGameLibrary('releaseDate', 'Action').subscribe(library => {
      expect(library).toEqual(dummyLibrary);
    });
    
    const req = httpMock.expectOne(r => 
      r.url === '/api/game-library/paginated' &&
      r.params.get('sortBy') === 'releaseDate' &&
      r.params.get('filterByGenre') === 'Action'
    );
    expect(req.request.method).toBe('GET');
    req.flush(dummyLibrary);
  });

  it('should attach pagination parameters and parse paginated metadata when provided', () => {
    const dummyResponse: PaginatedGameLibraryResponse = {
      libraryId: 1,
      games: [{
        gameId: 42,
        igdbId: 101010,
        title: 'Test Game'
      }],
      currentPage: 0,
      totalPages: 2,
      totalElements: 15,
      pageSize: 10
    };

    service.getGameLibrary('title', '', 0, 10).subscribe(response => {
      expect(response.libraryId).toBe(1);
      expect(response.currentPage).toBe(0);
      expect(response.totalPages).toBe(2);
      expect(response.totalElements).toBe(15);
      expect(response.pageSize).toBe(10);
    });

    const req = httpMock.expectOne(req => 
      req.url === '/api/game-library/paginated' &&
      req.params.get('page') === '0' &&
      req.params.get('size') === '10'
    );
    expect(req.request.method).toBe('GET');
    req.flush(dummyResponse);
  });

  it('should propagate errors when the API call fails', () => {
    service.getGameLibrary().subscribe({
      next: () => fail('Should have failed with a 404 error'),
      error: error => {
        expect(error.status).toBe(404);
      }
    });

    const req = httpMock.expectOne('/api/game-library/paginated');
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  });
}); 
