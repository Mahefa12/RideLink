package com.app.ridelink.data.dao

import androidx.room.*
import com.app.ridelink.data.model.Ride
import com.app.ridelink.data.model.RideWithDriver
import com.app.ridelink.data.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface RideDao {
    
    @Query("""
        SELECT * FROM rides 
        WHERE isActive = 1
        AND departureTime > :currentTime
        ORDER BY departureTime ASC
    """)
    suspend fun getAvailableRides(currentTime: Long = System.currentTimeMillis()): List<Ride>
    
    @Query("""
        SELECT * FROM rides 
        WHERE isActive = 1
        AND departureTime > :currentTime
        AND (:originAddress IS NULL OR originAddress LIKE '%' || :originAddress || '%')
        AND (:destinationAddress IS NULL OR destinationAddress LIKE '%' || :destinationAddress || '%')
        AND (:maxPrice IS NULL OR pricePerSeat <= :maxPrice)
        AND (:minSeats IS NULL OR availableSeats >= :minSeats)
        AND (:petsAllowed IS NULL OR petsAllowed = :petsAllowed)
        AND (:vehicleType IS NULL OR vehicleType = :vehicleType)
        ORDER BY departureTime ASC
    """)
    suspend fun searchRides(
        currentTime: Long = System.currentTimeMillis(),
        originAddress: String? = null,
        destinationAddress: String? = null,
        maxPrice: Double? = null,
        minSeats: Int? = null,
        petsAllowed: Boolean? = null,
        vehicleType: String? = null
    ): List<Ride>
    
    @Query("""
        SELECT * FROM rides 
        WHERE driverId = :driverId 
        AND isActive = 1
        ORDER BY departureTime DESC
    """)
    suspend fun getRidesByDriver(driverId: String): List<Ride>
    
    @Query("""
        SELECT * FROM rides 
        WHERE driverId = :driverId 
        AND isActive = 1
        ORDER BY departureTime DESC
    """)
    fun getRidesByDriverFlow(driverId: String): Flow<List<Ride>>
    
    @Query("SELECT * FROM rides WHERE id = :rideId")
    suspend fun getRideById(rideId: String): Ride?
    
    @Query("SELECT * FROM rides WHERE id = :rideId")
    fun getRideByIdFlow(rideId: String): Flow<Ride?>
    
    @Query("SELECT * FROM users WHERE id = :driverId")
    suspend fun getDriverById(driverId: String): User?
    
    @Query("""
        SELECT COUNT(*) FROM rides 
        WHERE driverId = :driverId 
        AND isActive = 1
    """)
    suspend fun getRideCountByDriver(driverId: String): Int
    
    @Query("""
        SELECT * FROM rides 
        WHERE departureTime BETWEEN :startTime AND :endTime
        AND isActive = 1
        ORDER BY departureTime ASC
    """)
    suspend fun getRidesByDateRange(startTime: Long, endTime: Long): List<Ride>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRide(ride: Ride)
    
    @Update
    suspend fun updateRide(ride: Ride)
    
    @Query("""
        UPDATE rides 
        SET availableSeats = availableSeats - :seatsBooked,
            updatedAt = :updatedAt
        WHERE id = :rideId
        AND availableSeats >= :seatsBooked
    """)
    suspend fun bookSeats(rideId: String, seatsBooked: Int, updatedAt: Long = System.currentTimeMillis()): Int
    
    @Query("""
        UPDATE rides 
        SET availableSeats = availableSeats + :seatsReleased,
            updatedAt = :updatedAt
        WHERE id = :rideId
    """)
    suspend fun releaseSeats(rideId: String, seatsReleased: Int, updatedAt: Long = System.currentTimeMillis())
    
    @Query("""
        UPDATE rides 
        SET isActive = 0,
            updatedAt = :updatedAt
        WHERE id = :rideId
    """)
    suspend fun deactivateRide(rideId: String, updatedAt: Long = System.currentTimeMillis())
    
    @Delete
    suspend fun deleteRide(ride: Ride)
    
    @Query("DELETE FROM rides WHERE id = :rideId")
    suspend fun deleteRideById(rideId: String)
    
    @Query("""
        DELETE FROM rides 
        WHERE driverId = :driverId 
        AND departureTime < :currentTime
        AND isActive = 0
    """)
    suspend fun cleanupOldRides(driverId: String, currentTime: Long = System.currentTimeMillis())
}