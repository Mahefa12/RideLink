package com.app.ridelink.data.dao

import androidx.room.*
import com.app.ridelink.data.model.Message
import com.app.ridelink.data.model.MessageWithSender
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    
    @Query("""
        SELECT m.*, u.displayName as senderName, u.photoUrl as senderPhotoUrl 
        FROM messages m 
        INNER JOIN users u ON m.senderId = u.id 
        WHERE m.conversationId = :conversationId 
        ORDER BY m.timestamp ASC
    """)
    fun getMessagesForConversation(conversationId: String): Flow<List<MessageWithSender>>
    
    @Query("""
        SELECT m.id, m.conversationId, m.senderId, m.receiverId, m.content, m.messageType, 
               m.timestamp, m.isRead, m.isDelivered, m.rideId,
               u.displayName as senderName, u.photoUrl as senderPhotoUrl
        FROM messages m
        LEFT JOIN users u ON m.senderId = u.id
        WHERE m.conversationId = :conversationId
        ORDER BY m.timestamp ASC
        LIMIT :limit
    """)
    suspend fun getRecentMessagesForConversation(
        conversationId: String,
        limit: Int = 50
    ): List<MessageWithSender>
    
    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): Message?
    
    @Query("""
        SELECT * FROM messages 
        WHERE conversationId = :conversationId 
        ORDER BY timestamp DESC 
        LIMIT 1
    """)
    suspend fun getLastMessageForConversation(conversationId: String): Message?
    
    @Query("""
        SELECT COUNT(*) FROM messages 
        WHERE conversationId = :conversationId 
        AND receiverId = :userId 
        AND isRead = 0
    """)
    suspend fun getUnreadMessageCount(conversationId: String, userId: String): Int
    
    @Query("""
        SELECT COUNT(*) FROM messages 
        WHERE receiverId = :userId 
        AND isRead = 0
    """)
    suspend fun getTotalUnreadMessageCount(userId: String): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<Message>)
    
    @Update
    suspend fun updateMessage(message: Message)
    
    @Query("""
        UPDATE messages 
        SET isRead = 1 
        WHERE conversationId = :conversationId 
        AND receiverId = :userId 
        AND isRead = 0
    """)
    suspend fun markMessagesAsRead(conversationId: String, userId: String)
    
    @Query("""
        UPDATE messages 
        SET isDelivered = 1 
        WHERE id = :messageId
    """)
    suspend fun markMessageAsDelivered(messageId: String)
    
    @Delete
    suspend fun deleteMessage(message: Message)
    
    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)
    
    @Query("""
        SELECT * FROM messages 
        WHERE rideId = :rideId 
        ORDER BY timestamp ASC
    """)
    suspend fun getMessagesForRide(rideId: String): List<Message>
    
    @Query("""
        SELECT * FROM messages 
        WHERE senderId = :userId OR receiverId = :userId 
        ORDER BY timestamp DESC 
        LIMIT :limit
    """)
    suspend fun getRecentMessagesForUser(userId: String, limit: Int = 100): List<Message>
}