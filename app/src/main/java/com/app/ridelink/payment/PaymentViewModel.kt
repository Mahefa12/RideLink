package com.app.ridelink.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentService: PaymentService,
    private val paymentRepository: PaymentRepository
) : ViewModel() {
    
    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()
    
    private val _cardValidation = MutableStateFlow(CardValidationState())
    val cardValidation: StateFlow<CardValidationState> = _cardValidation.asStateFlow()
    
    private val _savedPaymentMethods = MutableStateFlow<List<SavedPaymentMethod>>(emptyList())
    val savedPaymentMethods: StateFlow<List<SavedPaymentMethod>> = _savedPaymentMethods.asStateFlow()
    
    // Initialize payment service with Stripe publishable key
    fun initializePayment(publishableKey: String) {
        paymentService.initialize(publishableKey)
    }
    
    // Process payment with card details
    fun processPayment(
        amount: Double,
        currency: String,
        rideId: String,
        userId: String,
        description: String,
        cardDetails: CardDetails
    ) {
        viewModelScope.launch {
            try {
                _paymentState.value = PaymentState.Processing
                
                // Step 1: Create payment intent on backend
                val paymentIntentResult = paymentRepository.createPaymentIntent(
                    amount = amount,
                    currency = currency,
                    rideId = rideId,
                    userId = userId,
                    description = description
                )
                
                if (paymentIntentResult.isFailure) {
                    _paymentState.value = PaymentState.Error(
                        paymentIntentResult.exceptionOrNull()?.message ?: "Failed to create payment intent"
                    )
                    return@launch
                }
                
                val paymentIntentResponse = paymentIntentResult.getOrThrow()
                
                // Step 2: Create payment method with card details
                val paymentMethodResult = paymentService.createPaymentMethod(
                    cardNumber = cardDetails.number,
                    expiryMonth = cardDetails.expiryMonth,
                    expiryYear = cardDetails.expiryYear,
                    cvc = cardDetails.cvc,
                    holderName = cardDetails.holderName
                )
                
                if (paymentMethodResult.isFailure) {
                    _paymentState.value = PaymentState.Error(
                        paymentMethodResult.exceptionOrNull()?.message ?: "Failed to create payment method"
                    )
                    return@launch
                }
                
                val paymentMethod = paymentMethodResult.getOrThrow()
                
                // Confirm payment with Stripe
                val confirmResult = paymentService.confirmPayment(
                    paymentIntentClientSecret = paymentIntentResponse.clientSecret,
                    paymentMethodId = paymentMethod.id!!
                )
                
                if (confirmResult.isFailure) {
                    _paymentState.value = PaymentState.Error(
                        confirmResult.exceptionOrNull()?.message ?: "Payment confirmation failed"
                    )
                    return@launch
                }
                
                // Finalize payment on backend
                val backendConfirmResult = paymentRepository.confirmPayment(
                    paymentIntentId = paymentIntentResponse.paymentIntentId,
                    paymentMethodId = paymentMethod.id!!,
                    rideId = rideId
                )
                
                if (backendConfirmResult.isFailure) {
                    _paymentState.value = PaymentState.Error(
                        backendConfirmResult.exceptionOrNull()?.message ?: "Backend confirmation failed"
                    )
                    return@launch
                }
                
                _paymentState.value = PaymentState.Success(paymentIntentResponse.paymentIntentId)
                
            } catch (e: Exception) {
                _paymentState.value = PaymentState.Error(e.message ?: "Payment processing failed")
            }
        }
    }
    
    // Process payment with saved payment method
    fun processPaymentWithSavedMethod(
        amount: Double,
        currency: String,
        rideId: String,
        userId: String,
        description: String,
        savedPaymentMethodId: String
    ) {
        viewModelScope.launch {
            try {
                _paymentState.value = PaymentState.Processing
                
                // Create payment intent
                val paymentIntentResult = paymentRepository.createPaymentIntent(
                    amount = amount,
                    currency = currency,
                    rideId = rideId,
                    userId = userId,
                    description = description
                )
                
                if (paymentIntentResult.isFailure) {
                    _paymentState.value = PaymentState.Error(
                        paymentIntentResult.exceptionOrNull()?.message ?: "Failed to create payment intent"
                    )
                    return@launch
                }
                
                val paymentIntentResponse = paymentIntentResult.getOrThrow()
                
                // Confirm payment with saved method
                val confirmResult = paymentService.confirmPayment(
                    paymentIntentClientSecret = paymentIntentResponse.clientSecret,
                    paymentMethodId = savedPaymentMethodId
                )
                
                if (confirmResult.isFailure) {
                    _paymentState.value = PaymentState.Error(
                        confirmResult.exceptionOrNull()?.message ?: "Payment confirmation failed"
                    )
                    return@launch
                }
                
                // Confirm on backend
                val backendConfirmResult = paymentRepository.confirmPayment(
                    paymentIntentId = paymentIntentResponse.paymentIntentId,
                    paymentMethodId = savedPaymentMethodId,
                    rideId = rideId
                )
                
                if (backendConfirmResult.isFailure) {
                    _paymentState.value = PaymentState.Error(
                        backendConfirmResult.exceptionOrNull()?.message ?: "Backend confirmation failed"
                    )
                    return@launch
                }
                
                _paymentState.value = PaymentState.Success(paymentIntentResponse.paymentIntentId)
                
            } catch (e: Exception) {
                _paymentState.value = PaymentState.Error(e.message ?: "Payment processing failed")
            }
        }
    }
    
    // Validate card details in real-time
    fun validateCardDetails(cardDetails: CardDetails) {
        val isNumberValid = paymentService.validateCardNumber(cardDetails.number)
        val isExpiryValid = paymentService.validateExpiryDate(cardDetails.expiryMonth, cardDetails.expiryYear)
        val isCvcValid = paymentService.validateCvc(cardDetails.cvc)
        val isNameValid = cardDetails.holderName.isNotBlank()
        
        val cardType = paymentService.getCardType(cardDetails.number)
        
        _cardValidation.value = CardValidationState(
            isNumberValid = isNumberValid,
            isExpiryValid = isExpiryValid,
            isCvcValid = isCvcValid,
            isNameValid = isNameValid,
            cardType = cardType,
            isValid = isNumberValid && isExpiryValid && isCvcValid && isNameValid
        )
    }
    
    // Save payment method for future use
    fun savePaymentMethod(
        userId: String,
        paymentMethodId: String,
        isDefault: Boolean = false
    ) {
        viewModelScope.launch {
            paymentRepository.savePaymentMethod(userId, paymentMethodId, isDefault)
        }
    }
    
    // Reset payment state
    fun resetPaymentState() {
        _paymentState.value = PaymentState.Idle
    }
    
    // Load saved payment methods (mock implementation)
    fun loadSavedPaymentMethods(userId: String) {
        // In a real app, this would fetch from backend
        _savedPaymentMethods.value = listOf(
            SavedPaymentMethod(
                id = "pm_1",
                type = "Visa",
                lastFour = "4242",
                expiryMonth = 12,
                expiryYear = 2025,
                isDefault = true
            ),
            SavedPaymentMethod(
                id = "pm_2",
                type = "Mastercard",
                lastFour = "5555",
                expiryMonth = 8,
                expiryYear = 2026,
                isDefault = false
            )
        )
    }
}

data class CardDetails(
    val number: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val cvc: String,
    val holderName: String
)

data class CardValidationState(
    val isNumberValid: Boolean = false,
    val isExpiryValid: Boolean = false,
    val isCvcValid: Boolean = false,
    val isNameValid: Boolean = false,
    val cardType: CardType = CardType.UNKNOWN,
    val isValid: Boolean = false
)

data class SavedPaymentMethod(
    val id: String,
    val type: String,
    val lastFour: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val isDefault: Boolean
)