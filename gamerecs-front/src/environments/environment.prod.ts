export const environment = {
  production: true,
  testing: false,
  apiUrl: 'https://gamer-reco.com/api',
  oauth: {
    google: {
      clientId: '${GOOGLE_OAUTH_CLIENT_ID}',
      redirectUri: 'https://gamer-reco.com/auth/google/callback'
    }
  }
}; 
