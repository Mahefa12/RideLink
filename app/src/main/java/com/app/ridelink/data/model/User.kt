package com.app.ridelink.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val phoneNumber: String?,
    val isEmailVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class UserPreferences(
    val notificationsEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val locationSharingEnabled: Boolean = true,
    val autoAcceptRides: Boolean = false
)