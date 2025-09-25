package com.app.ridelink.ui.map

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.ridelink.location.LocationData
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

data class MapUiState(
    val currentLocation: LocationData? = null,
    val selectedLocation: GeoPoint? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasLocationPermission: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateBack: () -> Unit,
    onLocationSelected: (GeoPoint) -> Unit = {},
    initialLocation: GeoPoint? = null,
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            viewModel.onPermissionGranted()
        }
    }

    // Request permissions on first composition
    LaunchedEffect(Unit) {
        if (!uiState.hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Set initial location if provided
    LaunchedEffect(initialLocation) {
        initialLocation?.let {
            viewModel.setSelectedLocation(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Location") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.hasLocationPermission) {
                        IconButton(
                            onClick = { viewModel.getCurrentLocation() }
                        ) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = "My Location"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            uiState.selectedLocation?.let { location ->
                ExtendedFloatingActionButton(
                    onClick = { onLocationSelected(location) },
                    icon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                    },
                    text = { Text("Confirm Location") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.hasLocationPermission) {
                MapContent(
                    uiState = uiState,
                    onMapClick = { latLng ->
                        viewModel.setSelectedLocation(latLng)
                    }
                )
            } else {
                // Permission denied state
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.Center),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Location Permission Required",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please grant location permission to use the map feature.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }
            }

            // Loading indicator
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Error message
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
fun MapContent(
    uiState: MapUiState,
    onMapClick: (GeoPoint) -> Unit
) {
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    
    // Default location (Cape Town) if no current location
    val defaultLocation = GeoPoint(-33.9249, 18.4241)
    val mapLocation = uiState.currentLocation?.let {
        GeoPoint(it.latitude, it.longitude)
    } ?: defaultLocation

    DisposableEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        onDispose { }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(15.0)
                controller.setCenter(mapLocation)
                mapView = this
                
                // Set up map click listener
                setOnTouchListener { _, event ->
                    if (event.action == android.view.MotionEvent.ACTION_UP) {
                        val projection = this.projection
                        val geoPoint = projection.fromPixels(event.x.toInt(), event.y.toInt()) as GeoPoint
                        onMapClick(geoPoint)
                    }
                    false
                }
            }
        },
        update = { map ->
            map.overlays.clear()
            
            // Current location marker
            uiState.currentLocation?.let { location ->
                val marker = Marker(map)
                marker.position = GeoPoint(location.latitude, location.longitude)
                marker.title = "Current Location"
                marker.snippet = "You are here"
                map.overlays.add(marker)
            }

            // Selected location marker
            uiState.selectedLocation?.let { location ->
                val marker = Marker(map)
                marker.position = location
                marker.title = "Selected Location"
                marker.snippet = "Tap confirm to select this location"
                map.overlays.add(marker)
            }
            
            map.invalidate()
        }
    )
    
    // Update camera when current location changes
    LaunchedEffect(uiState.currentLocation) {
        uiState.currentLocation?.let { location ->
            val newPosition = GeoPoint(location.latitude, location.longitude)
            mapView?.controller?.animateTo(newPosition)
        }
    }
}