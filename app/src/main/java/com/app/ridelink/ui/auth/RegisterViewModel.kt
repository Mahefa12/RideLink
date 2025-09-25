package com.app.ridelink.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.ridelink.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RegisterUiState {
    object Idle : RegisterUiState()
    object Loading : RegisterUiState()
    data class Success(val message: String) : RegisterUiState()
    data class Error(val message: String) : RegisterUiState()
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun registerWithEmail(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            try {
                val result = authRepository.registerWithEmail(email, password, displayName)
                if (result.isSuccess) {
                    _uiState.value = RegisterUiState.Success("Registration successful!")
                } else {
                    _uiState.value = RegisterUiState.Error(
                        result.exceptionOrNull()?.message ?: "Registration failed"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = RegisterUiState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    fun getGoogleSignInIntent() = authRepository.getGoogleSignInIntent()
    
    fun handleGoogleSignInResult(data: android.content.Intent?) {
        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            try {
                val result = authRepository.handleGoogleSignInResult(data)
                if (result.isSuccess) {
                    _uiState.value = RegisterUiState.Success("Google sign-in successful!")
                } else {
                    _uiState.value = RegisterUiState.Error(
                        result.exceptionOrNull()?.message ?: "Google sign-in failed"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = RegisterUiState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    fun clearError() {
        if (_uiState.value is RegisterUiState.Error) {
            _uiState.value = RegisterUiState.Idle
        }
    }
}