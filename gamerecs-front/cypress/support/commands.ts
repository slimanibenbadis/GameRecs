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

Cypress.Commands.add('checkToast', (severity: string, summary: string, detail: string) => {
  // Wait for toast container with longer timeout
  cy.get('p-toast', { timeout: 10000 }).should('exist');
  
  // Use contains instead of find to locate the message
  cy.contains('.p-toast-message', detail, { timeout: 10000 })
    .should('be.visible')
    .should('have.class', `p-toast-message-${severity}`)
    .within(() => {
      // Use contains for text content to be more resilient
      cy.contains('.p-toast-summary', summary).should('be.visible');
      cy.contains('.p-toast-detail', detail).should('be.visible');
    });
});

export {};

// Add the type definition to the Chainable interface
declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Cypress {
    interface Chainable {
      checkToast(
        severity: string,
        summary: string,
        detail: string
      ): Chainable<void>;
    }
  }
} 
