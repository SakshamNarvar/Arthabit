// Centralized API configuration — keep service URLs in one place.
// When running via docker-compose, the React Native app hits localhost
// which maps to the host-published ports from services.yml.

const API_CONFIG = {
  AUTH_SERVICE_URL: 'http://localhost:9898',
  USER_SERVICE_URL: 'http://localhost:9810',
  EXPENSE_SERVICE_URL: 'http://localhost:9820',
  DS_SERVICE_URL: 'http://localhost:8010',
};

export default API_CONFIG;
