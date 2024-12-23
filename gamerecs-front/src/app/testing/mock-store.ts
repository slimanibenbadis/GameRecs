import { Store } from '@ngrx/store';
import { TestBed } from '@angular/core/testing';
import { provideMockStore } from '@ngrx/store/testing';

interface IState {
  [key: string]: unknown;
}

/**
 * Creates a mock store for testing
 * @param initialState Initial state for the store
 * @returns MockStore instance
 */
export function createMockStore(initialState: IState = {}): Store<IState> {
  TestBed.configureTestingModule({
    providers: [
      provideMockStore({ initialState })
    ]
  });

  return TestBed.inject(Store);
} 