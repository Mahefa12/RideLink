package com.app.ridelink.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.ridelink.auth.AuthenticationManager
import com.app.ridelink.data.model.User
import com.app.ridelink.data.model.UserPreferences
import com.app.ridelink.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Review(
    val id: String,
    val reviewerName: String,
    val rating: Int,
    val comment: String,
    val date: Long
)

data class UserDocument(
    val id: String,
    val name: String,
    val type: DocumentType,
    val url: String,
    val uploadDate: Long
)

enum class DocumentType(val displayName: String) {
    DRIVERS_LICENSE("Driver's License"),
    INSURANCE("Insurance"),
    VEHICLE_REGISTRATION("Vehicle Registration"),
    OTHER("Other")
}

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(
        val user: User,
        val rating: Double,
        val totalRides: Int,
        val recentReviews: List<Review>,
        val documents: List<UserDocument>,
        val preferences: UserPreferences,
        val isUploadingPhoto: Boolean = false,
        val isUploadingDocument: Boolean = false
    ) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val authenticationManager: AuthenticationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                val user = authRepository.getCurrentUserSync()
                if (user != null) {
                    // Load additional profile data
                    val rating = loadUserRating(user.id)
                    val totalRides = loadTotalRides(user.id)
                    val reviews = loadRecentReviews(user.id)
                    val documents = loadUserDocuments(user.id)
                    val preferences = loadUserPreferences(user.id)
                    
                    _uiState.value = ProfileUiState.Success(
                        user = user,
                        rating = rating,
                        totalRides = totalRides,
                        recentReviews = reviews,
                        documents = documents,
                        preferences = preferences
                    )
                } else {
                    _uiState.value = ProfileUiState.Error("User not found")
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(
                    e.message ?: "Failed to load profile"
                )
            }
        }
    }

    fun refreshProfile() {
        loadProfile()
    }

    fun updateField(field: String, value: String) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState is ProfileUiState.Success) {
                    val updatedUser = when (field) {
                        "displayName" -> currentState.user.copy(displayName = value)
                        "phoneNumber" -> currentState.user.copy(phoneNumber = value)
                        else -> currentState.user
                    }
                    
                    val result = authRepository.updateUserProfile(updatedUser)
                    if (result.isSuccess) {
                        _uiState.value = currentState.copy(user = updatedUser)
                        authenticationManager.refreshUser()
                    } else {
                        _uiState.value = ProfileUiState.Error(
                            result.exceptionOrNull()?.message ?: "Failed to update $field"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    fun uploadProfilePhoto(uri: Uri) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState is ProfileUiState.Success) {
                    _uiState.value = currentState.copy(isUploadingPhoto = true)
                    
                    // Simulate photo upload process
                    kotlinx.coroutines.delay(2000)
                    
                    val photoUrl = "https://example.com/photos/${System.currentTimeMillis()}.jpg"
                    val updatedUser = currentState.user.copy(photoUrl = photoUrl)
                    
                    val result = authRepository.updateUserProfile(updatedUser)
                    if (result.isSuccess) {
                        _uiState.value = currentState.copy(
                            user = updatedUser,
                            isUploadingPhoto = false
                        )
                        authenticationManager.refreshUser()
                    } else {
                        _uiState.value = ProfileUiState.Error(
                            result.exceptionOrNull()?.message ?: "Failed to upload photo"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(
                    e.message ?: "Failed to upload photo"
                )
            }
        }
    }

    fun uploadDocument(uri: Uri) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState is ProfileUiState.Success) {
                    _uiState.value = currentState.copy(isUploadingDocument = true)
                    
                    // Simulate document upload process
                    kotlinx.coroutines.delay(3000)
                    
                    val newDocument = UserDocument(
                        id = System.currentTimeMillis().toString(),
                        name = "Document_${System.currentTimeMillis()}",
                        type = DocumentType.OTHER,
                        url = "https://example.com/documents/${System.currentTimeMillis()}.pdf",
                        uploadDate = System.currentTimeMillis()
                    )
                    
                    val updatedDocuments = currentState.documents + newDocument
                    
                    _uiState.value = currentState.copy(
                        documents = updatedDocuments,
                        isUploadingDocument = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(
                    e.message ?: "Failed to upload document"
                )
            }
        }
    }

    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState is ProfileUiState.Success) {
                    // Remove document from list
                    val updatedDocuments = currentState.documents.filter { it.id != documentId }
                    
                    _uiState.value = currentState.copy(documents = updatedDocuments)
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(
                    e.message ?: "Failed to delete document"
                )
            }
        }
    }

    fun updatePreference(key: String, value: Any) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState is ProfileUiState.Success) {
                    val updatedPreferences = when (key) {
                        "notificationsEnabled" -> currentState.preferences.copy(notificationsEnabled = value as Boolean)
                        "darkModeEnabled" -> currentState.preferences.copy(darkModeEnabled = value as Boolean)
                        "locationSharingEnabled" -> currentState.preferences.copy(locationSharingEnabled = value as Boolean)
                        "autoAcceptRides" -> currentState.preferences.copy(autoAcceptRides = value as Boolean)
                        else -> currentState.preferences
                    }
                    
                    // Update preferences in state
                    _uiState.value = currentState.copy(preferences = updatedPreferences)
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(
                    e.message ?: "Failed to update preference"
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                authenticationManager.signOut()
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(
                    e.message ?: "Failed to sign out"
                )
            }
        }
    }

    // Mock data loading functions
    private suspend fun loadUserRating(userId: String): Double {
        return 4.5
    }

    private suspend fun loadTotalRides(userId: String): Int {
        return 42
    }

    private suspend fun loadRecentReviews(userId: String): List<Review> {
        return listOf(
            Review(
                id = "1",
                reviewerName = "John Doe",
                rating = 5,
                comment = "Great driver, very punctual and friendly!",
                date = System.currentTimeMillis() - 86400000
            ),
            Review(
                id = "2",
                reviewerName = "Jane Smith",
                rating = 4,
                comment = "Good ride, clean car.",
                date = System.currentTimeMillis() - 172800000
            ),
            Review(
                id = "3",
                reviewerName = "Mike Johnson",
                rating = 5,
                comment = "Excellent service, highly recommended!",
                date = System.currentTimeMillis() - 259200000
            )
        )
    }

    private suspend fun loadUserDocuments(userId: String): List<UserDocument> {
        return listOf(
            UserDocument(
                id = "doc1",
                name = "Driver's License",
                type = DocumentType.DRIVERS_LICENSE,
                url = "https://example.com/documents/license.pdf",
                uploadDate = System.currentTimeMillis() - 2592000000
            ),
            UserDocument(
                id = "doc2",
                name = "Insurance Certificate",
                type = DocumentType.INSURANCE,
                url = "https://example.com/documents/insurance.pdf",
                uploadDate = System.currentTimeMillis() - 1296000000
            )
        )
    }

    private suspend fun loadUserPreferences(userId: String): UserPreferences {
        return UserPreferences(
            notificationsEnabled = true,
            darkModeEnabled = false,
            locationSharingEnabled = true,
            autoAcceptRides = false
        )
    }
}