describe('Email Verification Flow', () => {
  beforeEach(() => {
    // Reset any previous state
    cy.intercept('GET', '/api/users/verify*', {
      statusCode: 200,
      body: {
        message: 'Email Verified Successfully!'
      }
    }).as('verifyEmail');
  });

  it('should show success message when token is valid', () => {
    // Visit verification page with valid token
    cy.visit('/verify?token=valid-token');
    
    // Wait for verification API call
    cy.wait('@verifyEmail');
    
    // Check success message is displayed
    cy.get('.text-green-600, .text-green-400').within(() => {
      cy.get('.pi-check-circle').should('be.visible');
      cy.contains('Email Verified Successfully!').should('be.visible');
      cy.contains('You can now proceed to login and start using Gamer-Reco.').should('be.visible');
    });

    // Check success button
    cy.get('button.p-button-success').should('be.visible')
      .and('contain', 'Go to Login');

    // Verify toast message
    cy.checkToast('success', 'Success', 'Your email has been successfully verified!');
  });

  it('should show error message when token is invalid', () => {
    // Mock invalid token response
    cy.intercept('GET', '/api/users/verify*', {
      statusCode: 400,
      body: {
        message: 'Invalid verification link. Please request a new one.'
      }
    }).as('verifyInvalidToken');

    // Visit verification page with invalid token
    cy.visit('/verify?token=invalid-token');
    
    // Wait for verification API call
    cy.wait('@verifyInvalidToken');
    
    // Check error message is displayed
    cy.get('.text-red-600, .text-red-400').within(() => {
      cy.get('.pi-times-circle').should('be.visible');
      cy.contains('Verification Failed').should('be.visible');
      cy.contains('Invalid verification link. Please request a new one.').should('be.visible');
    });

    // Check error button
    cy.get('button.p-button-primary').should('be.visible')
      .and('contain', 'Back to Registration');

    // Verify toast message
    cy.checkToast('error', 'Verification Failed', 'Invalid verification link. Please request a new one.');
  });

  it('should show error when token is expired', () => {
    // Mock expired token response
    cy.intercept('GET', '/api/users/verify*', {
      statusCode: 400,
      body: {
        message: 'Verification token has expired'
      }
    }).as('verifyExpiredToken');

    // Visit verification page with expired token
    cy.visit('/verify?token=expired-token');
    
    // Wait for verification API call
    cy.wait('@verifyExpiredToken');
    
    // Check error message is displayed
    cy.get('.text-red-600, .text-red-400').within(() => {
      cy.get('.pi-times-circle').should('be.visible');
      cy.contains('Verification Failed').should('be.visible');
      cy.contains('Verification token has expired').should('be.visible');
    });

    // Check error button
    cy.get('button.p-button-primary').should('be.visible')
      .and('contain', 'Back to Registration');

    // Verify toast message
    cy.checkToast('error', 'Verification Failed', 'Verification token has expired');
  });

  it('should handle missing token parameter', () => {
    // Visit verification page without token
    cy.visit('/verify');
    
    // Wait for loading state to be removed
    cy.get('p-progressspinner').should('not.exist');
    
    // Check error message is displayed
    cy.get('.text-red-600, .text-red-400').within(() => {
      cy.get('.pi-times-circle').should('be.visible');
      cy.contains('Verification Failed').should('be.visible');
      cy.contains('Invalid verification link. Please request a new one.').should('be.visible');
    });

    // Check error button
    cy.get('button.p-button-primary').should('be.visible')
      .and('contain', 'Back to Registration');

    // Verify toast message with increased timeout
    cy.checkToast('error', 'Verification Failed', 'Invalid verification link. Please request a new one.');
  });

  it('should show loading state while verifying', () => {
    // Mock delayed response
    cy.intercept('GET', '/api/users/verify*', {
      statusCode: 200,
      body: {
        message: 'Email Verified Successfully!'
      },
      delay: 1000
    }).as('verifyEmailDelayed');

    // Visit verification page
    cy.visit('/verify?token=valid-token');
    
    // Check loading state is shown
    cy.get('p-progressspinner').should('be.visible');
    cy.contains('Verifying your email...').should('be.visible');
    
    // Wait for verification to complete
    cy.wait('@verifyEmailDelayed');
    
    // Check loading state is removed
    cy.get('p-progressspinner').should('not.exist');
    cy.contains('Verifying your email...').should('not.exist');
  });

  it('should handle server errors gracefully', () => {
    // Mock server error
    cy.intercept('GET', '/api/users/verify*', {
      statusCode: 500,
      body: {
        message: 'Internal server error'
      }
    }).as('verifyServerError');

    // Visit verification page
    cy.visit('/verify?token=valid-token');
    
    // Wait for verification API call
    cy.wait('@verifyServerError');
    
    // Check error message is displayed
    cy.get('.text-red-600, .text-red-400').within(() => {
      cy.get('.pi-times-circle').should('be.visible');
      cy.contains('Verification Failed').should('be.visible');
      cy.contains('Internal server error').should('be.visible');
    });

    // Check error button
    cy.get('button.p-button-primary').should('be.visible')
      .and('contain', 'Back to Registration');

    // Verify toast message
    cy.checkToast('error', 'Verification Failed', 'Internal server error');
  });
}); 
