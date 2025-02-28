import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { GameLibraryService, GameLibrary, Game } from '../services/game-library.service';

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
    const dummyLibrary: GameLibrary = {
      libraryId: 1,
      games: [{
        gameId: 42,
        igdbId: 101010,
        title: 'Test Game',
        description: 'A fun game',
        releaseDate: '2023-10-01',
        coverImageUrl: 'http://example.com/cover.jpg',
        updatedAt: '2023-10-01T10:00:00'
      }]
    };

    service.getGameLibrary().subscribe((library: GameLibrary) => {
      expect(library.libraryId).toBe(1);
      expect(library.games.length).toBe(1);
      expect(library.games[0].title).toBe('Test Game');
    });

    const req = httpMock.expectOne('/api/game-library');
    expect(req.request.method).toBe('GET');
    req.flush(dummyLibrary);
  });

  it('should propagate errors when the API call fails', () => {
    service.getGameLibrary().subscribe({
      next: () => fail('Should have failed with a 404 error'),
      error: error => {
        expect(error.status).toBe(404);
      }
    });

    const req = httpMock.expectOne('/api/game-library');
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  });
}); 
