package com.app.ridelink.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.ridelink.ui.home.RideRequest
import com.app.ridelink.ui.home.RideCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateToRideDetail: (RideRequest) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var fromLocation by remember { mutableStateOf("") }
    var toLocation by remember { mutableStateOf("") }
    
    // Hardcoded sample rides for search
    val sampleRides = remember {
        listOf(
            RideRequest(
                id = "search1",
                from = "Downtown",
                to = "Airport",
                date = "2024-01-15",
                time = "14:30",
                price = 25.0,
                availableSeats = 3,
                driverName = "Sarah Johnson",
                driverRating = 4.8,
                carModel = "Toyota Camry",
                estimatedDuration = "45 min"
            ),
            RideRequest(
                id = "search2",
                from = "University",
                to = "Mall",
                date = "2024-01-15",
                time = "16:00",
                price = 15.0,
                availableSeats = 2,
                driverName = "Mike Chen",
                driverRating = 4.9,
                carModel = "Honda Civic",
                estimatedDuration = "25 min"
            ),
            RideRequest(
                id = "search3",
                from = "City Center",
                to = "Beach",
                date = "2024-01-16",
                time = "10:00",
                price = 30.0,
                availableSeats = 4,
                driverName = "Emma Wilson",
                driverRating = 4.7,
                carModel = "Nissan Altima",
                estimatedDuration = "1 hour"
            )
        )
    }
    
    val filteredRides = sampleRides.filter { ride ->
        (fromLocation.isEmpty() || ride.from.contains(fromLocation, ignoreCase = true)) &&
        (toLocation.isEmpty() || ride.to.contains(toLocation, ignoreCase = true))
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Search for Rides",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Search filters
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                OutlinedTextField(
                    value = fromLocation,
                    onValueChange = { fromLocation = it },
                    label = { Text("From") },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = toLocation,
                    onValueChange = { toLocation = it },
                    label = { Text("To") },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { /* Handle filter */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Filters")
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Button(
                        onClick = { /* Handle search */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Search")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Search results
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Available Rides",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${filteredRides.size} rides found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (filteredRides.isEmpty()) {
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
                        text = "Try adjusting your search criteria",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredRides) { ride ->
                    RideCard(
                        ride = ride,
                        onJoinRide = { /* Handle join ride */ },
                        isMyRide = false,
                        onClick = { onNavigateToRideDetail(ride) }
                    )
                }
            }
        }
    }
}