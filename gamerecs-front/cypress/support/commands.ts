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

export {};

// Add the type definition to the Chainable interface
declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Cypress {
    interface Chainable {
      checkToast(severity: 'success' | 'error', summary: string, detail: string): Chainable<void>;
      checkPrimeNGCheckbox(selector: string): Chainable<void>;
    }
  }
} 
