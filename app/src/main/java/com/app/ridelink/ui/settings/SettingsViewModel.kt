package com.app.ridelink.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.ridelink.auth.AuthenticationManager
import com.app.ridelink.data.model.User
import com.app.ridelink.data.repository.AuthRepository
import com.app.ridelink.ui.theme.ThemeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SettingsUiState {
    object Loading : SettingsUiState()
    data class Success(val user: User) : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val authenticationManager: AuthenticationManager,
    private val themeManager: ThemeManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadUserData()
    }

    fun loadUserData() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            try {
                // Call the suspend version that returns User?
                val user = authRepository.getCurrentUserSync()
                if (user != null) {
                    _uiState.value = SettingsUiState.Success(user)
                } else {
                    _uiState.value = SettingsUiState.Error("User not found")
                }
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(
                    e.message ?: "Failed to load user data"
                )
            }
        }
    }

    fun updateDisplayName(newDisplayName: String) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState is SettingsUiState.Success) {
                    val updatedUser = currentState.user.copy(displayName = newDisplayName)
                    val result = authRepository.updateUserProfile(updatedUser)
                    if (result.isSuccess) {
                        _uiState.value = SettingsUiState.Success(updatedUser)
                        authenticationManager.refreshUser()
                    } else {
                        _uiState.value = SettingsUiState.Error(
                            result.exceptionOrNull()?.message ?: "Failed to update display name"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    fun updatePhoneNumber(newPhoneNumber: String) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState is SettingsUiState.Success) {
                    val updatedUser = currentState.user.copy(phoneNumber = newPhoneNumber)
                    val result = authRepository.updateUserProfile(updatedUser)
                    if (result.isSuccess) {
                        _uiState.value = SettingsUiState.Success(updatedUser)
                        authenticationManager.refreshUser()
                    } else {
                        _uiState.value = SettingsUiState.Error(
                            result.exceptionOrNull()?.message ?: "Failed to update phone number"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    fun updateEmail(newEmail: String) {
        viewModelScope.launch {
            try {
                val result = authRepository.updateEmail(newEmail)
                if (result.isSuccess) {
                    loadUserData()
                    authenticationManager.refreshUser()
                } else {
                    _uiState.value = SettingsUiState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to update email"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            try {
                val result = authRepository.changePassword(currentPassword, newPassword)
                if (result.isFailure) {
                    _uiState.value = SettingsUiState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to change password"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                authenticationManager.signOut()
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(
                    e.message ?: "Failed to sign out"
                )
            }
        }
    }

    fun clearError() {
        if (_uiState.value is SettingsUiState.Error) {
            loadUserData()
        }
    }

    // Theme management functions
    val isDarkTheme = themeManager.isDarkTheme
    val isSystemTheme = themeManager.isSystemTheme

    fun setDarkTheme(isDark: Boolean) {
        viewModelScope.launch {
            themeManager.setDarkTheme(isDark)
        }
    }

    fun setSystemTheme(useSystem: Boolean) {
        viewModelScope.launch {
            themeManager.setSystemTheme(useSystem)
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            themeManager.toggleTheme()
        }
    }
}