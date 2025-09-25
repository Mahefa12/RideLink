package com.app.ridelink.ui.booking

import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import kotlinx.parcelize.Parcelize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.ridelink.ui.home.RideRequest
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Parcelize
data class BookingDetails(
    val bookingId: String,
    val rideRequest: RideRequest,
    val selectedDate: LocalDate,
    val selectedTime: LocalTime,
    val selectedSeats: Int,
    val paymentMethod: PaymentMethod,
    val totalAmount: Double,
    val status: BookingStatus = BookingStatus.CONFIRMED
) : Parcelable

enum class BookingStatus {
    CONFIRMED,
    PENDING,
    CANCELLED,
    COMPLETED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingConfirmationScreen(
    bookingDetails: BookingDetails,
    onNavigateToHome: () -> Unit,
    onViewBookingDetails: () -> Unit,
    onShareBooking: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Booking Confirmed",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            BookingConfirmationBottomBar(
                onNavigateToHome = onNavigateToHome,
                onViewBookingDetails = onViewBookingDetails,
                onShareBooking = onShareBooking
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                SuccessIndicator()
            }
            
            item {
                BookingIdCard(bookingDetails.bookingId)
            }
            
            item {
                BookingDetailsCard(bookingDetails)
            }
            
            item {
                BookingStatusCard(bookingDetails.status)
            }
            
            item {
                ImportantNotesCard()
            }
        }
    }
}

@Composable
fun SuccessIndicator() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Success",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Booking Confirmed!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Your ride has been successfully booked",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BookingIdCard(bookingId: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Booking ID",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = bookingId,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Save this ID for future reference",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun BookingDetailsCard(bookingDetails: BookingDetails) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Trip Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Route
            DetailRow(
                icon = Icons.Default.DateRange,
                label = "Route",
                value = "${bookingDetails.rideRequest.from} → ${bookingDetails.rideRequest.to}"
            )
            
            // Date & Time
            DetailRow(
                icon = Icons.Default.DateRange,
                label = "Date & Time",
                value = "${bookingDetails.selectedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))} at ${bookingDetails.selectedTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
            )
            
            // Seats
            DetailRow(
                icon = Icons.Default.Person,
                label = "Seats",
                value = "${bookingDetails.selectedSeats} seat${if (bookingDetails.selectedSeats > 1) "s" else ""}"
            )
            
            // Driver
            DetailRow(
                icon = Icons.Default.Person,
                label = "Driver",
                value = bookingDetails.rideRequest.driverName
            )
            
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            
            // Payment Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = bookingDetails.paymentMethod.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total Amount",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$${String.format("%.2f", bookingDetails.totalAmount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun BookingStatusCard(status: BookingStatus) {
    val statusColor = when (status) {
        BookingStatus.CONFIRMED -> MaterialTheme.colorScheme.primary
        BookingStatus.PENDING -> MaterialTheme.colorScheme.tertiary
        BookingStatus.CANCELLED -> MaterialTheme.colorScheme.error
        BookingStatus.COMPLETED -> MaterialTheme.colorScheme.primary
    }
    
    val statusText = when (status) {
        BookingStatus.CONFIRMED -> "Confirmed"
        BookingStatus.PENDING -> "Pending"
        BookingStatus.CANCELLED -> "Cancelled"
        BookingStatus.COMPLETED -> "Completed"
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, statusColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
fun ImportantNotesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Important Notes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val notes = listOf(
                "Please arrive at the pickup location 5 minutes early",
                "Contact your driver if you're running late",
                "Cancellation is free up to 2 hours before departure",
                "Keep your booking ID handy for reference",
                "Rate your experience after the trip"
            )
            
            notes.forEach { note ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BookingConfirmationBottomBar(
    onNavigateToHome: () -> Unit,
    onViewBookingDetails: () -> Unit,
    onShareBooking: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Button(
                onClick = onNavigateToHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Back to Home",
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewBookingDetails,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("View Details")
                }
                
                OutlinedButton(
                    onClick = onShareBooking,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share")
                }
            }
        }
    }
}