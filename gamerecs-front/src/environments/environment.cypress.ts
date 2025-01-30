export const environment = {
  production: false,
  testing: true,  // This ensures password feedback is disabled during Cypress tests
  apiUrl: 'http://localhost:4200/api',
  oauth: {
    google: {
      clientId: 'cypress-client-id',  // Mock client ID for Cypress tests
      redirectUri: 'http://localhost:4200/auth/google/callback'
    }
  }
}; 
