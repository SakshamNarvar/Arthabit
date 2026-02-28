package com.nstrange.arthabit.domain.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class Expense(
    val key: Int,
    val amount: Double,
    val merchant: String,
    val currency: String,
    val createdAt: LocalDateTime
) {
    val formattedDate: String
        get() = createdAt.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))

    val formattedAmount: String
        get() = "$currency %.2f".format(amount)
}

