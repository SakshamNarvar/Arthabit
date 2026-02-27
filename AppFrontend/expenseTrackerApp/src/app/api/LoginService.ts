import AsyncStorage from "@react-native-async-storage/async-storage";
import API_CONFIG from "../config/apiConfig";

class LoginService {

    constructor() {}

    async isLoggedIn(){
        try {
            const SERVER_BASE_URL = API_CONFIG.AUTH_SERVICE_URL;
            console.log('Inside login');
            const accessToken = await AsyncStorage.getItem('accessToken');
            if (!accessToken) {
                return false;
            }
            console.log('Token is ' + accessToken);
            const response = await fetch(`${SERVER_BASE_URL}/auth/v1/ping`, {
              method: 'GET',
              headers: {
                Accept: 'application/json',
                'Content-Type': 'application/json',
                Authorization: 'Bearer ' + accessToken,
                'X-Requested-With': 'XMLHttpRequest',
              },
            });
            if (!response.ok) {
                return false;
            }
            const responseBody = await response.text();
            console.log("Response body in isLoggedIn(): ", responseBody);
            // Backend returns "Ping Successful for user: <uuid>"
            const uuidMatch = responseBody.match(/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i);
            return uuidMatch !== null;
        } catch (error) {
            console.error('Error checking login status:', error);
            return false;
        }
      };
}

export default LoginService;