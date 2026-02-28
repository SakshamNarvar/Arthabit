package com.nstrange.arthabit.domain.repository

import com.nstrange.arthabit.domain.model.AuthTokens
import com.nstrange.arthabit.util.Resource

interface AuthRepository {
    suspend fun ping(): Resource<String>
    suspend fun login(username: String, password: String): Resource<AuthTokens>
    suspend fun signup(
        firstName: String,
        lastName: String,
        email: String,
        phoneNumber: Long,
        password: String,
        username: String
    ): Resource<AuthTokens>
    suspend fun refreshToken(): Resource<AuthTokens>
    suspend fun logout()
}

