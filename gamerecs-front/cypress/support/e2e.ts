// Import commands.js using ES2015 syntax:
import './commands';

// Set testing environment
import { environment } from '../../src/environments/environment.test';
(window as any).environment = environment;

// Alternatively you can use CommonJS syntax:
// require('./commands');

declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Cypress {
    interface Chainable {
      /**
       * Custom command to fill registration form
       * @param username - The username to enter
       * @param email - The email to enter
       * @param password - The password to enter
       * @param bio - Optional bio text
       * @param profilePictureUrl - Optional profile picture URL
       */
      fillRegistrationForm(
        username: string,
        email: string,
        password: string,
        bio?: string,
        profilePictureUrl?: string
      ): Chainable<void>;

      /**
       * Custom command to check toast message
       * @param severity - The severity of the toast (success, error, warn, info)
       * @param summary - The summary text to check for
       * @param detail - The detail text to check for
       */
      checkToast(
        severity: string,
        summary: string,
        detail: string
      ): Chainable<void>;
    }
  }
}

// Hide fetch/XHR requests from command log
const app = window.top;
if (app) {
  app.document.addEventListener('DOMContentLoaded', () => {
    const style = app.document.createElement('style');
    style.innerHTML = '.command-name-request, .command-name-xhr { display: none }';
    app.document.head.appendChild(style);
  });
} 
