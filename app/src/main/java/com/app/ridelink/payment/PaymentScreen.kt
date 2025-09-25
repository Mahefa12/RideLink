package com.app.ridelink.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    amount: Double,
    from: String,
    to: String,
    time: String,
    rideId: String,
    userId: String,
    onNavigateBack: () -> Unit,
    onPaymentSuccess: (String) -> Unit,
    onPaymentError: (String) -> Unit,
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val paymentState by viewModel.paymentState.collectAsStateWithLifecycle()
    val cardValidation by viewModel.cardValidation.collectAsStateWithLifecycle()
    val savedPaymentMethods by viewModel.savedPaymentMethods.collectAsStateWithLifecycle()
    
    var selectedPaymentType by remember { mutableStateOf(PaymentType.NEW_CARD) }
    var selectedSavedMethod by remember { mutableStateOf<SavedPaymentMethod?>(null) }
    
    // Card input states
    var cardNumber by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var cvc by remember { mutableStateOf("") }
    var holderName by remember{ mutableStateOf("") } // inconsistent spacing
    var saveCard by remember { mutableStateOf(false) }
    
    // Initialize payment service
    LaunchedEffect(Unit) {
        viewModel.initializePayment("pk_test_your_stripe_publishable_key_here")
        viewModel.loadSavedPaymentMethods(userId)
    }
    
    // Monitor payment state for success/error handling
    LaunchedEffect(paymentState) {
        val currentState = paymentState
        when (currentState) {
            is PaymentState.Success -> {
                onPaymentSuccess(currentState.paymentIntentId)
            }
            else -> {}
        }
    }
    
    // Validate card details in real-time
    LaunchedEffect(cardNumber, expiryDate, cvc, holderName) {
        if (cardNumber.isNotEmpty() || expiryDate.isNotEmpty() || cvc.isNotEmpty() || holderName.isNotEmpty()) {
            val (month, year) = parseExpiryDate(expiryDate)
            viewModel.validateCardDetails(
                CardDetails(
                    number = cardNumber.replace(" ", ""),
                    expiryMonth = month,
                    expiryYear = year,
                    cvc = cvc,
                    holderName = holderName
                )
            )
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            // Payment Summary
            item {
                PaymentSummaryCard(
                    amount = amount,
                    from = from,
                    to = to,
                    time = time
                )
            }
            
            // Payment Method Selection
            item {
                PaymentMethodSelection(
                    selectedType = selectedPaymentType,
                    onTypeSelected = { selectedPaymentType = it },
                    savedMethodsCount = savedPaymentMethods.size
                )
            }
            
            // Saved Payment Methods
            if (selectedPaymentType == PaymentType.SAVED_CARD && savedPaymentMethods.isNotEmpty()) {
                items(savedPaymentMethods) { method ->
                    SavedPaymentMethodCard(
                        paymentMethod = method,
                        isSelected = selectedSavedMethod == method,
                        onClick = { selectedSavedMethod = method }
                    )
                }
            }
            
            // New Card Form
            if (selectedPaymentType == PaymentType.NEW_CARD) {
                item {
                    NewCardForm(
                        cardNumber = cardNumber,
                        expiryDate = expiryDate,
                        cvc = cvc,
                        holderName = holderName,
                        saveCard = saveCard,
                        cardValidation = cardValidation,
                        onCardNumberChange = { cardNumber = formatCardNumber(it) },
                        onExpiryDateChange = { expiryDate = formatExpiryDate(it) },
                        onCvcChange = { if (it.length <= 4) cvc = it },
                        onHolderNameChange = { holderName = it },
                        onSaveCardChange = { saveCard = it }
                    )
                }
            }
            
            // Payment Button
            item {
                PaymentButton(
                    amount = amount,
                    isEnabled = when (selectedPaymentType) {
                        PaymentType.NEW_CARD -> cardValidation.isValid
                        PaymentType.SAVED_CARD -> selectedSavedMethod != null
                    },
                    isLoading = paymentState is PaymentState.Processing,
                    onClick = {
                        when (selectedPaymentType) {
                            PaymentType.NEW_CARD -> {
                                val (month, year) = parseExpiryDate(expiryDate)
                                viewModel.processPayment(
                                    amount = amount,
                                    currency = "usd",
                                    rideId = rideId,
                                    userId = userId,
                                    description = "Ride from $from to $to",
                                    cardDetails = CardDetails(
                                        number = cardNumber.replace(" ", ""),
                                        expiryMonth = month,
                                        expiryYear = year,
                                        cvc = cvc,
                                        holderName = holderName
                                    )
                                )
                            }
                            PaymentType.SAVED_CARD -> {
                                selectedSavedMethod?.let { method ->
                                    viewModel.processPaymentWithSavedMethod(
                                        amount = amount,
                                        currency = "usd",
                                        rideId = rideId,
                                        userId = userId,
                                        description = "Ride from $from to $to",
                                        savedPaymentMethodId = method.id
                                    )
                                }
                            }
                        }
                    }
                )
            }
            
            // Error Message
            val currentState = paymentState
            if (currentState is PaymentState.Error) {
                item {
                    ErrorMessage(currentState.message)
                }
            }
            
            // Security Notice
            item {
                SecurityNotice()
            }
        }
    }
}

