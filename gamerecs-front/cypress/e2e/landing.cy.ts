describe('Landing Page', () => {
  beforeEach(() => {
    cy.visit('/');
    // Wait for the main container to be visible
    cy.get('.min-h-screen').should('be.visible');
  });

  it('should display navigation bar with correct elements', () => {
    // Check logo
    cy.get('nav').find('.font-fira').should('contain', 'Gamer-Reco');
    
    // Check navigation buttons
    cy.get('nav').within(() => {
      cy.get('[routerLink="/auth/login"]').should('be.visible').and('contain', 'Login');
      cy.get('[routerLink="/auth/register"]').should('be.visible').and('contain', 'Sign Up');
    });
  });

  it('should display hero section with correct content', () => {
    // Check main heading
    cy.get('h1').should('contain', 'Find Games You\'ll').and('contain', 'Actually Love');
    
    // Check hero description
    cy.get('.text-xl.md\\:text-2xl').should('contain', 'Get highly personalized game suggestions');
    
    // Check CTA buttons
    cy.get('.cta-button').should('contain', 'Get Started');
    cy.get('.p-button-outlined.p-button-lg').should('contain', 'Learn More');
  });

  it('should display statistics section with correct numbers', () => {
    cy.get('.stats-item').should('have.length', 3);
    
    // Check individual stats
    cy.get('.stats-item').eq(0).should('contain', '10K+').and('contain', 'Active Users');
    cy.get('.stats-item').eq(1).should('contain', '50K+').and('contain', 'Games Rated');
    cy.get('.stats-item').eq(2).should('contain', '95%').and('contain', 'Happy Gamers');
  });

  it('should display features section with all feature cards', () => {
    // Check section header
    cy.get('h2').should('contain', 'Discover Your Next Favorite Game');
    
    // Check feature cards
    cy.get('.feature-card').should('have.length', 6);
    
    // Check specific features
    const features = [
      'Smart Recommendations',
      'Library Integration',
      'Backlog Management',
      'Profile System',
      'Game Discovery',
      'Easy Authentication'
    ];
    
    features.forEach(feature => {
      cy.get('.feature-card')
        .contains(feature)
        .should('be.visible');
    });
  });

  it('should have working navigation links', () => {
    // Test login button navigation
    cy.get('[routerLink="/auth/login"]').first().click();
    cy.url().should('include', '/auth/login');
    
    // Go back to landing page
    cy.visit('/');
    
    // Test register button navigation
    cy.get('[routerLink="/auth/register"]').first().click();
    cy.url().should('include', '/auth/register');
    
    // Go back to landing page
    cy.visit('/');
    
    // Test learn more button navigation
    cy.get('[routerLink="/about"]').click();
    cy.url().should('include', '/about');
  });

  it('should display footer with copyright information', () => {
    cy.get('footer')
      .should('be.visible')
      .and('contain', '© 2025 Gamer-Reco. All rights reserved.');
  });

  it('should load hero background image', () => {
    cy.get('img[alt="Gaming Background"]')
      .should('be.visible')
      .and('have.attr', 'src')
      .and('include', 'hero-bg.jpg');
  });

  it('should have proper responsive behavior', () => {
    // Test mobile view
    cy.viewport('iphone-6');
    cy.get('.flex.gap-4').should('be.visible');
    cy.get('.text-6xl').should('be.visible');
    
    // Test tablet view
    cy.viewport('ipad-2');
    cy.get('.flex.gap-4').should('be.visible');
    cy.get('.md\\:text-7xl').should('be.visible');
    
    // Test desktop view
    cy.viewport(1920, 1080);
    cy.get('.flex.gap-4').should('be.visible');
    cy.get('.md\\:text-7xl').should('be.visible');
  });
}); 
