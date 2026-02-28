package com.nstrange.arthabit.util

/**
 * A generic wrapper for API/data responses used throughout the app.
 */
sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()
}

