import { MockStore, provideMockStore } from '@ngrx/store/testing';
import { TestBed } from '@angular/core/testing';

/**
 * Creates a mock NgRx store with initial state for testing
 * @param initialState The initial state to use for the store
 * @returns MockStore instance
 */
export function createMockStore(initialState: any = {}): MockStore {
  TestBed.configureTestingModule({
    providers: [
      provideMockStore({ initialState })
    ]
  });

  return TestBed.inject(MockStore);
}

/**
 * Helper to create mock selectors for testing
 * @param mockStore The mock store instance
 * @param selectorMap Map of selector functions to their mock values
 */
export function setMockSelectors(mockStore: MockStore, selectorMap: Record<string, any>): void {
  Object.entries(selectorMap).forEach(([key, value]) => {
    const selector = mockStore.overrideSelector(key, value);
    mockStore.refreshState();
    selector.setResult(value);
  });
} 