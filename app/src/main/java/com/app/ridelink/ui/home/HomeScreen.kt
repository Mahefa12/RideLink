package com.app.ridelink.ui.home

import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Search

import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.parcelize.Parcelize
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// Ride data model for UI display
@Parcelize
data class RideRequest(
    val id: String,
    val from: String,
    val to: String,
    val date: String,
    val time: String,
    val price: Double,
    val availableSeats: Int,
    val driverName: String,
    val driverRating: Double,
    val carModel: String,
    val estimatedDuration: String,
    val fromLat: Double = 0.0,
    val fromLng: Double = 0.0,
    val toLat: Double = 0.0,
    val toLng: Double = 0.0
) : Parcelable

// Search filter parameters for ride filtering
data class SearchFilters(
    val origin: String = "",
    val destination: String = "",
    val date: String = "",
    val vehicleType: String = "All",
    val maxPrice: Int = 1000,
    val minRating: Float = 0f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onLogout: () -> Unit = {},
    onNavigateToMap: () -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
    onNavigateToRideDetail: (RideRequest) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // UI state management
    var showRideRequestDialog by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) }
    var showFilters by remember { mutableStateOf(false) }
    var searchFilters by remember { mutableStateOf(SearchFilters()) }
    var filteredRides by remember { mutableStateOf(emptyList<RideRequest>()) }
    
    // Reset dialog state when ride is successfully created
    LaunchedEffect(uiState.isRideCreated) {
        if (uiState.isRideCreated) {
            showRideRequestDialog = false
            viewModel.clearRideCreatedFlag()
        }
    }
    
    // Clear error messages automatically
    uiState.errorMessage?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            viewModel.clearError()
        }
    }
    
    // Transform database rides to UI display format
    val availableRides = uiState.availableRides.map { ride ->
        RideRequest(
            id = ride.id,
            from = ride.originAddress,
            to = ride.destinationAddress,
            date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(ride.departureTime)),
            time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(ride.departureTime)),
            price = ride.pricePerSeat,
            availableSeats = ride.availableSeats,
            driverName = "Driver", 
            driverRating = 4.5, 
            carModel = ride.vehicleModel ?: ride.vehicleType,
            estimatedDuration = "N/A",
            fromLat = ride.originLatitude ?: 0.0,
            fromLng = ride.originLongitude ?: 0.0,
            toLat = ride.destinationLatitude ?: 0.0,
            toLng = ride.destinationLongitude ?: 0.0
        )
    }
    
    val myRides = uiState.myRides.map { ride ->
        RideRequest(
            id = ride.id,
            from = ride.originAddress,
            to = ride.destinationAddress,
            date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(ride.departureTime)),
            time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(ride.departureTime)),
            price = ride.pricePerSeat,
            availableSeats = ride.availableSeats,
            driverName = "You",
            driverRating = 4.5,
            carModel = ride.vehicleModel ?: ride.vehicleType,
            estimatedDuration = "N/A",
            fromLat = ride.originLatitude ?: 0.0,
            fromLng = ride.originLongitude ?: 0.0,
            toLat = ride.destinationLatitude ?: 0.0,
            toLng = ride.destinationLongitude ?: 0.0
        )
    }

    // Filter rides based on search criteria
    LaunchedEffect(searchFilters, availableRides) {
        filteredRides = availableRides.filter { ride ->
            (searchFilters.origin.isEmpty() || ride.from.contains(searchFilters.origin, ignoreCase = true)) &&
            (searchFilters.destination.isEmpty() || ride.to.contains(searchFilters.destination, ignoreCase = true)) &&
            (searchFilters.vehicleType == "All" || ride.carModel.contains(searchFilters.vehicleType, ignoreCase = true)) &&
            ride.price <= searchFilters.maxPrice &&
            ride.driverRating >= searchFilters.minRating
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RideLink") },
                actions = {
                    IconButton(onClick = onNavigateToMessages) {
                        Icon(Icons.Default.Email, contentDescription = "Messages")
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showRideRequestDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Ride")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = activeTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Find Rides") }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("My Rides") }
                )
            }
            
            // Content based on selected tab
            when (activeTab) {
                0 -> {
                    // Find Rides Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            // Search Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Search Rides",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(onClick = { showFilters = !showFilters }) {
                                            Icon(Icons.Default.Settings, contentDescription = "Filters")
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Origin and Destination
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = searchFilters.origin,
                                            onValueChange = { searchFilters = searchFilters.copy(origin = it) },
                                            label = { Text("From") },
                                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = searchFilters.destination,
                                            onValueChange = { searchFilters = searchFilters.copy(destination = it) },
                                            label = { Text("To") },
                                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Date and Search Button
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = searchFilters.date,
                                            onValueChange = { searchFilters = searchFilters.copy(date = it) },
                                            label = { Text("Date") },
                                            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            placeholder = { Text("Today") }
                                        )
                                        Button(
                                            onClick = { /* Search action */ },
                                            modifier = Modifier.height(56.dp)
                                        ) {
                                            Icon(Icons.Default.Search, contentDescription = "Search")
                                        }
                                    }
                                    
                                    // Filters Section
                                    if (showFilters) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Divider()
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        Text(
                                            text = "Filters",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        // Vehicle Type Filter
                                        Text("Vehicle Type", style = MaterialTheme.typography.bodyMedium)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf("All", "Car", "SUV", "Van").forEach { type ->
                                                FilterChip(
                                                    onClick = { searchFilters = searchFilters.copy(vehicleType = type) },
                                                    label = { Text(type) },
                                                    selected = searchFilters.vehicleType == type
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        // Price Range
                                        Text("Max Price: R${searchFilters.maxPrice}", style = MaterialTheme.typography.bodyMedium)
                                        Slider(
                                            value = searchFilters.maxPrice.toFloat(),
                                            onValueChange = { searchFilters = searchFilters.copy(maxPrice = it.toInt()) },
                                            valueRange = 100f..1000f,
                                            steps = 17
                                        )
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        // Rating Filter
                                        Text("Min Rating: ${searchFilters.minRating}", style = MaterialTheme.typography.bodyMedium)
                                        Slider(
                                            value = searchFilters.minRating,
                                            onValueChange = { searchFilters = searchFilters.copy(minRating = it) },
                                            valueRange = 0f..5f,
                                            steps = 9
                                        )
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        // Clear Filters Button
                                        OutlinedButton(
                                            onClick = { searchFilters = SearchFilters() },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Clear, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Clear Filters")
                                        }
                                    }
                                }
                            }
                        }
                        
                        item {
                            // Quick Actions Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Quick Actions",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = onNavigateToMap,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                Icons.Default.LocationOn,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Set Location")
                                        }
                                        
                                        Button(
                                            onClick = { showRideRequestDialog = true },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Create Ride")
                                        }
                                    }
                                }
                            }
                        }
                        
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Nearby Rides Map",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${filteredRides.size} rides found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        item {
                            // Map View
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(400.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    // Map background with grid pattern
                                    Column(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        repeat(8) { row ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f)
                                            ) {
                                                repeat(6) { col ->
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .fillMaxHeight()
                                                            .background(
                                                                if ((row + col) % 2 == 0) 
                                                                    MaterialTheme.colorScheme.surfaceVariant 
                                                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                                            )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Ride markers positioned on the map
                                    filteredRides.forEachIndexed { index, ride ->
                                        val xOffset = (index * 60 + 40) % 300
                                        val yOffset = (index * 80 + 50) % 320
                                        
                                        Card(
                                            modifier = Modifier
                                                .offset(
                                                    x = xOffset.dp,
                                                    y = yOffset.dp
                                                )
                                                .size(60.dp),
                                            shape = RoundedCornerShape(30.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            ),
                                            onClick = { onNavigateToRideDetail(ride) }
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(4.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.LocationOn,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "R${ride.price.toInt()}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                    
                                    // Map legend
                                    Card(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.LocationOn,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Available Rides",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Ride details list below map
                        item {
                            Text(
                                text = "Ride Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }
                        
                        if (filteredRides.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "No rides found",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Try adjusting your search filters",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(filteredRides.take(3)) { ride ->
                                RideCard(
                                    ride = ride,
                                    onJoinRide = { onNavigateToRideDetail(ride) },
                                    isMyRide = false,
                                    onClick = { onNavigateToRideDetail(ride) }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // My Rides Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "My Rides",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        
                        items(myRides) { ride ->
                            RideCard(
                                ride = ride,
                                onJoinRide = { onNavigateToRideDetail(ride) },
                                isMyRide = true,
                                onClick = { onNavigateToRideDetail(ride) }
                            )
                        }
                        
                        if (myRides.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "No rides yet",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Create your first ride by tapping the + button",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Ride Request Dialog
    if (showRideRequestDialog) {
        RideRequestDialog(
            onDismiss = { showRideRequestDialog = false },
            onCreateRide = { from, to, time, price, seats ->
                viewModel.createRide(from, to, time, price, seats)
            },
            isLoading = uiState.isLoading,
            errorMessage = uiState.errorMessage
        )
    }
}

@Composable
fun RideCard(
    ride: RideRequest,
    onJoinRide: () -> Unit,
    isMyRide: Boolean,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${ride.from} → ${ride.to}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = ride.driverName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "${ride.time} • ${ride.availableSeats} seats available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Vehicle Type Badge
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = ride.carModel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        
                        // Rating
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = ride.driverRating.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "R${ride.price.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = onJoinRide,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = if (isMyRide) "Edit" else "Join",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RideRequestDialog(
    onDismiss: () -> Unit,
    onCreateRide: (String, String, String, String, Int) -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var seats by remember { mutableStateOf("1") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Ride") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Display error message
                errorMessage?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                
                OutlinedTextField(
                    value = from,
                    onValueChange = { from = it },
                    label = { Text("From") },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = to,
                    onValueChange = { to = it },
                    label = { Text("To") },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Departure Time") },
                    placeholder = { Text("e.g., 2:30 PM") },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price per seat") },
                    placeholder = { Text("e.g., R315") },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = seats,
                    onValueChange = { seats = it },
                    label = { Text("Available seats") },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Loading state
                if (isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Creating ride...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val seatCount = seats.toIntOrNull() ?: 1
                    onCreateRide(from, to, time, price, seatCount)
                },
                enabled = !isLoading && from.isNotBlank() && to.isNotBlank() && time.isNotBlank() && price.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Create Ride")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}