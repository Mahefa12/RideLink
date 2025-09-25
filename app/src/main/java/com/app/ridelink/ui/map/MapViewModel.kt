package com.app.ridelink.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.ridelink.location.LocationManager
import org.osmdroid.util.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationManager: LocationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        val hasPermission = locationManager.hasLocationPermission()
        _uiState.value = _uiState.value.copy(hasLocationPermission = hasPermission)
        
        if (hasPermission) {
            getCurrentLocation()
        }
    }

    fun onPermissionGranted() {
        _uiState.value = _uiState.value.copy(
            hasLocationPermission = true,
            error = null
        )
        getCurrentLocation()
    }

    fun getCurrentLocation() {
        if (!_uiState.value.hasLocationPermission) {
            _uiState.value = _uiState.value.copy(
                error = "Location permission not granted"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        viewModelScope.launch {
            locationManager.getCurrentLocation()
                .onSuccess { location ->
                    _uiState.value = _uiState.value.copy(
                        currentLocation = location,
                        isLoading = false,
                        error = null
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to get current location"
                    )
                }
        }
    }

    fun setSelectedLocation(location: GeoPoint) {
        _uiState.value = _uiState.value.copy(
            selectedLocation = location,
            error = null
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun calculateDistanceToSelected(): Float? {
        val currentLoc = _uiState.value.currentLocation
        val selectedLoc = _uiState.value.selectedLocation
        
        return if (currentLoc != null && selectedLoc != null) {
            locationManager.calculateDistance(
                currentLoc.latitude, currentLoc.longitude,
                selectedLoc.latitude, selectedLoc.longitude
            )
        } else {
            null
        }
    }
}