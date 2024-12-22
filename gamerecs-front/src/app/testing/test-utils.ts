import { ComponentFixture } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

/**
 * Test utilities for common testing operations
 */
export class TestUtils {
  /**
   * Finds an element in the fixture by CSS selector
   */
  static queryByCss<T>(fixture: ComponentFixture<T>, selector: string): HTMLElement {
    return fixture.debugElement.query(By.css(selector))?.nativeElement;
  }

  /**
   * Finds all elements in the fixture by CSS selector
   */
  static queryAllByCss<T>(fixture: ComponentFixture<T>, selector: string): HTMLElement[] {
    return fixture.debugElement.queryAll(By.css(selector)).map(el => el.nativeElement);
  }

  /**
   * Triggers change detection and waits for it to complete
   */
  static async detectChanges<T>(fixture: ComponentFixture<T>): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
  }
} 