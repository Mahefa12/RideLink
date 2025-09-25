package com.app.ridelink.data.dao

import androidx.room.*
import com.app.ridelink.data.model.Conversation
import com.app.ridelink.data.model.ConversationWithParticipant
import com.app.ridelink.data.model.ConversationDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    
    @Query("""
        SELECT c.id, c.user1Id, c.user2Id, c.rideId, c.lastMessage, c.lastMessageTimestamp, 
               c.lastMessageSenderId, c.unreadCount, c.isActive, c.createdAt,
               u.displayName as participantName, u.photoUrl as participantPhotoUrl, u.id as participantId
        FROM conversations c
        LEFT JOIN users u ON (CASE WHEN c.user1Id = :userId THEN c.user2Id ELSE c.user1Id END) = u.id
        WHERE (c.user1Id = :userId OR c.user2Id = :userId) AND c.isActive = 1
        ORDER BY c.lastMessageTimestamp DESC
    """)
    suspend fun getConversationsForUser(userId: String): List<ConversationWithParticipant>
    
    @Query("""
        SELECT * FROM conversations 
        WHERE ((user1Id = :user1Id AND user2Id = :user2Id) OR (user1Id = :user2Id AND user2Id = :user1Id))
        AND isActive = 1
        LIMIT 1
    """)
    suspend fun getConversationBetweenUsers(user1Id: String, user2Id: String): Conversation?
    
    @Query("""
        SELECT * FROM conversations 
        WHERE rideId = :rideId 
        AND isActive = 1
        LIMIT 1
    """)
    suspend fun getConversationForRide(rideId: String): Conversation?
    
    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    suspend fun getConversationById(conversationId: String): Conversation?
    
    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    fun getConversationByIdFlow(conversationId: String): Flow<Conversation?>
    
    @Query("""
        SELECT COUNT(*) FROM conversations 
        WHERE (user1Id = :userId OR user2Id = :userId) 
        AND unreadCount > 0 
        AND isActive = 1
    """)
    suspend fun getUnreadConversationCount(userId: String): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: Conversation)
    
    @Update
    suspend fun updateConversation(conversation: Conversation)
    
    @Query("""
        UPDATE conversations 
        SET lastMessage = :lastMessage, 
            lastMessageTimestamp = :timestamp, 
            lastMessageSenderId = :senderId
        WHERE id = :conversationId
    """)
    suspend fun updateLastMessage(
        conversationId: String, 
        lastMessage: String, 
        timestamp: Long, 
        senderId: String
    )
    
    @Query("""
        UPDATE conversations 
        SET unreadCount = unreadCount + 1 
        WHERE id = :conversationId
    """)
    suspend fun incrementUnreadCount(conversationId: String)
    
    @Query("""
        UPDATE conversations 
        SET unreadCount = 0 
        WHERE id = :conversationId
    """)
    suspend fun resetUnreadCount(conversationId: String)
    
    @Query("""
        UPDATE conversations 
        SET isActive = 0 
        WHERE id = :conversationId
    """)
    suspend fun deactivateConversation(conversationId: String)
    
    @Delete
    suspend fun deleteConversation(conversation: Conversation)
    
    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversationById(conversationId: String)
    
    @Query("""
        SELECT * FROM conversations 
        WHERE (user1Id = :userId OR user2Id = :userId) 
        AND isActive = 1
        ORDER BY lastMessageTimestamp DESC
    """)
    suspend fun getAllConversationsForUser(userId: String): List<Conversation>
}