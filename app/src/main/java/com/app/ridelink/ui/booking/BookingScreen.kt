package com.app.ridelink.ui.booking

import android.os.Parcelable
import androidx.compose.foundation.background
import kotlinx.parcelize.Parcelize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
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

data class BookingState(
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedTime: LocalTime? = null,
    val selectedSeats: Int = 1,
    val selectedPaymentMethod: PaymentMethod? = null,
    val currentStep: BookingStep = BookingStep.DATE_TIME,
    val isLoading: Boolean = false
)

enum class BookingStep {
    DATE_TIME,
    PAYMENT,
    CONFIRMATION
}

@Parcelize
data class PaymentMethod(
    val id: String,
    val name: String,
    val type: PaymentType,
    val lastFour: String? = null
) : Parcelable

enum class PaymentType {
    CREDIT_CARD,
    DEBIT_CARD,
    PAYPAL,
    APPLE_PAY,
    GOOGLE_PAY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    rideRequest: RideRequest,
    onNavigateBack: () -> Unit,
    onBookingComplete: (String) -> Unit,
    onNavigateToPayment: (Double, String, String, String, String) -> Unit = { _, _, _, _, _ -> }
) {
    var bookingState by remember { mutableStateOf(BookingState()) }
    
    val availableTimes = remember {
        listOf(
            LocalTime.of(6, 0),
            LocalTime.of(7, 30),
            LocalTime.of(9, 0),
            LocalTime.of(10, 30),
            LocalTime.of(12, 0),
            LocalTime.of(14, 0),
            LocalTime.of(16, 30),
            LocalTime.of(18, 0),
            LocalTime.of(19, 30)
        )
    }
    
    val paymentMethods = remember {
        listOf(
            PaymentMethod("1", "Visa ending in 1234", PaymentType.CREDIT_CARD, "1234"),
            PaymentMethod("2", "Mastercard ending in 5678", PaymentType.CREDIT_CARD, "5678"),
            PaymentMethod("3", "PayPal", PaymentType.PAYPAL),
            PaymentMethod("4", "Apple Pay", PaymentType.APPLE_PAY),
            PaymentMethod("5", "Google Pay", PaymentType.GOOGLE_PAY)
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (bookingState.currentStep) {
                            BookingStep.DATE_TIME -> "Select Date & Time"
                            BookingStep.PAYMENT -> "Payment Method"
                            BookingStep.CONFIRMATION -> "Confirm Booking"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (bookingState.currentStep == BookingStep.DATE_TIME) {
                            onNavigateBack()
                        } else {
                            bookingState = bookingState.copy(
                                currentStep = when (bookingState.currentStep) {
                                    BookingStep.PAYMENT -> BookingStep.DATE_TIME
                                    BookingStep.CONFIRMATION -> BookingStep.PAYMENT
                                    else -> BookingStep.DATE_TIME
                                }
                            )
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            BookingBottomBar(
                bookingState = bookingState,
                rideRequest = rideRequest,
                onNextStep = {
                    when (bookingState.currentStep) {
                        BookingStep.DATE_TIME -> {
                            if (bookingState.selectedTime != null) {
                                bookingState = bookingState.copy(currentStep = BookingStep.PAYMENT)
                            }
                        }
                        BookingStep.PAYMENT -> {
                            if (bookingState.selectedPaymentMethod != null) {
                                bookingState = bookingState.copy(currentStep = BookingStep.CONFIRMATION)
                            }
                        }
                        BookingStep.CONFIRMATION -> {
                            bookingState = bookingState.copy(isLoading = true)
                            // Simulate booking process
                            onBookingComplete("BK${System.currentTimeMillis()}")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                RideInfoCard(rideRequest)
            }
            
            when (bookingState.currentStep) {
                BookingStep.DATE_TIME -> {
                    item {
                        DateTimeSelectionSection(
                            selectedDate = bookingState.selectedDate,
                            selectedTime = bookingState.selectedTime,
                            selectedSeats = bookingState.selectedSeats,
                            availableTimes = availableTimes,
                            maxSeats = rideRequest.availableSeats,
                            onDateSelected = { date ->
                                bookingState = bookingState.copy(selectedDate = date)
                            },
                            onTimeSelected = { time ->
                                bookingState = bookingState.copy(selectedTime = time)
                            },
                            onSeatsChanged = { seats ->
                                bookingState = bookingState.copy(selectedSeats = seats)
                            }
                        )
                    }
                }
                BookingStep.PAYMENT -> {
                    item {
                        PaymentMethodSection(
                            paymentMethods = paymentMethods,
                            selectedPaymentMethod = bookingState.selectedPaymentMethod,
                            onPaymentMethodSelected = { method ->
                                bookingState = bookingState.copy(selectedPaymentMethod = method)
                            },
                            onAddNewPaymentMethod = {
                                val subtotal = rideRequest.price
                                val total = subtotal * 1.1 // Including fees
                                onNavigateToPayment(
                                    total,
                                    rideRequest.from,
                                    rideRequest.to,
                                    rideRequest.time,
                                    rideRequest.id
                                )
                            }
                        )
                    }
                }
                BookingStep.CONFIRMATION -> {
                    item {
                        BookingConfirmationSection(
                            bookingState = bookingState,
                            rideRequest = rideRequest
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RideInfoCard(rideRequest: RideRequest) {
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
                text = "${rideRequest.from} → ${rideRequest.to}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Driver: ${rideRequest.driverName}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${rideRequest.price}/seat",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun DateTimeSelectionSection(
    selectedDate: LocalDate,
    selectedTime: LocalTime?,
    selectedSeats: Int,
    availableTimes: List<LocalTime>,
    maxSeats: Int,
    onDateSelected: (LocalDate) -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
    onSeatsChanged: (Int) -> Unit
) {
    Column {
        // Date Selection
        Text(
            text = "Select Date",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(7) { index ->
                val date = LocalDate.now().plusDays(index.toLong())
                DateChip(
                    date = date,
                    isSelected = date == selectedDate,
                    onClick = { onDateSelected(date) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Time Selection
        Text(
            text = "Select Time",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(availableTimes) { time ->
                TimeChip(
                    time = time,
                    isSelected = time == selectedTime,
                    onClick = { onTimeSelected(time) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Seat Selection
        Text(
            text = "Number of Seats",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        SeatSelector(
            selectedSeats = selectedSeats,
            maxSeats = maxSeats,
            onSeatsChanged = onSeatsChanged
        )
    }
}

@Composable
fun DateChip(
    date: LocalDate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    Card(
        modifier = Modifier
            .clickable { onClick() }
            .width(80.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = date.format(DateTimeFormatter.ofPattern("MMM")),
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = date.format(DateTimeFormatter.ofPattern("EEE")),
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )
        }
    }
}

@Composable
fun TimeChip(
    time: LocalTime,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    Card(
        modifier = Modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
    ) {
        Text(
            text = time.format(DateTimeFormatter.ofPattern("HH:mm")),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )
    }
}

@Composable
fun SeatSelector(
    selectedSeats: Int,
    maxSeats: Int,
    onSeatsChanged: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconButton(
            onClick = { if (selectedSeats > 1) onSeatsChanged(selectedSeats - 1) },
            enabled = selectedSeats > 1
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Decrease seats",
                modifier = Modifier.size(20.dp)
            )
        }
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedSeats.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        IconButton(
            onClick = { if (selectedSeats < maxSeats) onSeatsChanged(selectedSeats + 1) },
            enabled = selectedSeats < maxSeats
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Increase seats",
                modifier = Modifier.size(20.dp)
            )
        }
        
        Text(
            text = "Max: $maxSeats",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PaymentMethodSection(
    paymentMethods: List<PaymentMethod>,
    selectedPaymentMethod: PaymentMethod?,
    onPaymentMethodSelected: (PaymentMethod) -> Unit,
    onAddNewPaymentMethod: () -> Unit = {}
) {
    Column {
        Text(
            text = "Choose Payment Method",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        paymentMethods.forEach { method ->
            PaymentMethodCard(
                paymentMethod = method,
                isSelected = method == selectedPaymentMethod,
                onClick = { onPaymentMethodSelected(method) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onAddNewPaymentMethod,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add New Payment Method")
        }
    }
}

@Composable
fun PaymentMethodCard(
    paymentMethod: PaymentMethod,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = paymentMethod.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = paymentMethod.type.name.replace("_", " "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun BookingConfirmationSection(
    bookingState: BookingState,
    rideRequest: RideRequest
) {
    Column {
        Text(
            text = "Booking Summary",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                ConfirmationRow("Route", "${rideRequest.from} → ${rideRequest.to}")
                ConfirmationRow("Date", bookingState.selectedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")))
                ConfirmationRow("Time", bookingState.selectedTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "")
                ConfirmationRow("Seats", bookingState.selectedSeats.toString())
                ConfirmationRow("Driver", rideRequest.driverName)
                ConfirmationRow("Payment", bookingState.selectedPaymentMethod?.name ?: "")
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                val subtotal = rideRequest.price * bookingState.selectedSeats
                val serviceFee = subtotal * 0.1
                val total = subtotal + serviceFee
                
                ConfirmationRow("Subtotal", "$${String.format("%.2f", subtotal)}")
                ConfirmationRow("Service Fee", "$${String.format("%.2f", serviceFee)}")
                ConfirmationRow(
                    "Total", 
                    "$${String.format("%.2f", total)}",
                    isTotal = true
                )
            }
        }
    }
}

@Composable
fun ConfirmationRow(
    label: String,
    value: String,
    isTotal: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (isTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            style = if (isTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = if (isTotal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun BookingBottomBar(
    bookingState: BookingState,
    rideRequest: RideRequest,
    onNextStep: () -> Unit
) {
    val total = (rideRequest.price * bookingState.selectedSeats) * 1.1 // Including service fee
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$${String.format("%.2f", total)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Button(
                onClick = onNextStep,
                enabled = when (bookingState.currentStep) {
                    BookingStep.DATE_TIME -> bookingState.selectedTime != null
                    BookingStep.PAYMENT -> bookingState.selectedPaymentMethod != null
                    BookingStep.CONFIRMATION -> !bookingState.isLoading
                },
                modifier = Modifier.height(48.dp)
            ) {
                if (bookingState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = when (bookingState.currentStep) {
                            BookingStep.DATE_TIME -> "Continue"
                            BookingStep.PAYMENT -> "Continue"
                            BookingStep.CONFIRMATION -> "Confirm Booking"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}