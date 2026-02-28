package com.nstrange.arthabit.data.repository

import com.nstrange.arthabit.data.local.TokenManager
import com.nstrange.arthabit.data.remote.AuthApi
import com.nstrange.arthabit.data.remote.dto.LoginRequest
import com.nstrange.arthabit.data.remote.dto.RefreshTokenRequest
import com.nstrange.arthabit.data.remote.dto.SignupRequest
import com.nstrange.arthabit.domain.model.AuthTokens
import com.nstrange.arthabit.domain.repository.AuthRepository
import com.nstrange.arthabit.util.Resource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun ping(): Resource<String> {
        return try {
            val token = tokenManager.getAccessToken() ?: return Resource.Error("No access token")
            val response = authApi.ping("Bearer $token")
            if (response.isSuccessful) {
                val body = response.body()?.string() ?: ""
                val uuidRegex = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
                val match = uuidRegex.find(body)
                if (match != null) {
                    Resource.Success(match.value)
                } else {
                    Resource.Error("Invalid ping response")
                }
            } else {
                Resource.Error("Ping failed: ${response.code()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun login(username: String, password: String): Resource<AuthTokens> {
        return try {
            val response = authApi.login(LoginRequest(username, password))
            if (response.isSuccessful) {
                val body = response.body() ?: return Resource.Error("Empty response")
                val tokens = AuthTokens(
                    accessToken = body.accessToken,
                    refreshToken = body.token,
                    userId = body.userId
                )
                tokenManager.saveTokens(tokens.accessToken, tokens.refreshToken, tokens.userId)
                Resource.Success(tokens)
            } else {
                val errorBody = response.errorBody()?.string()
                Resource.Error(errorBody ?: "Login failed")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun signup(
        firstName: String,
        lastName: String,
        email: String,
        phoneNumber: Long,
        password: String,
        username: String
    ): Resource<AuthTokens> {
        return try {
            val response = authApi.signup(
                SignupRequest(
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    phoneNumber = phoneNumber,
                    password = password,
                    username = username
                )
            )
            if (response.isSuccessful) {
                val body = response.body() ?: return Resource.Error("Empty response")
                val tokens = AuthTokens(
                    accessToken = body.accessToken,
                    refreshToken = body.token,
                    userId = body.userId
                )
                tokenManager.saveTokens(tokens.accessToken, tokens.refreshToken, tokens.userId)
                Resource.Success(tokens)
            } else {
                val errorBody = response.errorBody()?.string()
                Resource.Error(errorBody ?: "Signup failed")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun refreshToken(): Resource<AuthTokens> {
        return try {
            val refreshToken = tokenManager.getRefreshToken()
                ?: return Resource.Error("No refresh token")
            val response = authApi.refreshToken(RefreshTokenRequest(refreshToken))
            if (response.isSuccessful) {
                val body = response.body() ?: return Resource.Error("Empty response")
                val tokens = AuthTokens(
                    accessToken = body.accessToken,
                    refreshToken = body.token,
                    userId = body.userId
                )
                tokenManager.saveTokens(tokens.accessToken, tokens.refreshToken, tokens.userId)
                Resource.Success(tokens)
            } else {
                Resource.Error("Token refresh failed: ${response.code()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun logout() {
        tokenManager.clearTokens()
    }
}

