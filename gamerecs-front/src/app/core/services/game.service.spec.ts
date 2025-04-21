import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { GameService, GameSearchResponse, IgdbUpdateResponse } from './game.service';
import { HttpErrorResponse } from '@angular/common/http';
import { MessageService } from 'primeng/api';
import { GameDto } from '../../models/game.dto';

describe('GameService', () => {
  let service: GameService;
  let httpMock: HttpTestingController;
  let messageService: MessageService;

  // Mock response data for game by ID
  const mockGameDto: GameDto = {
    id: 42,
    title: 'Test Game',
    description: 'A test game description',
    releaseDate: '2023-01-01',
    coverImageUrl: 'http://example.com/cover.jpg',
    genres: [{ id: 1, name: 'RPG' }],
    platforms: [{ id: 1, name: 'PC' }],
    publishers: [{ id: 1, name: 'Test Publisher' }],
    developers: [{ id: 1, name: 'Test Developer' }],
    userRating: 85,
    percentileRank: 90,
    predictedRating: 82,
    backlogStatus: 'In Progress'
  };

  // Mock response data
  const mockGameSearchResponse: GameSearchResponse = {
    games: [
      {
        gameId: 1,
        igdbId: 101,
        title: 'Test Game',
        description: 'A test game',
        releaseDate: '2023-01-01',
        coverImageUrl: 'http://example.com/cover.jpg',
        updatedAt: '2023-01-01T12:00:00'
      }
    ],
    currentPage: 0,
    totalPages: 1,
    totalElements: 1,
    pageSize: 50,
    query: 'test'
  };

  const mockIgdbUpdateResponse: IgdbUpdateResponse = {
    message: 'IGDB data updated successfully',
    data: [{
      id: 101,
      name: 'Test Game'
    }]
  };

  beforeEach(() => {
    // Create mock for MessageService
    const messageServiceSpy = jasmine.createSpyObj('MessageService', ['add']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        GameService,
        { provide: MessageService, useValue: messageServiceSpy }
      ]
    });
    service = TestBed.inject(GameService);
    httpMock = TestBed.inject(HttpTestingController);
    messageService = TestBed.inject(MessageService);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // Basic service tests
  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // getGameById method tests
  describe('getGameById', () => {
    it('should return a game by ID', () => {
      const gameId = 42;
      
      service.getGameById(gameId).subscribe(game => {
        expect(game).toEqual(mockGameDto);
        expect(game.id).toBe(gameId);
        expect(game.title).toBe('Test Game');
      });

      const req = httpMock.expectOne(`/api/games/${gameId}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockGameDto);
    });

    it('should show error toast when API returns an error', () => {
      const gameId = 999;
      
      service.getGameById(gameId).subscribe({
        next: () => fail('Should have thrown an error'),
        error: err => {
          expect(err.status).toBe(404);
        }
      });

      const req = httpMock.expectOne(`/api/games/${gameId}`);
      req.flush('Game not found', { status: 404, statusText: 'Not Found' });
      
      // Verify toast was displayed
      expect(messageService.add).toHaveBeenCalledWith({
        severity: 'error',
        summary: 'Error',
        detail: 'Could not load game'
      });
    });
  });

  // searchGames method tests
  describe('searchGames', () => {
    it('should return search results for valid query', () => {
      service.searchGames('test').subscribe(response => {
        expect(response).toEqual(mockGameSearchResponse);
        expect(response.games.length).toBe(1);
        expect(response.query).toBe('test');
      });

      const req = httpMock.expectOne(request => 
        request.url === '/api/games/search' && 
        request.params.get('query') === 'test'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockGameSearchResponse);
    });

    it('should trim whitespace from query', () => {
      service.searchGames('  test  ').subscribe();

      const req = httpMock.expectOne(request => 
        request.url === '/api/games/search' && 
        request.params.get('query') === 'test'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockGameSearchResponse);
    });

    it('should validate and correct pagination parameters', () => {
      service.searchGames('test', -1, 200).subscribe();

      const req = httpMock.expectOne(request => 
        request.url === '/api/games/search' && 
        request.params.get('page') === '0' && // Should correct negative to 0
        request.params.get('size') === '100'  // Should cap at 100
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockGameSearchResponse);
    });

    it('should use default pagination when not specified', () => {
      service.searchGames('test').subscribe();

      const req = httpMock.expectOne(request => 
        request.url === '/api/games/search' && 
        request.params.get('page') === '0' && 
        request.params.get('size') === '50'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockGameSearchResponse);
    });

    it('should enforce minimum size', () => {
      service.searchGames('test', 0, 0).subscribe();

      const req = httpMock.expectOne(request => 
        request.url === '/api/games/search' && 
        request.params.get('size') === '1' // Should enforce minimum of 1
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockGameSearchResponse);
    });

    it('should retry once on failure', () => {
      service.searchGames('test').subscribe({
        next: response => expect(response).toEqual(mockGameSearchResponse),
        error: () => fail('Expected successful response after retry')
      });

      // First request fails
      const firstReq = httpMock.expectOne(request => 
        request.url === '/api/games/search' && 
        request.params.get('query') === 'test'
      );
      firstReq.error(new ErrorEvent('Network error'));

      // Second request (retry) succeeds
      const retryReq = httpMock.expectOne(request => 
        request.url === '/api/games/search' && 
        request.params.get('query') === 'test'
      );
      retryReq.flush(mockGameSearchResponse);
    });

    it('should throw error for query less than 2 characters', () => {
      service.searchGames('a').subscribe({
        next: () => fail('Should have thrown an error'),
        error: error => expect(error.message).toBe('Search query must be at least 2 characters long')
      });

      // No HTTP request should be made
      httpMock.expectNone('/api/games/search');
    });

    it('should throw error for empty query', () => {
      service.searchGames('').subscribe({
        next: () => fail('Should have thrown an error'),
        error: error => expect(error.message).toBe('Search query must be at least 2 characters long')
      });

      // No HTTP request should be made
      httpMock.expectNone('/api/games/search');
    });

    it('should throw error for null query', () => {
      service.searchGames(null as any).subscribe({
        next: () => fail('Should have thrown an error'),
        error: error => expect(error.message).toBe('Search query must be at least 2 characters long')
      });

      // No HTTP request should be made
      httpMock.expectNone('/api/games/search');
    });

    it('should handle error from the API', () => {
      service.searchGames('test').subscribe({
        next: () => fail('Should have thrown an error'),
        error: error => expect(error.status).toBe(500)
      });

      const req = httpMock.expectOne(request => 
        request.url === '/api/games/search' && 
        request.params.get('query') === 'test'
      );
      req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });

      // Retry should also fail
      const retryReq = httpMock.expectOne(request => 
        request.url === '/api/games/search' && 
        request.params.get('query') === 'test'
      );
      retryReq.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
    });
  });

  // triggerIgdbUpdate method tests
  describe('triggerIgdbUpdate', () => {
    it('should send update request for valid query', () => {
      service.triggerIgdbUpdate('test').subscribe(response => {
        expect(response).toEqual(mockIgdbUpdateResponse);
        expect(response.message).toBe('IGDB data updated successfully');
      });

      const req = httpMock.expectOne(request => 
        request.url === '/api/igdb/update' && 
        request.params.get('query') === 'test'
      );
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toBeNull();
      req.flush(mockIgdbUpdateResponse);
    });

    it('should trim whitespace from query', () => {
      service.triggerIgdbUpdate('  test  ').subscribe();

      const req = httpMock.expectOne(request => 
        request.url === '/api/igdb/update' && 
        request.params.get('query') === 'test'
      );
      expect(req.request.method).toBe('POST');
      req.flush(mockIgdbUpdateResponse);
    });

    it('should throw error for query less than 2 characters', () => {
      service.triggerIgdbUpdate('a').subscribe({
        next: () => fail('Should have thrown an error'),
        error: error => expect(error.message).toBe('Update query must be at least 2 characters long')
      });

      // No HTTP request should be made
      httpMock.expectNone('/api/igdb/update');
    });

    it('should throw error for empty query', () => {
      service.triggerIgdbUpdate('').subscribe({
        next: () => fail('Should have thrown an error'),
        error: error => expect(error.message).toBe('Update query must be at least 2 characters long')
      });

      // No HTTP request should be made
      httpMock.expectNone('/api/igdb/update');
    });

    it('should throw error for null query', () => {
      service.triggerIgdbUpdate(null as any).subscribe({
        next: () => fail('Should have thrown an error'),
        error: error => expect(error.message).toBe('Update query must be at least 2 characters long')
      });

      // No HTTP request should be made
      httpMock.expectNone('/api/igdb/update');
    });

    it('should handle error from the API', () => {
      service.triggerIgdbUpdate('test').subscribe({
        next: () => fail('Should have thrown an error'),
        error: error => expect(error.status).toBe(500)
      });

      const req = httpMock.expectOne(request => 
        request.url === '/api/igdb/update' && 
        request.params.get('query') === 'test'
      );
      req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
    });
  });

  // updateAndSearch method tests
  describe('updateAndSearch', () => {
    it('should send update-and-search request for valid query', () => {
      service.updateAndSearch('test').subscribe(response => {
        expect(response).toEqual(mockGameSearchResponse);
      });

      const req = httpMock.expectOne(request => 
        request.url === '/api/igdb/update-and-search' && 
        request.params.get('query') === 'test'
      );
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toBeNull();
      req.flush(mockGameSearchResponse);
    });

    it('should trim whitespace from query', () => {
      service.updateAndSearch('  test  ').subscribe();

      const req = httpMock.expectOne(request => 
        request.url === '/api/igdb/update-and-search' && 
        request.params.get('query') === 'test'
      );
      expect(req.request.method).toBe('POST');
      req.flush(mockGameSearchResponse);
    });

    it('should validate and correct pagination parameters', () => {
      service.updateAndSearch('test', -1, 200).subscribe();

      const req = httpMock.expectOne(request => 
        request.url === '/api/igdb/update-and-search' && 
        request.params.get('page') === '0' && // Should correct negative to 0
        request.params.get('size') === '100'  // Should cap at 100
      );
      expect(req.request.method).toBe('POST');
      req.flush(mockGameSearchResponse);
    });

    it('should use default pagination when not specified', () => {
      service.updateAndSearch('test').subscribe();

      const req = httpMock.expectOne(request => 
        request.url === '/api/igdb/update-and-search' && 
        request.params.get('page') === '0' && 
        request.params.get('size') === '50'
      );
      expect(req.request.method).toBe('POST');
      req.flush(mockGameSearchResponse);
    });

    it('should enforce minimum size', () => {
      service.updateAndSearch('test', 0, 0).subscribe();

      const req = httpMock.expectOne(request => 
        request.url === '/api/igdb/update-and-search' && 
        request.params.get('size') === '1' // Should enforce minimum of 1
      );
      expect(req.request.method).toBe('POST');
      req.flush(mockGameSearchResponse);
    });

    it('should throw error for query less than 2 characters', () => {
      service.updateAndSearch('a').subscribe({
        next: () => fail('Should have thrown an error'),
        error: error => expect(error.message).toBe('Search query must be at least 2 characters long')
      });

      // No HTTP request should be made
      httpMock.expectNone('/api/igdb/update-and-search');
    });

    it('should throw error for empty query', () => {
      service.updateAndSearch('').subscribe({
        next: () => fail('Should have thrown an error'),
        error: error => expect(error.message).toBe('Search query must be at least 2 characters long')
      });

      // No HTTP request should be made
      httpMock.expectNone('/api/igdb/update-and-search');
    });

    it('should throw error for null query', () => {
      service.updateAndSearch(null as any).subscribe({
        next: () => fail('Should have thrown an error'),
        error: error => expect(error.message).toBe('Search query must be at least 2 characters long')
      });

      // No HTTP request should be made
      httpMock.expectNone('/api/igdb/update-and-search');
    });

    it('should handle error from the API', () => {
      service.updateAndSearch('test').subscribe({
        next: () => fail('Should have thrown an error'),
        error: error => expect(error.status).toBe(500)
      });

      const req = httpMock.expectOne(request => 
        request.url === '/api/igdb/update-and-search' && 
        request.params.get('query') === 'test'
      );
      req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
    });
  });

  // Error handling tests
  describe('error handling', () => {
    // Test various error types and formats that the private handleError and getErrorMessage methods handle
    
    it('should handle client-side error', () => {
      service.searchGames('test').subscribe({
        next: () => fail('Should have thrown an error'),
        error: error => {
          console.log('Actual error message:', error.message);
          expect(error.message).toBe('Client error: Network error');
        }
      });

      const req = httpMock.expectOne(r => r.url.includes('/api/games/search'));
      req.error(new ErrorEvent('Network error', { message: 'Network error' }));
      
      // Retry request also fails with the same error
      const retryReq = httpMock.expectOne(r => r.url.includes('/api/games/search'));
      retryReq.error(new ErrorEvent('Network error', { message: 'Network error' }));
    });

    it('should handle error with message in error object', () => {
      service.searchGames('test').subscribe({
        next: () => fail('Should have thrown an error'),
        error: error => expect(error.status).toBe(400)
      });

      const req = httpMock.expectOne(r => r.url.includes('/api/games/search'));
      req.flush({ message: 'Bad request error' }, { status: 400, statusText: 'Bad Request' });
      
      // Retry request also fails with the same error
      const retryReq = httpMock.expectOne(r => r.url.includes('/api/games/search'));
      retryReq.flush({ message: 'Bad request error' }, { status: 400, statusText: 'Bad Request' });
    });

    it('should handle error with field-specific errors', () => {
      service.triggerIgdbUpdate('test').subscribe({
        next: () => fail('Should have thrown an error'),
        error: error => expect(error.status).toBe(400)
      });

      const req = httpMock.expectOne(r => r.url.includes('/api/igdb/update'));
      req.flush({
        message: 'Validation failed',
        errors: {
          query: 'Invalid query format',
          limit: 'Limit exceeds maximum allowed'
        }
      }, { status: 400, statusText: 'Bad Request' });
    });

    it('should handle 400 status code with default message', () => {
      service.updateAndSearch('test').subscribe({
        next: () => fail('Should have thrown an error'),
        error: error => expect(error.status).toBe(400)
      });

      const req = httpMock.expectOne(r => r.url.includes('/api/igdb/update-and-search'));
      req.flush({}, { status: 400, statusText: 'Bad Request' });
    });

    it('should handle 401 status code with default message', () => {
      service.searchGames('test').subscribe({
        next: () => fail('Should have thrown an error'),
        error: error => expect(error.status).toBe(401)
      });

      const req = httpMock.expectOne(r => r.url.includes('/api/games/search'));
      req.flush({}, { status: 401, statusText: 'Unauthorized' });
      
      // Retry request also fails with the same error
      const retryReq = httpMock.expectOne(r => r.url.includes('/api/games/search'));
      retryReq.flush({}, { status: 401, statusText: 'Unauthorized' });
    });

    it('should handle 403 status code with default message', () => {
      service.searchGames('test').subscribe({
        next: () => fail('Should have thrown an error'),
        error: error => expect(error.status).toBe(403)
      });

      const req = httpMock.expectOne(r => r.url.includes('/api/games/search'));
      req.flush({}, { status: 403, statusText: 'Forbidden' });
      
      // Retry request also fails with the same error
      const retryReq = httpMock.expectOne(r => r.url.includes('/api/games/search'));
      retryReq.flush({}, { status: 403, statusText: 'Forbidden' });
    });

    it('should handle 404 status code with default message', () => {
      service.searchGames('test').subscribe({
        next: () => fail('Should have thrown an error'),
        error: error => expect(error.status).toBe(404)
      });

      const req = httpMock.expectOne(r => r.url.includes('/api/games/search'));
      req.flush({}, { status: 404, statusText: 'Not Found' });
      
      // Retry request also fails with the same error
      const retryReq = httpMock.expectOne(r => r.url.includes('/api/games/search'));
      retryReq.flush({}, { status: 404, statusText: 'Not Found' });
    });

    it('should handle 500 status code with default message', () => {
      service.searchGames('test').subscribe({
        next: () => fail('Should have thrown an error'),
        error: error => expect(error.status).toBe(500)
      });

      const req = httpMock.expectOne(r => r.url.includes('/api/games/search'));
      req.flush({}, { status: 500, statusText: 'Internal Server Error' });
      
      // Retry request also fails with the same error
      const retryReq = httpMock.expectOne(r => r.url.includes('/api/games/search'));
      retryReq.flush({}, { status: 500, statusText: 'Internal Server Error' });
    });

    it('should handle 503 status code with default message', () => {
      service.searchGames('test').subscribe({
        next: () => fail('Should have thrown an error'),
        error: error => expect(error.status).toBe(503)
      });

      const req = httpMock.expectOne(r => r.url.includes('/api/games/search'));
      req.flush({}, { status: 503, statusText: 'Service Unavailable' });
      
      // Retry request also fails with the same error
      const retryReq = httpMock.expectOne(r => r.url.includes('/api/games/search'));
      retryReq.flush({}, { status: 503, statusText: 'Service Unavailable' });
    });

    it('should handle other status codes with generic message', () => {
      service.searchGames('test').subscribe({
        next: () => fail('Should have thrown an error'),
        error: error => expect(error.status).toBe(418)
      });

      const req = httpMock.expectOne(r => r.url.includes('/api/games/search'));
      req.flush({}, { status: 418, statusText: 'I\'m a teapot' });
      
      // Retry request also fails with the same error
      const retryReq = httpMock.expectOne(r => r.url.includes('/api/games/search'));
      retryReq.flush({}, { status: 418, statusText: 'I\'m a teapot' });
    });
  });
}); 
