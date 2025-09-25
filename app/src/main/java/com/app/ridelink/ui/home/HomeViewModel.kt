package com.app.ridelink.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.ridelink.auth.AuthenticationManager
import com.app.ridelink.data.repository.RideRepository
import com.app.ridelink.data.model.Ride
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val availableRides: List<Ride> = emptyList(),
    val myRides: List<Ride> = emptyList(),
    val errorMessage: String? = null,
    val isRideCreated: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val rideRepository: RideRepository,
    private val authenticationManager: AuthenticationManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        // Observe current user and load data when available
        viewModelScope.launch {
            authenticationManager.currentUser.collect { user ->
                if (user != null) {
                    loadAvailableRides()
                    loadUserRides(user.id)
                }
            }
        }
    }
    
    private fun loadAvailableRides() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val rides = rideRepository.getAvailableRides()
                _uiState.value = _uiState.value.copy(
                    availableRides = rides,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load rides: ${e.message}"
                )
            }
        }
    }

    private fun loadUserRides(userId: String) {
        viewModelScope.launch {
            try {
                val userRides = rideRepository.getRidesByDriver(userId)
                _uiState.value = _uiState.value.copy(myRides = userRides)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to load user rides: ${e.message}"
                )
            }
        }
    }
    
    fun createRide(
        from: String,
        to: String,
        time: String,
        price: String,
        seats: Int,
        description: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val currentUser = authenticationManager.currentUser.value
                if (currentUser == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Please log in to create a ride"
                    )
                    return@launch
                }
                
                // Convert price string to double
                val pricePerSeat = try {
                    price.replace("R", "").replace(",", "").trim().toDouble()
                } catch (e: NumberFormatException) {
                    Log.e("RideLink", "HomeViewModel: Invalid price format: $price")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Invalid price format"
                    )
                    return@launch
                }
                
                // Parse departure time (for now, use current date + parsed time)
                val departureTime = try {
                    parseTimeToTimestamp(time)
                } catch (e: Exception) {
                    Log.e("RideLink", "HomeViewModel: Invalid time format: $time")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Invalid time format. Please use format like '2:30 PM'"
                    )
                    return@launch
                }
                
                // Extract destination state (simplified - use province from address)
                val destinationState = extractStateFromAddress(to)
                
                val result = rideRepository.createRide(
                    driverId = currentUser.id,
                    title = "$from to $to",
                    description = description,
                    originAddress = from,
                    destinationAddress = to,
                    destinationState = destinationState,
                    departureTime = departureTime,
                    availableSeats = seats,
                    pricePerSeat = pricePerSeat
                )
                
                if (result.isSuccess) {
                    Log.d("RideLink", "HomeViewModel: Successfully created ride with ID: ${result.getOrNull()}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRideCreated = true,
                        errorMessage = null
                    )
                    // Reload rides to show the new ride
                    loadAvailableRides()
                    loadUserRides(currentUser.id)
                } else {
                    val error = result.exceptionOrNull()
                    Log.e("RideLink", "HomeViewModel: Failed to create ride: ${error?.message}", error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to create ride: ${error?.message}"
                    )
                }
            } catch (e: Exception) {
                Log.e("RideLink", "HomeViewModel: Unexpected error creating ride: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Unexpected error: ${e.message}"
                )
            }
        }
    }
    
    fun clearRideCreatedFlag() {
        _uiState.value = _uiState.value.copy(isRideCreated = false)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    private fun parseTimeToTimestamp(timeString: String): Long {
        // Parse time like "2:30 PM" and combine with today's date
        val calendar = Calendar.getInstance()
        
        // Simple time parsing - you might want to make this more robust
        val timePattern = Regex("(\\d{1,2}):(\\d{2})\\s*(AM|PM)", RegexOption.IGNORE_CASE)
        val matchResult = timePattern.find(timeString.trim())
        
        if (matchResult != null) {
            val (hourStr, minuteStr, amPm) = matchResult.destructured
            var hour = hourStr.toInt()
            val minute = minuteStr.toInt()
            
            // Convert to 24-hour format
            if (amPm.uppercase() == "PM" && hour != 12) {
                hour += 12
            } else if (amPm.uppercase() == "AM" && hour == 12) {
                hour = 0
            }
            
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            
            // If the time is in the past today, set it for tomorrow
            if (calendar.timeInMillis < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            return calendar.timeInMillis
        } else {
            throw IllegalArgumentException("Invalid time format: $timeString")
        }
    }
    
    private fun extractStateFromAddress(address: String): String {
        // Simple state extraction - you might want to make this more sophisticated
        val southAfricanProvinces = listOf(
            "Western Cape", "Eastern Cape", "Northern Cape", "Free State",
            "KwaZulu-Natal", "North West", "Gauteng", "Mpumalanga", "Limpopo"
        )
        
        for (province in southAfricanProvinces) {
            if (address.contains(province, ignoreCase = true)) {
                return province
            }
        }
        
        // Default fallback based on common cities
        return when {
            address.contains("Cape Town", ignoreCase = true) || 
            address.contains("Stellenbosch", ignoreCase = true) -> "Western Cape"
            address.contains("Johannesburg", ignoreCase = true) || 
            address.contains("Pretoria", ignoreCase = true) || 
            address.contains("Sandton", ignoreCase = true) -> "Gauteng"
            address.contains("Durban", ignoreCase = true) -> "KwaZulu-Natal"
            else -> "Western Cape" // Default
        }
    }
}