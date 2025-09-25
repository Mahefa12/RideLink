package com.app.ridelink.data.repository

import com.app.ridelink.data.dao.MessageDao
import com.app.ridelink.data.dao.ConversationDao
import com.app.ridelink.data.dao.UserDao
import com.app.ridelink.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

sealed class MessagingResult {
    data class Success<T>(val data: T) : MessagingResult()
    data class Error(val message: String) : MessagingResult()
    object Loading : MessagingResult()
}

@Singleton
class MessagingRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
    private val userDao: UserDao
) {
    
    /**
     * Get all conversations for a user
     */
    suspend fun getConversationsForUser(userId: String): List<ConversationWithParticipant> {
        return conversationDao.getConversationsForUser(userId)
    }
    
    /**
     * Get messages for a specific conversation
     */
    fun getMessagesForConversation(conversationId: String): Flow<List<MessageWithSender>> {
        return messageDao.getMessagesForConversation(conversationId)
    }
    
    /**
     * Get conversation details with participant info
     */
    fun getConversationDetails(conversationId: String, currentUserId: String): Flow<ConversationDetails?> {
        Log.d("RideLink", "MessagingRepository: Getting conversation details for ID: $conversationId, user: $currentUserId")
        return combine(
            conversationDao.getConversationByIdFlow(conversationId),
            messageDao.getMessagesForConversation(conversationId)
        ) { conversation, messages ->
            Log.d("RideLink", "MessagingRepository: Found conversation: $conversation")
            Log.d("RideLink", "MessagingRepository: Found ${messages.size} messages")
            if (conversation != null) {
                val participantId = if (conversation.user1Id == currentUserId) {
                    conversation.user2Id
                } else {
                    conversation.user1Id
                }
                Log.d("RideLink", "MessagingRepository: Participant ID: $participantId")
                
                val participant = userDao.getUserByIdSync(participantId)
                Log.d("RideLink", "MessagingRepository: Found participant: $participant")
                val lastMessageWithSender = messages.lastOrNull()
                val lastMessage = lastMessageWithSender?.let { msg ->
                    Message(
                        id = msg.id,
                        conversationId = msg.conversationId,
                        senderId = msg.senderId,
                        receiverId = msg.receiverId,
                        content = msg.content,
                        messageType = msg.messageType,
                        timestamp = msg.timestamp,
                        isRead = msg.isRead,
                        isDelivered = msg.isDelivered,
                        rideId = msg.rideId
                    )
                }
                
                if (participant != null) {
                    val details = ConversationDetails(conversation, participant, lastMessage)
                    Log.d("RideLink", "MessagingRepository: Created conversation details: $details")
                    details
                } else {
                    Log.e("RideLink", "MessagingRepository: Participant not found for ID: $participantId")
                    null
                }
            } else {
                Log.e("RideLink", "MessagingRepository: Conversation not found for ID: $conversationId")
                null
            }
        }
    }
    
    /**
     * Send a message
     */
    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        receiverId: String,
        content: String,
        messageType: MessageType = MessageType.TEXT,
        rideId: String? = null
    ): MessagingResult {
        return try {
            val messageId = UUID.randomUUID().toString()
            val message = Message(
                id = messageId,
                conversationId = conversationId,
                senderId = senderId,
                receiverId = receiverId,
                content = content,
                messageType = messageType,
                timestamp = System.currentTimeMillis(),
                isRead = false,
                isDelivered = false,
                rideId = rideId
            )
            
            // Insert the message
            messageDao.insertMessage(message)
            
            // Update conversation with last message info
            conversationDao.updateLastMessage(
                conversationId = conversationId,
                lastMessage = content,
                timestamp = message.timestamp,
                senderId = senderId
            )
            
            // Increment unread count for receiver
            conversationDao.incrementUnreadCount(conversationId)
            
            MessagingResult.Success(message)
        } catch (e: Exception) {
            MessagingResult.Error(e.message ?: "Failed to send message")
        }
    }
    
    /**
     * Create or get existing conversation between two users
     */
    suspend fun createOrGetConversation(
        user1Id: String,
        user2Id: String,
        rideId: String? = null
    ): MessagingResult {
        return try {
            // Look for existing conversation between users
            val existingConversation = conversationDao.getConversationBetweenUsers(user1Id, user2Id)
            
            if (existingConversation != null) {
                MessagingResult.Success(existingConversation)
            } else {
                // Create new conversation
                val conversationId = UUID.randomUUID().toString()
                val conversation = Conversation(
                    id = conversationId,
                    user1Id = user1Id,
                    user2Id = user2Id,
                    rideId = rideId,
                    lastMessage = null,
                    lastMessageTimestamp = System.currentTimeMillis(),
                    lastMessageSenderId = null,
                    unreadCount = 0,
                    isActive = true,
                    createdAt = System.currentTimeMillis()
                )
                
                conversationDao.insertConversation(conversation)
                MessagingResult.Success(conversation)
            }
        } catch (e: Exception) {
            MessagingResult.Error(e.message ?: "Failed to create conversation")
        }
    }
    
    /**
     * Mark messages as read
     */
    suspend fun markMessagesAsRead(conversationId: String, userId: String): MessagingResult {
        return try {
            messageDao.markMessagesAsRead(conversationId, userId)
            conversationDao.resetUnreadCount(conversationId)
            MessagingResult.Success(Unit)
        } catch (e: Exception) {
            MessagingResult.Error(e.message ?: "Failed to mark messages as read")
        }
    }
    
    /**
     * Get unread message count for user
     */
    suspend fun getUnreadMessageCount(userId: String): Int {
        return try {
            messageDao.getTotalUnreadMessageCount(userId)
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Get conversation for a specific ride
     */
    suspend fun getConversationForRide(rideId: String): Conversation? {
        return try {
            conversationDao.getConversationForRide(rideId)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Delete conversation
     */
    suspend fun deleteConversation(conversationId: String): MessagingResult {
        return try {
            messageDao.deleteMessagesForConversation(conversationId)
            conversationDao.deleteConversationById(conversationId)
            MessagingResult.Success(Unit)
        } catch (e: Exception) {
            MessagingResult.Error(e.message ?: "Failed to delete conversation")
        }
    }
    
    /**
     * Search messages by content
     */
    suspend fun searchMessages(userId: String, query: String): List<MessageWithSender> {
        return try {
            // This would require a more complex query, for now return empty
            // In a real implementation, you'd add a search query to MessageDao
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Send automated ride status message
     */
    suspend fun sendRideStatusMessage(
        conversationId: String,
        senderId: String,
        receiverId: String,
        messageType: MessageType,
        rideId: String
    ): MessagingResult {
        val content = when (messageType) {
            MessageType.RIDE_REQUEST -> "Ride request sent"
            MessageType.RIDE_ACCEPTED -> "Ride request accepted"
            MessageType.RIDE_CANCELLED -> "Ride has been cancelled"
            MessageType.PICKUP_ARRIVED -> "Driver has arrived at pickup location"
            MessageType.RIDE_STARTED -> "Ride has started"
            MessageType.RIDE_COMPLETED -> "Ride completed successfully"
            else -> "Ride status update"
        }
        
        return sendMessage(
            conversationId = conversationId,
            senderId = senderId,
            receiverId = receiverId,
            content = content,
            messageType = messageType,
            rideId = rideId
        )
    }
}