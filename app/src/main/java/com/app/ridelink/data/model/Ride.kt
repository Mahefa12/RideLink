package com.app.ridelink.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "rides",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["driverId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["driverId"]),
        Index(value = ["departureTime"]),
        Index(value = ["originLatitude", "originLongitude"]),
        Index(value = ["destinationLatitude", "destinationLongitude"])
    ]
)
data class Ride(
    @PrimaryKey
    val id: String,
    val driverId: String,
    val title: String,
    val description: String? = null,
    val originAddress: String,
    val destinationAddress: String,
    val originLatitude: Double? = null,
    val originLongitude: Double? = null,
    val destinationLatitude: Double? = null,
    val destinationLongitude: Double? = null,
    val destinationState: String,
    val departureTime: Long,
    val availableSeats: Int,
    val pricePerSeat: Double,
    val vehicleType: String = "Car",
    val vehicleModel: String? = null,
    val vehiclePlateNumber: String? = null,
    val petsAllowed: Boolean = false,
    val smokingAllowed: Boolean = false,
    val recurringDays: String = "", // JSON string of recurring days
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// Data class for ride with driver information
data class RideWithDriver(
    val ride: Ride,
    val driver: User,
    val rating: Double = 0.0,
    val totalRides: Int = 0
)

// Enum for recurring days
enum class RecurringDay {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
}

// Data class for ride search filters
data class RideSearchFilters(
    val originAddress: String? = null,
    val destinationAddress: String? = null,
    val departureDate: Long? = null,
    val maxPrice: Double? = null,
    val minSeats: Int? = null,
    val petsAllowed: Boolean? = null,
    val vehicleType: String? = null
)