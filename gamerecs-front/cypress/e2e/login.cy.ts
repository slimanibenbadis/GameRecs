describe('Login Flow', () => {
  beforeEach(() => {
    cy.visit('/auth/login');
    // Wait for the form to be fully loaded and initialized
    cy.get('form').should('be.visible');
  });

  it('should display login form with all fields', () => {
    cy.get('[data-cy="username-input"]').should('be.visible');
    cy.get('[data-cy="password-input"]').should('be.visible');
    cy.get('[data-cy="remember-me-checkbox"]').should('be.visible');
    cy.get('[data-cy="login-button"]').should('be.visible');
  });

  it('should show validation errors for required fields', () => {
    // First verify the form is in its initial state
    cy.get('[data-cy="username-input"]').should('be.visible');
    cy.get('[data-cy="password-input"]').should('be.visible');
    
    // Click outside the form to ensure focus events are triggered
    cy.get('body').click();
    
    // Verify error messages are visible
    cy.get('[data-cy="username-error"]')
      .should('be.visible')
      .and('contain', 'Username is required');
    
    cy.get('[data-cy="password-error"]')
      .should('be.visible')
      .and('contain', 'Password is required');
    
    // Verify the submit button is disabled
    cy.get('[data-cy="login-button"] button').should('be.disabled');
  });

  it('should successfully login with valid credentials', () => {
    // More specific intercept with full URL pattern
    cy.intercept({
      method: 'POST',
      url: '**/api/auth/login',
    }, {
      statusCode: 200,
      body: {
        token: 'fake-jwt-token',
        username: 'testuser',
        email: 'test@example.com',
        emailVerified: true
      }
    }).as('loginRequest');

    // Type in username field
    cy.get('[data-cy="username-input"]')
      .should('be.visible')
      .clear()
      .type('testuser');

    // Type in password field
    cy.get('[data-cy="password-input"] input')
      .should('be.visible')
      .clear()
      .type('Password123!');

    // Ensure form is valid before clicking
    cy.get('[data-cy="login-button"] button')
      .should('not.be.disabled')
      .click();

    // Wait for request with longer timeout
    cy.wait('@loginRequest', { timeout: 10000 });
    
    // Verify navigation
    cy.url().should('include', '/health');
  });

  it('should handle invalid credentials', () => {
    // Intercept with full URL pattern
    cy.intercept('POST', '**/api/auth/login', {
      statusCode: 401,
      body: {
        status: 401,
        message: 'Invalid username or password'
      }
    }).as('loginFailure');

    // Clear fields and type with proper waiting
    cy.get('[data-cy="username-input"]')
      .should('be.visible')
      .clear()
      .type('wronguser');

    cy.get('[data-cy="password-input"] input')
      .should('be.visible')
      .clear()
      .type('WrongPass123!');

    // Ensure form is valid before clicking
    cy.get('[data-cy="login-button"] button')
      .should('not.be.disabled')
      .click();

    // Wait for request with increased timeout
    cy.wait('@loginFailure', { timeout: 10000 });

    // Verify error message with retry and timeout
    cy.get('[data-cy="error-message"]', { timeout: 10000 })
      .should('be.visible')
      .and('contain', 'Invalid username or password');
  });

  it('should persist session with "Remember me" checked', () => {
    cy.intercept('POST', '**/api/auth/login', {
      statusCode: 200,
      body: {
        token: 'fake-jwt-token',
        username: 'testuser',
        email: 'test@example.com',
        emailVerified: true
      }
    }).as('loginRequest');

    cy.get('[data-cy="username-input"]').should('be.visible').type('testuser');
    cy.get('[data-cy="password-input"] input').should('be.visible').type('Password123!');
    
    // Use the new custom command for PrimeNG checkbox
    cy.checkPrimeNGCheckbox('[data-cy="remember-me-checkbox"]');
    
    // Remove redundant check since it's now in the command
    cy.get('[data-cy="login-button"]').should('be.visible').click();

    cy.wait('@loginRequest');
    cy.reload();
    cy.url().should('include', '/health');
  });

  it('should redirect to login when accessing protected route without auth', () => {
    cy.visit('/health');
    cy.url().should('include', '/auth/login');
  });

  it('should handle unverified email error', () => {
    // Intercept with full URL pattern and proper response structure
    cy.intercept('POST', '**/api/auth/login', {
      statusCode: 401,
      body: {
        status: 401,
        message: 'Please verify your email before logging in',
        timestamp: new Date().toISOString()
      }
    }).as('loginUnverified');

    // Fill in the form
    cy.get('[data-cy="username-input"]')
      .should('be.visible')
      .clear()
      .type('unverified');

    cy.get('[data-cy="password-input"] input')
      .should('be.visible')
      .clear()
      .type('ValidPass123!');

    // Submit form and wait for button to be enabled
    cy.get('[data-cy="login-button"] button')
      .should('not.be.disabled')
      .click();

    // Wait for the request with timeout
    cy.wait('@loginUnverified', { timeout: 10000 })
      .its('response.statusCode')
      .should('eq', 401);

    // Add a small delay to ensure state updates are processed
    cy.wait(100);

    // Verify error message with retry and timeout
    cy.get('[data-cy="error-message"]', { timeout: 10000 })
      .should('exist')
      .should('be.visible')
      .invoke('text')
      .should('include', 'Please verify your email before logging in');

    // Log the error message for debugging
    cy.get('[data-cy="error-message"]').then($el => {
      cy.log('Actual error message:', $el.text());
    });
  });

  it('should disable login button while request is in progress', () => {
    // Intercept login request with delay
    cy.intercept('POST', '**/api/auth/login', {
      delay: 1000,
      statusCode: 200,
      body: {
        token: 'fake-jwt-token',
        username: 'testuser',
        email: 'test@example.com',
        emailVerified: true
      }
    }).as('loginRequest');

    // Fill in form fields
    cy.get('[data-cy="username-input"]')
      .should('be.visible')
      .clear()
      .type('testuser');

    cy.get('[data-cy="password-input"] input')
      .should('be.visible')
      .clear()
      .type('Password123!');

    // Get button reference before clicking
    cy.get('[data-cy="login-button"] button')
      .as('loginButton')
      .should('not.be.disabled');

    // Click the button
    cy.get('@loginButton').click();

    // Verify button is disabled and shows loading state
    cy.get('@loginButton')
      .should('be.disabled')
      .parent()
      .should('contain', 'Logging in...');

    // Wait for request to complete
    cy.wait('@loginRequest', { timeout: 10000 });
  });
}); 
