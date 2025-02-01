export const environment = {
  production: false,
  testing: true,
  apiUrl: 'http://localhost:4200/api',
  backendUrl: 'http://localhost:8080',
  oauth: {
    google: {
      clientId: 'test-client-id',  // Mock client ID for testing
      redirectUri: 'http://localhost:4200/auth/google/callback'
    }
  }
}; 
