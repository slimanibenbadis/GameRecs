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

    // Type in username field
    cy.get('[data-cy="username-input"]')
      .should('be.visible')
      .clear()
      .type('e2e_testuser');

    // Type in password field
    cy.get('[data-cy="password-input"] input')
      .should('be.visible')
      .clear()
      .type('Test1234');

    // Ensure form is valid before clicking
    cy.get('[data-cy="login-button"] button')
      .should('not.be.disabled')
      .click();
    
    // Verify navigation
    cy.url().should('include', '/profile');
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


    cy.get('[data-cy="username-input"]').should('be.visible').type('e2e_testuser');
    cy.get('[data-cy="password-input"] input').should('be.visible').type('Test1234');

    // Use the new custom command for PrimeNG checkbox
    cy.checkPrimeNGCheckbox('[data-cy="remember-me-checkbox"]');
    
    // Remove redundant check since it's now in the command
    cy.get('[data-cy="login-button"]').should('be.visible').click();
    cy.wait(1000);


    cy.reload();
    cy.url().should('include', '/profile');
  });

  it('should redirect to login when accessing protected route without auth', () => {
    cy.visit('/profile');
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

describe('Google OAuth Login Flow', () => {
  const mockGoogleUser = {
    email: 'testuser@example.com',
    name: 'Test User',
    picture: 'https://example.com/avatar.jpg'
  };

  beforeEach(() => {
    cy.intercept('GET', '**/oauth2/authorization/google', (req) => {
      req.redirect(`/auth/google/callback?token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6InRlc3R1c2VyQGV4YW1wbGUuY29tIiwibmFtZSI6IlRlc3QgVXNlciJ9.mock-signature`);
    }).as('googleAuthInit');
    
    cy.intercept('GET', '**/api/auth/google/callback*', {
      delay: 1000,  // Simulate 1s network latency
      statusCode: 200,
      body: {
        token: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6InRlc3R1c2VyQGV4YW1wbGUuY29tIiwibmFtZSI6IlRlc3QgVXNlciJ9.mock-signature',
        username: mockGoogleUser.email,
        email: mockGoogleUser.email,
        emailVerified: true,
        googleId: 'mock_google_id'
      }

    }).as('googleCallback');
  });

  it('should complete Google OAuth flow successfully', () => {
    cy.visit('/auth/login');
    
    cy.get('[data-cy="google-login-button"]').click();
    
    // First verify callback component mounted
    cy.location('pathname', { timeout: 10000 })
      .should('include', '/auth/google/callback');
    
    // Then check for loading indicator
    cy.get('[data-cy="google-callback-loading"]', { timeout: 15000 })
      .should('be.visible');

    cy.wait('@googleCallback', { timeout: 15000 }).then((interception) => {
      expect(interception.request.url).to.include('code=mock_auth_code');
    });

    cy.url().should('include', '/profile');
  });

  it('should handle Google OAuth failure', () => {
    cy.intercept('GET', '**/api/auth/google/callback*', {
      delay: 1000,  // Add delay here too
      statusCode: 401,
      body: { message: 'Google authentication failed' }
    }).as('failedGoogleCallback');

    cy.visit('/auth/login');
    cy.get('[data-cy="google-login-button"]').click();
    
    // Wait for callback component
    cy.get('[data-cy="google-callback-loading"]', { timeout: 10000 })
      .should('be.visible');

    cy.wait('@failedGoogleCallback', { timeout: 15000 });
    cy.url().should('include', '/auth/login');
  });
}); 
