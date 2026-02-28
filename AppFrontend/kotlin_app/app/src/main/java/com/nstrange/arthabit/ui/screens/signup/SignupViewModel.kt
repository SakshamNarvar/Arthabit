package com.nstrange.arthabit.ui.screens.signup

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

data class SignupUiState(
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val phoneNumber: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSignedUp: Boolean = false
)

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

    fun onFirstNameChange(value: String) {
        _uiState.update { it.copy(firstName = value, errorMessage = null) }
    }

    fun onLastNameChange(value: String) {
        _uiState.update { it.copy(lastName = value, errorMessage = null) }
    }

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(username = value, errorMessage = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onPhoneNumberChange(value: String) {
        _uiState.update { it.copy(phoneNumber = value, errorMessage = null) }
    }

    fun signup() {
        val state = _uiState.value

        if (state.firstName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "First name is required") }
            return
        }
        if (state.lastName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Last name is required") }
            return
        }
        if (state.username.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Username is required") }
            return
        }
        if (state.email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email is required") }
            return
        }
        if (state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Password is required") }
            return
        }
        if (state.phoneNumber.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Phone number is required") }
            return
        }

        val phone = state.phoneNumber.toLongOrNull()
        if (phone == null) {
            _uiState.update { it.copy(errorMessage = "Invalid phone number") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = authRepository.signup(
                firstName = state.firstName.trim(),
                lastName = state.lastName.trim(),
                email = state.email.trim(),
                phoneNumber = phone,
                password = state.password.trim(),
                username = state.username.trim()
            )

            when (result) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, isSignedUp = true) }
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
