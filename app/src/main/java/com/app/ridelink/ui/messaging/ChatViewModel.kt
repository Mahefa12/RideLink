package com.app.ridelink.ui.messaging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.ridelink.data.model.*
import com.app.ridelink.data.repository.MessagingRepository
import com.app.ridelink.data.repository.MessagingResult
import com.app.ridelink.auth.AuthenticationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

data class ChatUiState(
    val messages: List<MessageWithSender> = emptyList(),
    val conversationDetails: ConversationDetails? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSendingMessage: Boolean = false,
    val currentUserId: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messagingRepository: MessagingRepository,
    private val authenticationManager: AuthenticationManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    private var currentConversationId: String? = null
    
    init {
        // Get current user ID
        viewModelScope.launch {
            authenticationManager.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(currentUserId = user?.id)
            }
        }
    }
    
    /**
     * Load conversation and messages
     */
    fun loadConversation(conversationId: String) {
        Log.d("RideLink", "ChatViewModel: Loading conversation with ID: $conversationId")
        currentConversationId = conversationId
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        viewModelScope.launch {
            try {
                val currentUserId = _uiState.value.currentUserId
                Log.d("RideLink", "ChatViewModel: Current user ID: $currentUserId")
                if (currentUserId != null) {
                    // Launch both flows concurrently
                    launch {
                        // Load conversation details
                        messagingRepository.getConversationDetails(conversationId, currentUserId)
                            .collect { details ->
                                Log.d("RideLink", "ChatViewModel: Loaded conversation details: $details")
                                _uiState.value = _uiState.value.copy(
                                    conversationDetails = details,
                                    isLoading = false
                                )
                            }
                    }
                    
                    launch {
                        // Load messages
                        messagingRepository.getMessagesForConversation(conversationId)
                            .collect { messages ->
                                Log.d("RideLink", "ChatViewModel: Loaded ${messages.size} messages for conversation $conversationId")
                                _uiState.value = _uiState.value.copy(
                                    messages = messages,
                                    isLoading = false
                                )
                            }
                    }
                    
                    // Mark messages as read
                    markMessagesAsRead()
                } else {
                    Log.e("RideLink", "ChatViewModel: User not authenticated")
                    _uiState.value = _uiState.value.copy(
                        error = "User not authenticated",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("RideLink", "ChatViewModel: Error loading conversation: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to load conversation",
                    isLoading = false
                )
            }
        }
    }
    
    /**
     * Send a text message
     */
    fun sendMessage(content: String) {
        val conversationId = currentConversationId ?: return
        val currentUserId = _uiState.value.currentUserId ?: return
        val conversationDetails = _uiState.value.conversationDetails ?: return
        
        if (content.isBlank()) return
        
        _uiState.value = _uiState.value.copy(isSendingMessage = true)
        
        viewModelScope.launch {
            val result = messagingRepository.sendMessage(
                conversationId = conversationId,
                senderId = currentUserId,
                receiverId = conversationDetails.participant.id,
                content = content.trim(),
                messageType = MessageType.TEXT
            )
            
            when (result) {
                is MessagingResult.Success<*> -> {
                    _uiState.value = _uiState.value.copy(
                        isSendingMessage = false,
                        error = null
                    )
                }
                is MessagingResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSendingMessage = false,
                        error = result.message
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isSendingMessage = false)
                }
            }
        }
    }
    
    /**
     * Send a location message
     */
    fun sendLocationMessage(latitude: Double, longitude: Double) {
        val conversationId = currentConversationId ?: return
        val currentUserId = _uiState.value.currentUserId ?: return
        val conversationDetails = _uiState.value.conversationDetails ?: return
        
        _uiState.value = _uiState.value.copy(isSendingMessage = true)
        
        viewModelScope.launch {
            val locationContent = "Location: $latitude, $longitude"
            val result = messagingRepository.sendMessage(
                conversationId = conversationId,
                senderId = currentUserId,
                receiverId = conversationDetails.participant.id,
                content = locationContent,
                messageType = MessageType.LOCATION
            )
            
            when (result) {
                is MessagingResult.Success<*> -> {
                    _uiState.value = _uiState.value.copy(
                        isSendingMessage = false,
                        error = null
                    )
                }
                is MessagingResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSendingMessage = false,
                        error = result.message
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isSendingMessage = false)
                }
            }
        }
    }
    
    /**
     * Mark messages as read
     */
    private fun markMessagesAsRead() {
        val conversationId = currentConversationId ?: return
        val currentUserId = _uiState.value.currentUserId ?: return
        
        viewModelScope.launch {
            messagingRepository.markMessagesAsRead(conversationId, currentUserId)
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Create conversation with another user
     */
    fun createConversationWithUser(otherUserId: String, rideId: String? = null) {
        val currentUserId = _uiState.value.currentUserId ?: return
        
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        viewModelScope.launch {
            val result = messagingRepository.createOrGetConversation(
                user1Id = currentUserId,
                user2Id = otherUserId,
                rideId = rideId
            )
            
            when (result) {
                is MessagingResult.Success<*> -> {
                    val conversation = result.data as Conversation
                    loadConversation(conversation.id)
                }
                is MessagingResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message,
                        isLoading = false
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }
    
    /**
     * Check if message is from current user
     */
    fun isMessageFromCurrentUser(message: MessageWithSender): Boolean {
        return message.senderId == _uiState.value.currentUserId
    }
    
    /**
     * Format message timestamp
     */
    fun formatMessageTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            else -> {
                val date = java.util.Date(timestamp)
                java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(date)
            }
        }
    }
}