package com.nstrange.arthabit.data.remote

import com.nstrange.arthabit.data.remote.dto.AddExpenseRequest
import com.nstrange.arthabit.data.remote.dto.ExpenseDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ExpenseApi {

    @GET("/expense/v1/getExpense")
    suspend fun getExpenses(
        @Header("Authorization") authHeader: String,
        @Header("X-User-Id") userId: String
    ): Response<List<ExpenseDto>>

    @POST("/expense/v1/addExpense")
    suspend fun addExpense(
        @Header("Authorization") authHeader: String,
        @Header("X-User-Id") userId: String,
        @Body request: AddExpenseRequest
    ): Response<ResponseBody>
}

