describe('Registration Form', () => {
  beforeEach(() => {
    cy.visit('/register');
    cy.intercept('POST', '/api/users/register').as('registerUser');
  });

  it('should display registration form with all fields', () => {
    cy.get('h2').should('contain', 'Create Account');
    cy.get('#username').should('exist');
    cy.get('#email').should('exist');
    cy.get('#password').should('exist');
    cy.get('#confirmPassword').should('exist');
    cy.get('#bio').should('exist');
    cy.get('#profilePicture').should('exist');
    cy.get('p-button[type="submit"]').should('exist');
  });

  it('should show validation errors for required fields', () => {
    cy.get('form').should('be.visible');
    
    cy.get('#username').focus().blur();
    cy.get('#email').focus().blur();
    cy.get('#password').find('input').focus().blur();
    
    cy.get('p-button[type="submit"]').click();
    
    cy.get('small.text-red-500').should('exist');
    cy.get('small.text-red-500').should('have.length.at.least', 3);
    cy.get('small.text-red-500').contains('Username is required');
    cy.get('small.text-red-500').contains('Email is required');
    cy.get('small.text-red-500').contains('Password is required');
  });

  it('should validate username format', () => {
    cy.get('#username').type('u').blur();
    cy.get('small.text-red-500').should('contain', 'Username must be at least 3 characters');

    cy.get('#username').clear().type('user@name').blur();
    cy.get('small.text-red-500').should('contain', 'Username can only contain letters, numbers, underscores, and hyphens');

    cy.get('#username').clear().type('validusername').blur();
    cy.get('small.text-red-500').should('not.exist');
  });

  it('should validate email format', () => {
    cy.get('#email').type('invalid-email').blur();
    cy.get('small.text-red-500').should('contain', 'Please enter a valid email address');

    cy.get('#email').clear().type('valid@email.com').blur();
    cy.get('small.text-red-500').should('not.exist');
  });

  it('should validate password requirements', () => {
    cy.get('#password').find('input').type('weak').blur();
    cy.get('small.text-red-500').should('contain', 'Password must be at least 8 characters');

    cy.get('#password').find('input').clear().type('weakpass').blur();
    cy.get('small.text-red-500').should('contain', 'Password must contain at least one uppercase letter, one lowercase letter, and one number');

    cy.get('#password').find('input').clear().type('ValidPass123').blur();
    cy.get('small.text-red-500').should('not.exist');
  });

  it('should validate password confirmation', () => {
    // Type in password field and wait for any overlay to settle
    cy.get('#password').find('input').type('ValidPass123');
    cy.wait(100); // Brief wait for any overlay animations
    
    // Type in confirm password field
    cy.get('#confirmPassword').find('input').type('DifferentPass123').blur();
    cy.get('small.text-red-500').should('contain', 'Passwords do not match');

    cy.get('#confirmPassword').find('input').clear().type('ValidPass123').blur();
    cy.get('small.text-red-500').should('not.exist');
  });

  it('should validate optional profile picture URL', () => {
    cy.get('#profilePicture').type('invalid-url').blur();
    cy.get('small.text-red-500').should('contain', 'Please enter a valid image URL');

    cy.get('#profilePicture').clear().type('https://example.com/image.jpg').blur();
    cy.get('small.text-red-500').should('not.exist');
  });

  it('should successfully submit valid registration form', () => {
    const testUser = {
      username: 'testuser',
      email: 'test@example.com',
      password: 'TestPass123',
      bio: 'Test bio',
      profilePictureUrl: 'https://example.com/image.jpg'
    };

    // Intercept the registration request
    cy.intercept('POST', '/api/users/register').as('registerUser');

    cy.fillRegistrationForm(
      testUser.username,
      testUser.email,
      testUser.password,
      testUser.bio,
      testUser.profilePictureUrl
    );

    cy.get('p-button[type="submit"]').click();

    cy.wait('@registerUser').then((interception) => {
      // Log the request and response for debugging
      cy.log('Request:', JSON.stringify(interception.request?.body));
      cy.log('Response:', JSON.stringify(interception.response?.body));
      
      // Verify request body
      cy.wrap(interception.request?.body).should('deep.equal', {
        username: testUser.username,
        email: testUser.email,
        password: testUser.password,
        bio: testUser.bio,
        profilePictureUrl: testUser.profilePictureUrl
      });

      // Verify response
      cy.wrap(interception.response?.statusCode).should('eq', 201);
      cy.wrap(interception.response?.body).should('have.property', 'username', testUser.username);
      cy.wrap(interception.response?.body).should('have.property', 'email', testUser.email);
    });

    // Verify success message
    cy.get('p-toast').should('contain', 'Registration Successful');
  });

  it('should handle registration error from server', () => {
    const existingUser = {
      username: 'existinguser',
      email: 'test@example.com',
      password: 'TestPass123'
    };

    // Intercept with explicit error response
    cy.intercept('POST', '/api/users/register', {
      statusCode: 400,
      body: {
        message: 'Username already exists',
        status: 'BAD_REQUEST',
        errors: {
          username: 'Username is already taken'
        }
      }
    }).as('registerError');

    cy.fillRegistrationForm(existingUser.username, existingUser.email, existingUser.password);
    cy.get('p-button[type="submit"]').click();

    cy.wait('@registerError').then((interception) => {
      // Verify response
      cy.wrap(interception.response?.statusCode).should('eq', 400);
      cy.wrap(interception.response?.body.message).should('eq', 'Username already exists');
    });

    // Verify error toast - using same pattern as success test
    cy.get('p-toast').should('contain', 'Registration Failed');
  });

  it('should disable submit button while loading', () => {
    cy.intercept('POST', '/api/users/register', (req) => {
      req.reply({ delay: 1000, statusCode: 201 });
    }).as('slowRegister');

    cy.fillRegistrationForm('testuser', 'test@example.com', 'TestPass123');
    
    // Get the button and verify initial state
    cy.get('p-button[type="submit"]').should('exist');
    cy.get('p-button[type="submit"] button').should('not.be.disabled');
    
    // Click submit and verify loading state
    cy.get('p-button[type="submit"]').click();
    cy.get('p-button[type="submit"] button').should('be.disabled');
    cy.get('p-button[type="submit"]').should('contain', 'Creating Account...');

    // Wait for request to complete
    cy.wait('@slowRegister');
    
    // Verify button returns to normal state
    cy.get('p-button[type="submit"] button').should('not.be.disabled');
    cy.get('p-button[type="submit"]').should('contain', 'Create Account');
  });
}); 
