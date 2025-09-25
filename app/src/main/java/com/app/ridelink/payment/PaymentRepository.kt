package com.app.ridelink.payment

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import javax.inject.Inject
import javax.inject.Singleton

// API interface for payment endpoints
interface PaymentApi {
    @POST("payments/create-intent")
    suspend fun createPaymentIntent(
        @Body request: CreatePaymentIntentRequest
    ): Response<CreatePaymentIntentResponse>
    
    @POST("payments/confirm")
    suspend fun confirmPayment(
        @Body request: ConfirmPaymentRequest
    ): Response<ConfirmPaymentResponse>
    
    @POST("payments/save-method")
    suspend fun savePaymentMethod(
        @Body request: SavePaymentMethodRequest
    ): Response<SavePaymentMethodResponse>
}

@Singleton
class PaymentRepository @Inject constructor(
    private val paymentApi: PaymentApi
) {
    
    suspend fun createPaymentIntent(
        amount: Double,
        currency: String,
        rideId: String,
        userId: String,
        description: String
    ): Result<CreatePaymentIntentResponse> {
        return try {
            val request = CreatePaymentIntentRequest(
                amount = (amount * 100).toInt(), // Convert to cents
                currency = currency,
                rideId = rideId,
                userId = userId,
                description = description
            )
            
            val response = paymentApi.createPaymentIntent(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to create payment intent: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun confirmPayment(
        paymentIntentId: String,
        paymentMethodId: String,
        rideId: String
    ): Result<ConfirmPaymentResponse> {
        return try {
            val request = ConfirmPaymentRequest(
                paymentIntentId = paymentIntentId,
                paymentMethodId = paymentMethodId,
                rideId = rideId
            )
            
            val response = paymentApi.confirmPayment(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to confirm payment: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun savePaymentMethod(
        userId: String,
        paymentMethodId: String,
        isDefault: Boolean = false
    ): Result<SavePaymentMethodResponse> {
        return try {
            val request = SavePaymentMethodRequest(
                userId = userId,
                paymentMethodId = paymentMethodId,
                isDefault = isDefault
            )
            
            val response = paymentApi.savePaymentMethod(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to save payment method: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Request/Response data classes
data class CreatePaymentIntentRequest(
    val amount: Int, // Amount in cents
    val currency: String,
    val rideId: String,
    val userId: String,
    val description: String
)

data class CreatePaymentIntentResponse(
    val clientSecret: String,
    val paymentIntentId: String,
    val amount: Int,
    val currency: String
)

data class ConfirmPaymentRequest(
    val paymentIntentId: String,
    val paymentMethodId: String,
    val rideId: String
)

data class ConfirmPaymentResponse(
    val success: Boolean,
    val paymentIntentId: String,
    val status: String,
    val bookingId: String?
)

data class SavePaymentMethodRequest(
    val userId: String,
    val paymentMethodId: String,
    val isDefault: Boolean
)

data class SavePaymentMethodResponse(
    val success: Boolean,
    val paymentMethodId: String
)