export const environment = {
  production: true,
  testing: false,
  apiUrl: 'https://api.gamer-reco.com/api',
  backendUrl: '${BACKEND_URL}',
  oauth: {
    google: {
      clientId: '${GOOGLE_OAUTH_CLIENT_ID}',
      redirectUri: 'https://gamer-reco.com/auth/google/callback'
    }
  }
}; 
