Cypress.Commands.add('fillRegistrationForm', (
  username: string,
  email: string,
  password: string,
  bio?: string,
  profilePictureUrl?: string
) => {
  cy.get('#username').clear().type(username);
  cy.get('#email').clear().type(email);
  cy.get('#password').find('input').clear({ force: true }).type(password, { force: true });
  cy.get('#confirmPassword').find('input').clear({ force: true }).type(password, { force: true });
  
  if (bio) {
    cy.get('#bio').clear().type(bio);
  }
  
  if (profilePictureUrl) {
    cy.get('#profilePicture').clear().type(profilePictureUrl);
  }
}); 

Cypress.Commands.add('checkToast', (severity, summary, detail) => {
  // Wait for toast container to be present
  cy.get('p-toast', { timeout: 10000 })
    .should('exist')
    .within(() => {
      // Wait for specific toast message
      cy.get('.p-toast-message', { timeout: 5000 })
        .should('have.class', `p-toast-message-${severity}`)
        .within(() => {
          if (summary) {
            cy.get('.p-toast-summary').should('contain', summary);
          }
          if (detail) {
            cy.get('.p-toast-detail').should('contain', detail);
          }
        });
    });
});

Cypress.Commands.add('checkPrimeNGCheckbox', (selector: string) => {
  // Find the hidden input and set its value
  cy.get(selector)
    .find('input[type="checkbox"]')
    .check({ force: true });
});

Cypress.Commands.add('mockGoogleOAuthFlow', (success = true) => {
  cy.intercept('GET', '**/oauth2/authorization/google', (req) => {
    const redirectUrl = success 
      ? '/auth/google/callback?code=mock_auth_code'
      : '/auth/google/callback?error=access_denied';
    req.redirect(redirectUrl);
  }).as('googleAuthInit');

  if (success) {
    cy.intercept('GET', '**/api/auth/google/callback*', {
      delay: 500,  // Simulate network latency
      statusCode: 200,
      body: {
        token: 'mock_jwt_token',
        username: 'googleuser@example.com',
        email: 'googleuser@example.com',
        emailVerified: true,
        googleId: 'mock_google_id'
      }
    }).as('googleCallback');
  }
});

export {};

// Add the type definition to the Chainable interface
declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Cypress {
    interface Chainable {
      checkToast(severity: 'success' | 'error', summary: string, detail: string): Chainable<void>;
      checkPrimeNGCheckbox(selector: string): Chainable<void>;
      mockGoogleOAuthFlow(success?: boolean): Chainable<void>;
    }
  }
} 
