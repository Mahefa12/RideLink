package com.app.ridelink.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "conversations",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user1Id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user2Id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["user1Id"]),
        Index(value = ["user2Id"]),
        Index(value = ["lastMessageTimestamp"]),
        Index(value = ["rideId"])
    ]
)
data class Conversation(
    @PrimaryKey
    val id: String,
    val user1Id: String,
    val user2Id: String,
    val rideId: String? = null, // Optional: link to specific ride
    val lastMessage: String? = null,
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val lastMessageSenderId: String? = null,
    val unreadCount: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class ConversationWithParticipant(
    val id: String,
    val user1Id: String,
    val user2Id: String,
    val rideId: String?,
    val lastMessage: String?,
    val lastMessageTimestamp: Long,
    val lastMessageSenderId: String?,
    val unreadCount: Int,
    val isActive: Boolean,
    val createdAt: Long,
    val participantName: String,
    val participantPhotoUrl: String?,
    val participantId: String
)

data class ConversationDetails(
    val conversation: Conversation,
    val participant: User,
    val lastMessage: Message?
)