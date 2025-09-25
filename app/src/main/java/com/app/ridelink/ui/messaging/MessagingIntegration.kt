package com.app.ridelink.ui.messaging

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.ridelink.data.repository.MessagingRepository
import com.app.ridelink.data.repository.MessagingResult
import com.app.ridelink.auth.AuthenticationManager
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for integrating messaging with ride booking flow
 */
@Singleton
class MessagingIntegration @Inject constructor(
    private val messagingRepository: MessagingRepository,
    private val authenticationManager: AuthenticationManager
) {
    
    /**
     * Create or get conversation between current user and ride owner
     * Returns the conversation ID if successful
     */
    suspend fun createRideConversation(
        rideOwnerId: String,
        rideId: String
    ): String? {
        val currentUser = authenticationManager.currentUser.value
        if (currentUser?.id == null || currentUser.id == rideOwnerId) {
            return null // Can't create conversation with self or when not authenticated
        }
        
        return when (val result = messagingRepository.createOrGetConversation(
            user1Id = currentUser.id,
            user2Id = rideOwnerId,
            rideId = rideId
        )) {
            is MessagingResult.Success<*> -> {
                val conversation = result.data as com.app.ridelink.data.model.Conversation
                
                // Send initial ride status message
                messagingRepository.sendRideStatusMessage(
                    conversationId = conversation.id,
                    senderId = currentUser.id,
                    receiverId = rideOwnerId,
                    messageType = com.app.ridelink.data.model.MessageType.RIDE_REQUEST,
                    rideId = rideId
                )
                
                conversation.id
            }
            else -> null
        }
    }
    
    /**
     * Send ride status update message
     */
    suspend fun sendRideStatusUpdate(
        conversationId: String,
        receiverId: String,
        messageType: com.app.ridelink.data.model.MessageType,
        rideId: String
    ): Boolean {
        val currentUser = authenticationManager.currentUser.value
        if (currentUser?.id == null) return false
        
        return when (messagingRepository.sendRideStatusMessage(
            conversationId = conversationId,
            senderId = currentUser.id,
            receiverId = receiverId,
            messageType = messageType,
            rideId = rideId
        )) {
            is MessagingResult.Success<*> -> true
            else -> false
        }
    }
    
    /**
     * Get conversation ID for a specific ride between two users
     */
    suspend fun getRideConversationId(
        otherUserId: String,
        rideId: String
    ): String? {
        val currentUser = authenticationManager.currentUser.value
        if (currentUser?.id == null) return null
        
        return when (val result = messagingRepository.createOrGetConversation(
            user1Id = currentUser.id,
            user2Id = otherUserId,
            rideId = rideId
        )) {
            is MessagingResult.Success<*> -> {
                val conversation = result.data as com.app.ridelink.data.model.Conversation?
                conversation?.id
            }
            else -> null
        }
    }
}

/**
 * Composable helper for ride messaging integration
 */
@Composable
fun RideMessagingHandler(
    rideId: String,
    rideOwnerId: String,
    onConversationCreated: (String) -> Unit = {},
    messagingIntegration: MessagingIntegration = hiltViewModel<MessagingIntegrationViewModel>().messagingIntegration
) {
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(rideId, rideOwnerId) {
        coroutineScope.launch {
            val conversationId = messagingIntegration.createRideConversation(
                rideOwnerId = rideOwnerId,
                rideId = rideId
            )
            conversationId?.let { onConversationCreated(it) }
        }
    }
}

/**
 * ViewModel wrapper for MessagingIntegration to work with Hilt in Composables
 */
@dagger.hilt.android.lifecycle.HiltViewModel
class MessagingIntegrationViewModel @Inject constructor(
    val messagingIntegration: MessagingIntegration
) : androidx.lifecycle.ViewModel()

/**
 * Extension functions for common ride messaging scenarios
 */
object RideMessagingHelper {
    
    /**
     * Standard ride status messages
     */
    object StatusMessages {
        const val RIDE_REQUESTED = "Ride request sent"
        const val RIDE_ACCEPTED = "Ride request accepted"
        const val RIDE_DECLINED = "Ride request declined"
        const val RIDE_CANCELLED = "Ride has been cancelled"
        const val RIDE_STARTED = "Ride has started"
        const val RIDE_COMPLETED = "Ride completed"
        const val PICKUP_ARRIVED = "Driver has arrived at pickup location"
        const val PICKUP_WAITING = "Driver is waiting at pickup location"
    }
    
    /**
     * Generate contextual message for ride actions
     */
    fun getContextualMessage(action: String, rideDetails: String): String {
        return when (action) {
            "join" -> "I'd like to join your ride: $rideDetails"
            "offer" -> "I'm offering a ride: $rideDetails"
            "pickup" -> "Where should I pick you up for the ride: $rideDetails?"
            "dropoff" -> "Where would you like to be dropped off for the ride: $rideDetails?"
            "contact" -> "Hi! I'm interested in your ride: $rideDetails. Can we discuss the details?"
            else -> "Regarding the ride: $rideDetails"
        }
    }
}