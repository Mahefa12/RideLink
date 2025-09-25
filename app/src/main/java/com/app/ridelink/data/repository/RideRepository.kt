package com.app.ridelink.data.repository

import com.app.ridelink.data.dao.RideDao
import com.app.ridelink.data.dao.UserDao
import com.app.ridelink.data.model.Ride
import com.app.ridelink.data.model.RideWithDriver
import com.app.ridelink.data.model.RideSearchFilters
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RideRepository @Inject constructor(
    private val rideDao: RideDao,
    private val userDao: UserDao
) {
    
    /**
     * Create a new ride
     */
    suspend fun createRide(
        driverId: String,
        title: String,
        description: String?,
        originAddress: String,
        destinationAddress: String,
        destinationState: String,
        departureTime: Long,
        availableSeats: Int,
        pricePerSeat: Double,
        vehicleType: String = "Car",
        vehicleModel: String? = null,
        vehiclePlateNumber: String? = null,
        petsAllowed: Boolean = false,
        smokingAllowed: Boolean = false,
        recurringDays: String = ""
    ): Result<String> {
        return try {
            val rideId = UUID.randomUUID().toString()
            val ride = Ride(
                id = rideId,
                driverId = driverId,
                title = title,
                description = description,
                originAddress = originAddress,
                destinationAddress = destinationAddress,
                destinationState = destinationState,
                departureTime = departureTime,
                availableSeats = availableSeats,
                pricePerSeat = pricePerSeat,
                vehicleType = vehicleType,
                vehicleModel = vehicleModel,
                vehiclePlateNumber = vehiclePlateNumber,
                petsAllowed = petsAllowed,
                smokingAllowed = smokingAllowed,
                recurringDays = recurringDays
            )
            
            rideDao.insertRide(ride)
            Log.d("RideLink", "RideRepository: Created ride with ID: $rideId")
            Result.success(rideId)
        } catch (e: Exception) {
            Log.e("RideLink", "RideRepository: Error creating ride: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get available rides
     */
    suspend fun getAvailableRides(): List<Ride> {
        return try {
            val rides = rideDao.getAvailableRides()
            Log.d("RideLink", "RideRepository: Found ${rides.size} available rides")
            rides
        } catch (e: Exception) {
            Log.e("RideLink", "RideRepository: Error getting available rides: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Search rides with filters
     */
    suspend fun searchRides(filters: RideSearchFilters): List<Ride> {
        return try {
            val rides = rideDao.searchRides(
                originAddress = filters.originAddress,
                destinationAddress = filters.destinationAddress,
                maxPrice = filters.maxPrice,
                minSeats = filters.minSeats,
                petsAllowed = filters.petsAllowed,
                vehicleType = filters.vehicleType
            )
            Log.d("RideLink", "RideRepository: Found ${rides.size} rides matching filters")
            rides
        } catch (e: Exception) {
            Log.e("RideLink", "RideRepository: Error searching rides: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get rides by driver
     */
    suspend fun getRidesByDriver(driverId: String): List<Ride> {
        return try {
            val rides = rideDao.getRidesByDriver(driverId)
            Log.d("RideLink", "RideRepository: Found ${rides.size} rides for driver: $driverId")
            rides
        } catch (e: Exception) {
            Log.e("RideLink", "RideRepository: Error getting rides by driver: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get rides by driver as Flow
     */
    fun getRidesByDriverFlow(driverId: String): Flow<List<Ride>> {
        return rideDao.getRidesByDriverFlow(driverId)
    }
    
    /**
     * Get ride by ID
     */
    suspend fun getRideById(rideId: String): Ride? {
        return try {
            val ride = rideDao.getRideById(rideId)
            if (ride != null) {
                Log.d("RideLink", "RideRepository: Found ride: $rideId")
            } else {
                Log.w("RideLink", "RideRepository: Ride not found: $rideId")
            }
            ride
        } catch (e: Exception) {
            Log.e("RideLink", "RideRepository: Error getting ride by ID: ${e.message}", e)
            null
        }
    }
    
    /**
     * Get ride by ID as Flow
     */
    fun getRideByIdFlow(rideId: String): Flow<Ride?> {
        return rideDao.getRideByIdFlow(rideId)
    }
    
    /**
     * Insert a ride directly (for simple ride creation)
     */
    suspend fun insertRide(ride: Ride): Result<Unit> {
        return try {
            rideDao.insertRide(ride)
            Log.d("RideLink", "RideRepository: Inserted ride: ${ride.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("RideLink", "RideRepository: Error inserting ride: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update ride
     */
    suspend fun updateRide(ride: Ride): Result<Unit> {
        return try {
            rideDao.updateRide(ride.copy(updatedAt = System.currentTimeMillis()))
            Log.d("RideLink", "RideRepository: Updated ride: ${ride.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("RideLink", "RideRepository: Error updating ride: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Book seats for a ride
     */
    suspend fun bookSeats(rideId: String, seatsBooked: Int): Result<Boolean> {
        return try {
            val rowsAffected = rideDao.bookSeats(rideId, seatsBooked)
            val success = rowsAffected > 0
            if (success) {
                Log.d("RideLink", "RideRepository: Booked $seatsBooked seats for ride: $rideId")
            } else {
                Log.w("RideLink", "RideRepository: Failed to book seats - insufficient availability")
            }
            Result.success(success)
        } catch (e: Exception) {
            Log.e("RideLink", "RideRepository: Error booking seats: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Cancel/deactivate a ride
     */
    suspend fun cancelRide(rideId: String): Result<Unit> {
        return try {
            rideDao.deactivateRide(rideId)
            Log.d("RideLink", "RideRepository: Cancelled ride: $rideId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("RideLink", "RideRepository: Error cancelling ride: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete a ride
     */
    suspend fun deleteRide(rideId: String): Result<Unit> {
        return try {
            rideDao.deleteRideById(rideId)
            Log.d("RideLink", "RideRepository: Deleted ride: $rideId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("RideLink", "RideRepository: Error deleting ride: ${e.message}", e)
            Result.failure(e)
        }
    }
}