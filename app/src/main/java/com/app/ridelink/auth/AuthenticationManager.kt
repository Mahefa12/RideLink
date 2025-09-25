package com.app.ridelink.auth


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.app.ridelink.data.model.User
import com.app.ridelink.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

@Singleton
class AuthenticationManager @Inject constructor(
    private val authRepository: AuthRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    // Production authentication mode
    private val isPrototypeMode = false

    init {
        checkAuthenticationStatus()
    }

    private fun checkAuthenticationStatus() {
        scope.launch {
            try {
                val user = authRepository.getCurrentUserSync()
                if (user != null) {
                    _currentUser.value = user
                    _authState.value = AuthState.Authenticated(user)
                } else {
                    _authState.value = AuthState.Unauthenticated
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Authentication check failed")
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        scope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = authRepository.signInWithEmail(email, password)
                if (result.isSuccess) {
                    val user = authRepository.getCurrentUserSync()
                    if (user != null) {
                        _currentUser.value = user
                        _authState.value = AuthState.Authenticated(user)
                    } else {
                        _authState.value = AuthState.Error("Failed to retrieve user data")
                    }
                } else {
                    _authState.value = AuthState.Error(
                        result.exceptionOrNull()?.message ?: "Sign in failed"
                    )
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    fun getGoogleSignInIntent() = authRepository.getGoogleSignInIntent()
    
    fun handleGoogleSignInResult(data: android.content.Intent?) {
        scope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = authRepository.handleGoogleSignInResult(data)
                if (result.isSuccess) {
                    val user = result.getOrThrow()
                    _currentUser.value = user
                    _authState.value = AuthState.Authenticated(user)
                } else {
                    _authState.value = AuthState.Error(
                        result.exceptionOrNull()?.message ?: "Google sign in failed"
                    )
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    fun registerWithEmail(email: String, password: String, displayName: String) {
        scope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = authRepository.registerWithEmail(email, password, displayName)
                if (result.isSuccess) {
                    val user = authRepository.getCurrentUserSync()
                    if (user != null) {
                        _currentUser.value = user
                        _authState.value = AuthState.Authenticated(user)
                    } else {
                        _authState.value = AuthState.Error("Failed to retrieve user data")
                    }
                } else {
                    _authState.value = AuthState.Error(
                        result.exceptionOrNull()?.message ?: "Registration failed"
                    )
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    fun signOut() {
        scope.launch {
            try {
                authRepository.signOut()
                _currentUser.value = null
                _authState.value = AuthState.Unauthenticated
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign out failed")
            }
        }
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun refreshUser() {
        checkAuthenticationStatus()
    }

    fun sendPasswordResetEmail(email: String, onResult: (Boolean, String?) -> Unit) {
        scope.launch {
            try {
                val result = authRepository.sendPasswordResetEmail(email)
                if (result.isSuccess) {
                    onResult(true, null)
                } else {
                    onResult(false, result.exceptionOrNull()?.message ?: "Failed to send reset email")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "An unexpected error occurred")
            }
        }
    }
    
    // TODO: Remove this development helper method before production release
    // This is a temporary bypass for development testing
    private fun developmentBypass(email: String = "dev@example.com", displayName: String = "Dev User") {
        // Only available in debug builds
        if (true) { // Debug mode check
            scope.launch {
                _authState.value = AuthState.Loading
                try {
                    val devUser = User(
                        id = "dev_user_${System.currentTimeMillis()}",
                        email = email,
                        displayName = displayName,
                        photoUrl = null,
                        phoneNumber = null,
                        isEmailVerified = false, 
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    _currentUser.value = devUser
                    _authState.value = AuthState.Authenticated(devUser)
                } catch (e: Exception) {
                    _authState.value = AuthState.Error("Dev bypass failed: ${e.message}")
                }
            }
        }
    }
}