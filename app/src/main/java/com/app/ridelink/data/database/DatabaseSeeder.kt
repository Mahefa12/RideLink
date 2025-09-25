package com.app.ridelink.data.database

import com.app.ridelink.data.dao.UserDao
import com.app.ridelink.data.dao.ConversationDao
import com.app.ridelink.data.dao.MessageDao
import com.app.ridelink.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneOffset
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSeeder @Inject constructor(
    private val userDao: UserDao,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) {
    
    suspend fun seedDatabase() {
        withContext(Dispatchers.IO) {
            // Verify if database is already populated
            val existingUsers = userDao.getAllUsers()
            Log.d("RideLink", "DatabaseSeeder: Found ${existingUsers.size} existing users")
            if (existingUsers.isNotEmpty()) {
                Log.d("RideLink", "DatabaseSeeder: Database already seeded, skipping...")
                return@withContext // Database already seeded
            }
            
            Log.d("RideLink", "DatabaseSeeder: Starting fresh database seeding...")
            
            // Create sample users
            val currentUser = User(
                id = "current_user",
                email = "user@example.com",
                displayName = "Current User",
                photoUrl = null,
                phoneNumber = null,
                isEmailVerified = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            val users = listOf(
                currentUser,
                User(
                    id = "user_sarah",
                    email = "sarah.johnson@example.com",
                    displayName = "Sarah Johnson",
                    photoUrl = null,
                    phoneNumber = null,
                    isEmailVerified = true,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ),
                User(
                    id = "user_mike",
                    email = "mike.chen@example.com",
                    displayName = "Mike Chen",
                    photoUrl = null,
                    phoneNumber = null,
                    isEmailVerified = true,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ),
                User(
                    id = "user_emma",
                    email = "emma.wilson@example.com",
                    displayName = "Emma Wilson",
                    photoUrl = null,
                    phoneNumber = null,
                    isEmailVerified = true,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ),
                User(
                    id = "user_david",
                    email = "david.brown@example.com",
                    displayName = "David Brown",
                    photoUrl = null,
                    phoneNumber = null,
                    isEmailVerified = true,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ),
                User(
                    id = "user_lisa",
                    email = "lisa.garcia@example.com",
                    displayName = "Lisa Garcia",
                    photoUrl = null,
                    phoneNumber = null,
                    isEmailVerified = true,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            
            // Insert users
            users.forEach { user ->
                userDao.insertUser(user)
            }
            
            // Create conversations
            val now = System.currentTimeMillis()
            val conversations = listOf(
                Conversation(
                    id = "msg1",
                    user1Id = "current_user",
                    user2Id = "user_sarah",
                    rideId = "ride1",
                    lastMessage = "Thanks for accepting my ride request! I'll be ready at 8 AM sharp.",
                    lastMessageTimestamp = now - (30 * 60 * 1000), // 30 minutes ago
                    lastMessageSenderId = "user_sarah",
                    unreadCount = 1,
                    isActive = true,
                    createdAt = now - (2 * 60 * 60 * 1000) // 2 hours ago
                ),
                Conversation(
                    id = "msg2",
                    user1Id = "current_user",
                    user2Id = "user_mike",
                    rideId = "ride2",
                    lastMessage = "Hey! Are you still available for the ride to the mall?",
                    lastMessageTimestamp = now - (3 * 60 * 60 * 1000), // 3 hours ago
                    lastMessageSenderId = "user_mike",
                    unreadCount = 0,
                    isActive = true,
                    createdAt = now - (4 * 60 * 60 * 1000) // 4 hours ago
                ),
                Conversation(
                    id = "msg3",
                    user1Id = "current_user",
                    user2Id = "user_emma",
                    rideId = "ride3",
                    lastMessage = "The ride has been confirmed. See you tomorrow at 10 AM!",
                    lastMessageTimestamp = now - (24 * 60 * 60 * 1000), // 1 day ago
                    lastMessageSenderId = "user_emma",
                    unreadCount = 0,
                    isActive = true,
                    createdAt = now - (25 * 60 * 60 * 1000) // 25 hours ago
                ),
                Conversation(
                    id = "msg4",
                    user1Id = "current_user",
                    user2Id = "user_david",
                    rideId = null,
                    lastMessage = "Sorry, I need to cancel the ride. Something urgent came up.",
                    lastMessageTimestamp = now - (2 * 24 * 60 * 60 * 1000), // 2 days ago
                    lastMessageSenderId = "user_david",
                    unreadCount = 0,
                    isActive = true,
                    createdAt = now - (3 * 24 * 60 * 60 * 1000) // 3 days ago
                ),
                Conversation(
                    id = "msg5",
                    user1Id = "current_user",
                    user2Id = "user_lisa",
                    rideId = "ride5",
                    lastMessage = "Great ride! Thanks for the smooth trip to the airport.",
                    lastMessageTimestamp = now - (3 * 24 * 60 * 60 * 1000), // 3 days ago
                    lastMessageSenderId = "user_lisa",
                    unreadCount = 0,
                    isActive = true,
                    createdAt = now - (4 * 24 * 60 * 60 * 1000) // 4 days ago
                )
            )
            
            // Insert conversations
            conversations.forEach { conversation ->
                Log.d("RideLink", "DatabaseSeeder: Inserting conversation: ${conversation.id}")
                conversationDao.insertConversation(conversation)
            }
            
            // Create sample messages for each conversation
            val messages = mutableListOf<Message>()
            
            // Messages for Sarah Johnson conversation
            messages.addAll(listOf(
                Message(
                    id = "msg1_1",
                    conversationId = "msg1",
                    senderId = "current_user",
                    receiverId = "user_sarah",
                    content = "Hi Sarah! I saw your ride request to downtown. I can give you a ride.",
                    messageType = MessageType.TEXT,
                    timestamp = now - (2 * 60 * 60 * 1000),
                    isRead = true,
                    isDelivered = true,
                    rideId = "ride1"
                ),
                Message(
                    id = "msg1_2",
                    conversationId = "msg1",
                    senderId = "user_sarah",
                    receiverId = "current_user",
                    content = "Thanks for accepting my ride request! I'll be ready at 8 AM sharp.",
                    messageType = MessageType.TEXT,
                    timestamp = now - (30 * 60 * 1000),
                    isRead = false,
                    isDelivered = true,
                    rideId = "ride1"
                )
            ))
            
            // Messages for Mike Chen conversation
            messages.addAll(listOf(
                Message(
                    id = "msg2_1",
                    conversationId = "msg2",
                    senderId = "user_mike",
                    receiverId = "current_user",
                    content = "Hey! Are you still available for the ride to the mall?",
                    messageType = MessageType.TEXT,
                    timestamp = now - (3 * 60 * 60 * 1000),
                    isRead = true,
                    isDelivered = true,
                    rideId = "ride2"
                )
            ))
            
            // Messages for Emma Wilson conversation
            messages.addAll(listOf(
                Message(
                    id = "msg3_1",
                    conversationId = "msg3",
                    senderId = "current_user",
                    receiverId = "user_emma",
                    content = "Hi Emma, I can confirm the ride for tomorrow at 10 AM.",
                    messageType = MessageType.TEXT,
                    timestamp = now - (25 * 60 * 60 * 1000),
                    isRead = true,
                    isDelivered = true,
                    rideId = "ride3"
                ),
                Message(
                    id = "msg3_2",
                    conversationId = "msg3",
                    senderId = "user_emma",
                    receiverId = "current_user",
                    content = "The ride has been confirmed. See you tomorrow at 10 AM!",
                    messageType = MessageType.TEXT,
                    timestamp = now - (24 * 60 * 60 * 1000),
                    isRead = true,
                    isDelivered = true,
                    rideId = "ride3"
                )
            ))
            
            // Messages for David Brown conversation
            messages.addAll(listOf(
                Message(
                    id = "msg4_1",
                    conversationId = "msg4",
                    senderId = "user_david",
                    receiverId = "current_user",
                    content = "Sorry, I need to cancel the ride. Something urgent came up.",
                    messageType = MessageType.TEXT,
                    timestamp = now - (2 * 24 * 60 * 60 * 1000),
                    isRead = true,
                    isDelivered = true,
                    rideId = null
                )
            ))
            
            // Messages for Lisa Garcia conversation
            messages.addAll(listOf(
                Message(
                    id = "msg5_1",
                    conversationId = "msg5",
                    senderId = "user_lisa",
                    receiverId = "current_user",
                    content = "Great ride! Thanks for the smooth trip to the airport.",
                    messageType = MessageType.TEXT,
                    timestamp = now - (3 * 24 * 60 * 60 * 1000),
                    isRead = true,
                    isDelivered = true,
                    rideId = "ride5"
                )
            ))
            
            // Insert all messages
            messages.forEach { message ->
                Log.d("RideLink", "DatabaseSeeder: Inserting message: ${message.id} for conversation: ${message.conversationId}")
                messageDao.insertMessage(message)
            }
            
            Log.d("RideLink", "DatabaseSeeder: Finished seeding database with ${conversations.size} conversations and ${messages.size} messages")
        }
    }
}