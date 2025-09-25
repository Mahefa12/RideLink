package com.app.ridelink.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["senderId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["receiverId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["senderId"]),
        Index(value = ["receiverId"]),
        Index(value = ["conversationId"]),
        Index(value = ["timestamp"])
    ]
)
data class Message(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val messageType: MessageType = MessageType.TEXT,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val isDelivered: Boolean = false,
    val rideId: String? = null // Optional: link to specific ride for context
)

enum class MessageType {
    TEXT,
    LOCATION,
    RIDE_REQUEST,
    RIDE_ACCEPTED,
    RIDE_CANCELLED,
    RIDE_STATUS,
    PICKUP_ARRIVED,
    RIDE_STARTED,
    RIDE_COMPLETED
}

data class MessageWithSender(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val messageType: MessageType,
    val timestamp: Long,
    val isRead: Boolean,
    val isDelivered: Boolean,
    val rideId: String?,
    val senderName: String,
    val senderPhotoUrl: String?
)