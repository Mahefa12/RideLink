package com.app.ridelink.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.ridelink.auth.AuthenticationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val message: String) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
    data class PasswordResetSent(val message: String) : LoginUiState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authenticationManager: AuthenticationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                authenticationManager.signInWithEmail(email, password)
                _uiState.value = LoginUiState.Success("Login successful!")
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(
                    e.message ?: "Login failed"
                )
            }
        }
    }

    fun getGoogleSignInIntent() = authenticationManager.getGoogleSignInIntent()
    
    fun handleGoogleSignInResult(data: android.content.Intent?) {
        _uiState.value = LoginUiState.Loading
        authenticationManager.handleGoogleSignInResult(data)
        // The UI state will be updated through AuthenticationManager's state flow
        _uiState.value = LoginUiState.Success("Google sign-in initiated")
    }

    fun clearError() {
        if (_uiState.value is LoginUiState.Error) {
            _uiState.value = LoginUiState.Idle
        }
    }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            authenticationManager.sendPasswordResetEmail(email) { success, errorMessage ->
                if (success) {
                    _uiState.value = LoginUiState.PasswordResetSent("Password reset email sent to $email")
                } else {
                    _uiState.value = LoginUiState.Error(errorMessage ?: "Failed to send reset email")
                }
            }
        }
    }
    
    // Prototype bypass method
    fun bypassLogin(email: String = "prototype@ridelink.com", displayName: String = "Prototype User") {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                authenticationManager.bypassLogin(email, displayName)
                _uiState.value = LoginUiState.Success("Bypass login successful!")
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(
                    e.message ?: "Bypass login failed"
                )
            }
        }
    }
}
