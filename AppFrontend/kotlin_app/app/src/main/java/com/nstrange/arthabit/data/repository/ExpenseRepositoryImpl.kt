package com.nstrange.arthabit.data.repository

import com.nstrange.arthabit.data.local.TokenManager
import com.nstrange.arthabit.data.remote.ExpenseApi
import com.nstrange.arthabit.data.remote.dto.AddExpenseRequest
import com.nstrange.arthabit.domain.model.Expense
import com.nstrange.arthabit.domain.repository.ExpenseRepository
import com.nstrange.arthabit.util.Resource
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val expenseApi: ExpenseApi,
    private val tokenManager: TokenManager
) : ExpenseRepository {

    override suspend fun getExpenses(): Resource<List<Expense>> {
        return try {
            val token = tokenManager.getAccessToken()
                ?: return Resource.Error("No access token")
            val userId = tokenManager.getUserId()
                ?: return Resource.Error("No user ID")

            val response = expenseApi.getExpenses("Bearer $token", userId)
            if (response.isSuccessful) {
                val dtos = response.body() ?: emptyList()
                val expenses = dtos.mapIndexed { index, dto ->
                    Expense(
                        key = index,
                        amount = dto.amount,
                        merchant = dto.merchant,
                        currency = dto.currency,
                        createdAt = parseDate(dto.createdAt)
                    )
                }
                Resource.Success(expenses)
            } else {
                Resource.Error("Failed to fetch expenses: ${response.code()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun addExpense(
        amount: Double,
        merchant: String,
        currency: String
    ): Resource<Unit> {
        return try {
            val token = tokenManager.getAccessToken()
                ?: return Resource.Error("No access token")
            val userId = tokenManager.getUserId()
                ?: return Resource.Error("No user ID")

            val response = expenseApi.addExpense(
                authHeader = "Bearer $token",
                userId = userId,
                request = AddExpenseRequest(amount, merchant, currency)
            )
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to add expense: ${response.code()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
    }

    private fun parseDate(dateString: String): LocalDateTime {
        return try {
            LocalDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME)
        } catch (e: Exception) {
            LocalDateTime.now()
        }
    }
}

