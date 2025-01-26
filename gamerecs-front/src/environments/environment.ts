export const environment = {
  production: false,
  testing: false,
  apiUrl: 'http://localhost:4200/api',
  oauth: {
    google: {
      clientId: '${GOOGLE_OAUTH_CLIENT_ID}',
      redirectUri: 'http://localhost:4200/auth/google/callback'
    }
  }
}; 
