package com.app.ridelink.ui.messaging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.ridelink.data.model.ConversationWithParticipant
import com.app.ridelink.data.repository.MessagingRepository
import com.app.ridelink.auth.AuthenticationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationListUiState(
    val conversations: List<ConversationWithParticipant> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalUnreadCount: Int = 0,
    val currentUserId: String? = null
)

@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val messagingRepository: MessagingRepository,
    private val authenticationManager: AuthenticationManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ConversationListUiState())
    val uiState: StateFlow<ConversationListUiState> = _uiState.asStateFlow()
    
    init {
        // Get current user and load conversations
        viewModelScope.launch {
            authenticationManager.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(currentUserId = user?.id)
                user?.id?.let { userId ->
                    loadConversations(userId)
                    loadUnreadCount(userId)
                }
            }
        }
    }
    
    /**
     * Load conversations for the current user
     */
    private fun loadConversations(userId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        viewModelScope.launch {
            try {
                val conversations = messagingRepository.getConversationsForUser(userId)
                _uiState.value = _uiState.value.copy(
                    conversations = conversations,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to load conversations",
                    isLoading = false
                )
            }
        }
    }
    
    /**
     * Load total unread message count
     */
    private fun loadUnreadCount(userId: String) {
        viewModelScope.launch {
            try {
                val count = messagingRepository.getUnreadMessageCount(userId)
                _uiState.value = _uiState.value.copy(totalUnreadCount = count)
            } catch (e: Exception) {
                // Silently handle error for unread count
            }
        }
    }
    
    /**
     * Refresh conversations
     */
    fun refreshConversations() {
        val userId = _uiState.value.currentUserId ?: return
        loadConversations(userId)
        loadUnreadCount(userId)
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Format conversation timestamp
     */
    fun formatConversationTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m"
            diff < 86400_000 -> "${diff / 3600_000}h"
            diff < 604800_000 -> "${diff / 86400_000}d"
            else -> {
                val date = java.util.Date(timestamp)
                java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(date)
            }
        }
    }
    
    /**
     * Get conversation preview text
     */
    fun getConversationPreview(conversation: ConversationWithParticipant): String {
        return when {
            conversation.lastMessage.isNullOrBlank() -> "No messages yet"
            conversation.lastMessage.length > 50 -> 
                "${conversation.lastMessage.take(50)}..."
            else -> conversation.lastMessage
        }
    }
    
    /**
     * Check if conversation has unread messages
     */
    fun hasUnreadMessages(conversation: ConversationWithParticipant): Boolean {
        return conversation.unreadCount > 0
    }
    
    /**
     * Get unread message count for conversation
     */
    fun getUnreadCount(conversation: ConversationWithParticipant): Int {
        return conversation.unreadCount
    }
}