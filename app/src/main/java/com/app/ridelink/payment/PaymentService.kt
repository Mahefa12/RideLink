package com.app.ridelink.payment

import android.content.Context
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams

import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.coroutines.resume

@Singleton
class PaymentService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private lateinit var stripe: Stripe
    
    // Initialize Stripe with publishable key
    fun initialize(publishableKey: String) {
        PaymentConfiguration.init(context, publishableKey)
        stripe = Stripe(context, publishableKey)
    }
    
    // Create payment method from card details
    suspend fun createPaymentMethod(
        cardNumber: String,
        expiryMonth: Int,
        expiryYear: Int,
        cvc: String,
        holderName: String
    ): Result<PaymentMethod> {
        return try {
            val cardParams = PaymentMethodCreateParams.Card(
                number = cardNumber,
                expiryMonth = expiryMonth,
                expiryYear = expiryYear,
                cvc = cvc
            )
            
            val billingDetails = PaymentMethod.BillingDetails(
                name = holderName
            )
            
            val paymentMethodParams = PaymentMethodCreateParams.create(
                card = cardParams,
                billingDetails = billingDetails
            )
            
            val paymentMethod = stripe.createPaymentMethodSynchronous(paymentMethodParams)
            if (paymentMethod != null) {
                Result.success(paymentMethod)
            } else {
                Result.failure(Exception("Failed to create payment method"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Confirm payment with payment intent
    suspend fun confirmPayment(
        paymentIntentClientSecret: String,
        paymentMethodId: String
    ): Result<PaymentIntent> {
        return try {
            val confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodId(
                paymentMethodId = paymentMethodId,
                clientSecret = paymentIntentClientSecret
            )
            
            val paymentIntent = stripe.confirmPaymentIntentSynchronous(confirmParams)
             if (paymentIntent != null) {
                 Result.success(paymentIntent)
             } else {
                 Result.failure(Exception("Failed to confirm payment"))
             }
         } catch (e: Exception) {
             Result.failure(e)
         }
    }
    
    // Validate card number using Luhn algorithm
    fun validateCardNumber(cardNumber: String): Boolean {
        val cleanNumber = cardNumber.replace("\\s".toRegex(), "")
        if (cleanNumber.length < 13 || cleanNumber.length > 19) return false
        
        var sum = 0
        var alternate = false
        
        for (i in cleanNumber.length - 1 downTo 0) {
            var n = cleanNumber[i].toString().toInt()
            if (alternate) {
                n *= 2
                if (n > 9) n = (n % 10) + 1
            }
            sum += n
            alternate = !alternate
        }
        
        return sum % 10 == 0
    }
    
    // Validate expiry date
    fun validateExpiryDate(month: Int, year: Int): Boolean {
        if (month < 1 || month > 12) return false
        
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
        
        return when {
            year < currentYear -> false
            year == currentYear -> month >= currentMonth
            else -> true
        }
    }
    
    // Validate CVC
    fun validateCvc(cvc: String): Boolean {
        return cvc.length in 3..4 && cvc.all { it.isDigit() }
    }
    
    // Get card type from number
    fun getCardType(cardNumber: String): CardType {
        val cleanNumber = cardNumber.replace("\\s".toRegex(), "")
        return when {
            cleanNumber.startsWith("4") -> CardType.VISA
            cleanNumber.startsWith("5") || cleanNumber.startsWith("2") -> CardType.MASTERCARD
            cleanNumber.startsWith("3") -> CardType.AMEX
            cleanNumber.startsWith("6") -> CardType.DISCOVER
            else -> CardType.UNKNOWN
        }
    }
}

enum class CardType {
    VISA, MASTERCARD, AMEX, DISCOVER, UNKNOWN
}

data class PaymentRequest(
    val amount: Double,
    val currency: String = "usd",
    val description: String,
    val rideId: String,
    val userId: String
)

data class PaymentResponse(
    val success: Boolean,
    val paymentIntentId: String?,
    val error: String?
)

sealed class PaymentState {
    object Idle : PaymentState()
    object Processing : PaymentState()
    data class Success(val paymentIntentId: String) : PaymentState()
    data class Error(val message: String) : PaymentState()
}