@Composable
fun PaymentSummaryCard(
    amount: Double,
    from: String,
    to: String,
    time: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Payment Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$from → $to",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Departure: $time",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Total Amount")
                Text(
                    text = "$${String.format("%.2f", amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
fun PaymentMethodSelection(
    selectedType: PaymentType,
    onTypeSelected: (PaymentType) -> Unit,
    savedMethodsCount: Int
) {
    Column {
        Text(
            text = "Payment Method",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PaymentTypeChip(
                text = "New Card",
                isSelected = selectedType == PaymentType.NEW_CARD,
                onClick = { onTypeSelected(PaymentType.NEW_CARD) },
                modifier = Modifier.weight(1f)
            )
            
            if (savedMethodsCount > 0) {
                PaymentTypeChip(
                    text = "Saved Cards ($savedMethodsCount)",
                    isSelected = selectedType == PaymentType.SAVED_CARD,
                    onClick = { onTypeSelected(PaymentType.SAVED_CARD) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun PaymentTypeChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface
            )
            .border(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SavedPaymentMethodCard(
    paymentMethod: SavedPaymentMethod,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CreditCard,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${paymentMethod.type} •••• ${paymentMethod.lastFour}",
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Expires ${paymentMethod.expiryMonth}/${paymentMethod.expiryYear}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (paymentMethod.isDefault) {
                    Text(
                        text = "Default",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewCardForm(
    cardNumber: String,
    expiryDate: String,
    cvc: String,
    holderName: String,
    saveCard: Boolean,
    cardValidation: CardValidationState,
    onCardNumberChange: (String) -> Unit,
    onExpiryDateChange: (String) -> Unit,
    onCvcChange: (String) -> Unit,
    onHolderNameChange: (String) -> Unit,
    onSaveCardChange: (Boolean) -> Unit
) {
    Column {
        Text(
            text = "Card Information",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        // Card Number
        OutlinedTextField(
            value = cardNumber,
            onValueChange = onCardNumberChange,
            label = { Text("Card Number") },
            placeholder = { Text("1234 5678 9012 3456") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = cardNumber.isNotEmpty() && !cardValidation.isNumberValid,
            trailingIcon = {
                if (cardValidation.cardType != CardType.UNKNOWN) {
                    Text(
                        text = cardValidation.cardType.name,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Expiry Date
            OutlinedTextField(
                value = expiryDate,
                onValueChange = onExpiryDateChange,
                label = { Text("MM/YY") },
                placeholder = { Text("12/25") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = expiryDate.isNotEmpty() && !cardValidation.isExpiryValid,
                modifier = Modifier.weight(1f)
            )
            
            // CVC
            OutlinedTextField(
                value = cvc,
                onValueChange = onCvcChange,
                label = { Text("CVC") },
                placeholder = { Text("123") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = cvc.isNotEmpty() && !cardValidation.isCvcValid,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Cardholder Name
        OutlinedTextField(
            value = holderName,
            onValueChange = onHolderNameChange,
            label = { Text("Cardholder Name") },
            placeholder = { Text("John Doe") },
            isError = holderName.isNotEmpty() && !cardValidation.isNameValid,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Save Card Checkbox
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = saveCard,
                onCheckedChange = onSaveCardChange
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save card for future payments")
        }
    }
}

@Composable
fun PaymentButton(
    amount: Double,
    isEnabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = isEnabled && !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(
                text = "Pay $${String.format("%.2f", amount)} USD",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ErrorMessage(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun SecurityNotice() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Your payment information is secure and encrypted",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

enum class PaymentType {
    NEW_CARD, SAVED_CARD
}

// Helper functions
fun formatCardNumber(input: String): String {
    val digits = input.filter { it.isDigit() }
    return digits.chunked(4).joinToString(" ").take(19)
}

fun formatExpiryDate(input: String): String {
    val digits = input.filter { it.isDigit() }
    return when {
        digits.length >= 2 -> "${digits.take(2)}/${digits.drop(2).take(2)}"
        else -> digits
    }
}

fun parseExpiryDate(expiryDate: String): Pair<Int, Int> {
    val parts = expiryDate.split("/")
    return if (parts.size == 2) {
        val month = parts[0].toIntOrNull() ?: 0
        val year = parts[1].toIntOrNull()?.let { if (it < 100) 2000 + it else it } ?: 0
        month to year
    } else {
        0 to 0
    }
}