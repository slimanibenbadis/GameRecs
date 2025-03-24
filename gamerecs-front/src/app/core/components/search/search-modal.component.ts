import { Component, ElementRef, EventEmitter, HostListener, Output, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

/**
 * SearchModalComponent provides a modal interface for game search functionality.
 * It includes a search input field and placeholders for future features like
 * search suggestions, filters, and external library imports.
 * 
 * Features:
 * - Responsive design (90% width on mobile, 50% on desktop)
 * - Keyboard navigation (Escape to close)
 * - Focus trapping
 * - Accessibility support
 * - Smooth animations
 */
@Component({
  selector: 'app-search-modal',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    InputTextModule
  ],
  templateUrl: './search-modal.component.html',
  styleUrls: ['./search-modal.component.css']
})
export class SearchModalComponent {
  @Output() closeModal = new EventEmitter<void>();
  @ViewChild('modalContent') modalContent!: ElementRef;

  isVisible = false;
  searchQuery = '';

  /**
   * Handles the escape key press event to close the modal
   */
  @HostListener('document:keydown.escape')
  onEscapePressed() {
    this.close();
  }

  /**
   * Shows the modal and focuses the search input
   */
  show() {
    this.isVisible = true;
    // Focus the input when modal opens
    setTimeout(() => {
      const input = this.modalContent.nativeElement.querySelector('input');
      if (input) {
        input.focus();
      }
    });
  }

  /**
   * Closes the modal and resets the search query
   */
  close() {
    this.isVisible = false;
    this.searchQuery = '';
    this.closeModal.emit();
  }

  /**
   * Handles clicks on the modal backdrop to close the modal
   * @param event The mouse event from the click
   */
  closeOnBackdropClick(event: MouseEvent) {
    if (event.target === event.currentTarget) {
      this.close();
    }
  }
} 
