package com.nstrange.arthabit.domain.repository

import com.nstrange.arthabit.domain.model.Expense
import com.nstrange.arthabit.util.Resource

interface ExpenseRepository {
    suspend fun getExpenses(): Resource<List<Expense>>
    suspend fun addExpense(amount: Double, merchant: String, currency: String): Resource<Unit>
}

