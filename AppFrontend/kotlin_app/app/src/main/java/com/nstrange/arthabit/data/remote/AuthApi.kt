package com.nstrange.arthabit.data.remote

import com.nstrange.arthabit.data.remote.dto.AuthResponse
import com.nstrange.arthabit.data.remote.dto.LoginRequest
import com.nstrange.arthabit.data.remote.dto.RefreshTokenRequest
import com.nstrange.arthabit.data.remote.dto.SignupRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {

    @GET("/auth/v1/ping")
    suspend fun ping(
        @Header("Authorization") authHeader: String
    ): Response<ResponseBody>

    @POST("/auth/v1/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @POST("/auth/v1/signup")
    suspend fun signup(
        @Body request: SignupRequest
    ): Response<AuthResponse>

    @POST("/auth/v1/refreshToken")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<AuthResponse>
}

