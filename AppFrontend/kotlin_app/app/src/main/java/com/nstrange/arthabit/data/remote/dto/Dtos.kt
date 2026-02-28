package com.nstrange.arthabit.data.remote.dto

import com.google.gson.annotations.SerializedName

// ── Auth DTOs ──

data class LoginRequest(
    val username: String,
    val password: String
)

data class SignupRequest(
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    val email: String,
    @SerializedName("phone_number") val phoneNumber: Long,
    val password: String,
    val username: String
)

data class RefreshTokenRequest(
    val token: String
)

data class AuthResponse(
    val accessToken: String,
    val token: String,
    val userId: String? = null
)

// ── User DTOs ──

data class UserDto(
    @SerializedName("user_id") val userId: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    @SerializedName("phone_number") val phoneNumber: Long,
    val email: String,
    @SerializedName("profile_pic") val profilePic: String?
)

// ── Expense DTOs ──

data class ExpenseDto(
    val amount: Double,
    val merchant: String,
    val currency: String,
    @SerializedName("created_at") val createdAt: String
)

data class AddExpenseRequest(
    val amount: Double,
    val merchant: String,
    val currency: String
)

