package com.nstrange.arthabit.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nstrange.arthabit.domain.repository.AuthRepository
import com.nstrange.arthabit.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = true,
    val isAutoLoginChecking: Boolean = true,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        attemptAutoLogin()
    }

    private fun attemptAutoLogin() {
        viewModelScope.launch {
            // Step 1: Try ping with existing access token
            val pingResult = authRepository.ping()
            if (pingResult is Resource.Success) {
                _uiState.update {
                    it.copy(isLoading = false, isAutoLoginChecking = false, isLoggedIn = true)
                }
                return@launch
            }

            // Step 2: Try token refresh
            val refreshResult = authRepository.refreshToken()
            if (refreshResult is Resource.Success) {
                _uiState.update {
                    it.copy(isLoading = false, isAutoLoginChecking = false, isLoggedIn = true)
                }
                return@launch
            }

            // Show login form
            _uiState.update {
                it.copy(isLoading = false, isAutoLoginChecking = false)
            }
        }
    }

    fun onUsernameChange(username: String) {
        _uiState.update { it.copy(username = username, errorMessage = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun login() {
        val state = _uiState.value
        if (state.username.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Username cannot be empty") }
            return
        }
        if (state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Password cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = authRepository.login(state.username.trim(), state.password.trim())

            when (result) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
                is Resource.Loading -> { /* no-op */ }
            }
        }
    }
}
