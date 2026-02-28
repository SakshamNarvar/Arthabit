package com.nstrange.arthabit.domain.model

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val userId: String? = null
)

