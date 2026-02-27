import {View, Text, StyleSheet, TouchableOpacity, Alert} from 'react-native';
import React, {useEffect, useRef, useState} from 'react';
import CustomText from '../components/CustomText';
import CustomBox from '../components/CustomBox';
import {GestureHandlerRootView, TextInput} from 'react-native-gesture-handler';
import AsyncStorage from '@react-native-async-storage/async-storage';
import LoginService from '../api/LoginService';
import API_CONFIG from '../config/apiConfig';
import type {NativeStackScreenProps} from '@react-navigation/native-stack';
import type {RootStackParamList} from '../navigation/AppNavigator';

type LoginProps = NativeStackScreenProps<RootStackParamList, 'Login'>;

const Login: React.FC<LoginProps> = ({navigation}) => {
  const [userName, setUserName] = useState('');
  const [password, setPassword] = useState('');
  const [loggedIn, setLoggedIn] = useState(true);
  const loginService = new LoginService();
  const isMounted = useRef(true);
  const abortControllerRef = useRef<AbortController | null>(null);

  const tryRefreshToken = async () => {
    try {
      const SERVER_BASE_URL = API_CONFIG.AUTH_SERVICE_URL;
      console.log('Inside Refresh token');
      const storedRefreshToken = await AsyncStorage.getItem('refreshToken');
      if (!storedRefreshToken) {
        return false;
      }
      const response = await fetch(`${SERVER_BASE_URL}/auth/v1/refreshToken`, {
        method: 'POST',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
          'X-Requested-With': 'XMLHttpRequest',
        },
        body: JSON.stringify({
          token: storedRefreshToken,
        }),
      });

      if (response.ok) {
        const data = await response.json();
        await AsyncStorage.setItem('accessToken', data['accessToken']);
        await AsyncStorage.setItem('refreshToken', data['token']);
        const newRefreshToken = await AsyncStorage.getItem('refreshToken');
        const newAccessToken = await AsyncStorage.getItem('accessToken');
        console.log(
          'Tokens after refresh are ' + newRefreshToken + ' ' + newAccessToken,
        );
      }

      return response.ok;
    } catch (error) {
      console.error('Error refreshing token:', error);
      return false;
    }
  };

  const gotoHomePageWithLogin = async () => {
    if (!userName.trim() || !password.trim()) {
      Alert.alert('Login Failed', 'Please enter both username and password.');
      return;
    }

    // Cancel any previous in-flight login request
    abortControllerRef.current?.abort();
    const controller = new AbortController();
    abortControllerRef.current = controller;

    try {
      const SERVER_BASE_URL = API_CONFIG.AUTH_SERVICE_URL;
      const response = await fetch(`${SERVER_BASE_URL}/auth/v1/login`, {
        method: 'POST',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
          'X-Requested-With': 'XMLHttpRequest',
        },
        body: JSON.stringify({
          username: userName,
          password: password,
        }),
        signal: controller.signal,
      });

      if (!isMounted.current) return;

      if (response.ok) {
        const data = await response.json();
        await AsyncStorage.setItem('refreshToken', data['token']);
        await AsyncStorage.setItem('accessToken', data['accessToken']);
        if (data['userId']) {
          await AsyncStorage.setItem('userId', data['userId']);
        }
        navigation.reset({index: 0, routes: [{name: 'Home'}]});
      } else {
        let errorMsg = 'Login failed. Please try again.';
        try {
          const errorData = await response.json();
          if (errorData.message) {
            errorMsg = errorData.message;
          }
        } catch (_) {
          // response wasn't JSON
        }
        console.warn('Login failed:', response.status, errorMsg);
        Alert.alert('Login Failed', errorMsg);
      }
    } catch (error: any) {
      if (error?.name === 'AbortError') return;
      if (!isMounted.current) return;
      console.warn('Error during login:', error);
    }
  };

  const gotoSignup = () => {
    navigation.navigate('SignUp');
  };

  useEffect(() => {
    isMounted.current = true;

    const handleLogin = async () => {
      try {
        const isAlreadyLoggedIn = await loginService.isLoggedIn();
        if (!isMounted.current) return;
        setLoggedIn(isAlreadyLoggedIn);
        if (isAlreadyLoggedIn) {
          navigation.reset({index: 0, routes: [{name: 'Home'}]});
        } else {
          const refreshed = await tryRefreshToken();
          if (!isMounted.current) return;
          setLoggedIn(refreshed);
          if (refreshed) {
            navigation.reset({index: 0, routes: [{name: 'Home'}]});
          }
        }
      } catch (error) {
        console.warn('Error during auto-login:', error);
        if (isMounted.current) setLoggedIn(false);
      }
    };
    handleLogin();

    return () => {
      isMounted.current = false;
      abortControllerRef.current?.abort();
    };
  }, []);

  return (
    <GestureHandlerRootView style={{flex: 1}}>
      <View style={styles.loginContainer}>
        <CustomBox style={loginBox}>
          <CustomText style={styles.heading}>Login</CustomText>
          <TextInput
            placeholder="Username"
            value={userName}
            onChangeText={text => setUserName(text)}
            style={styles.textInput}
            placeholderTextColor="#888"
            autoCapitalize="none"
          />
          <TextInput
            placeholder="Password"
            value={password}
            onChangeText={text => setPassword(text)}
            style={styles.textInput}
            placeholderTextColor="#888"
            secureTextEntry
            autoCapitalize="none"
          />
        </CustomBox>
        <TouchableOpacity onPress={() => gotoHomePageWithLogin()} style={styles.button}>
          <CustomBox style={buttonBox}>
            <CustomText style={{textAlign: 'center'}}>Submit</CustomText>
          </CustomBox>
        </TouchableOpacity>
        <TouchableOpacity onPress={() => gotoSignup()} style={styles.button}>
          <CustomBox style={buttonBox}>
            <CustomText style={{textAlign: 'center'}}>Signup</CustomText>
          </CustomBox>
        </TouchableOpacity>
      </View>
    </GestureHandlerRootView>
  );
};

export default Login;

const styles = StyleSheet.create({
  loginContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  button: {
    marginTop: 20,
    width: '30%',
  },
  heading: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 20,
  },
  textInput: {
    backgroundColor: '#f0f0f0',
    borderRadius: 5,
    padding: 10,
    marginBottom: 10,
    width: '100%',
    color: 'black',
  },
});

const loginBox = {
  mainBox: {
    backgroundColor: '#fff',
    borderColor: 'black',
    borderWidth: 1,
    borderRadius: 10,
    padding: 20,
  },
  shadowBox: {
    backgroundColor: 'gray',
    borderRadius: 10,
  },
};

const buttonBox = {
  mainBox: {
    backgroundColor: '#fff',
    borderColor: 'black',
    borderWidth: 1,
    borderRadius: 10,
    padding: 10,
  },
  shadowBox: {
    backgroundColor: 'gray',
    borderRadius: 10,
  },
};