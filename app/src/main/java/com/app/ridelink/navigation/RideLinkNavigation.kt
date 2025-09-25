package com.app.ridelink.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.app.ridelink.ui.auth.LoginScreen
import com.app.ridelink.ui.auth.RegisterScreen
import com.app.ridelink.ui.settings.UserSettingsScreen
import com.app.ridelink.ui.profile.ProfileScreen
import com.app.ridelink.ui.home.HomeScreen
import com.app.ridelink.ui.home.RideRequest
import com.app.ridelink.ui.main.MainScreen
import com.app.ridelink.ui.map.MapScreen
import com.app.ridelink.ui.messaging.ChatScreen
import com.app.ridelink.ui.messaging.ConversationsListScreen
import com.app.ridelink.ui.ride.RideDetailScreen
import com.app.ridelink.ui.booking.BookingScreen
import com.app.ridelink.ui.booking.BookingConfirmationScreen
import com.app.ridelink.ui.booking.BookingDetails
import com.app.ridelink.ui.booking.PaymentMethod
import com.app.ridelink.ui.booking.PaymentType
import com.app.ridelink.payment.PaymentScreen
import com.app.ridelink.ui.search.SearchScreen
import com.app.ridelink.ui.messages.MessagesScreen
import java.time.LocalDate
import java.time.LocalTime

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Main : Screen("main")
    object Home : Screen("home")
    object Settings : Screen("settings")
    object Profile : Screen("profile")
    object Search : Screen("search")
    object Messages : Screen("messages")
    object Map : Screen("map")
    object ConversationsList : Screen("conversations")
    object Chat : Screen("chat/{conversationId}") {
        fun createRoute(conversationId: String) = "chat/$conversationId"
    }
    object RideDetail : Screen("ride_detail")
    object Booking : Screen("booking")
    object Payment : Screen("payment")
    object BookingConfirmation : Screen("booking_confirmation")
}

@Composable
fun RideLinkNavigation(
    navController: NavHostController,
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegistrationSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                onNavigateToRideDetail = { ride ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("ride", ride)
                    navController.navigate(Screen.RideDetail.route)
                },
                onNavigateToMap = {
                    navController.navigate(Screen.Map.route)
                },
                onNavigateToChat = { conversationId ->
                    navController.navigate(Screen.Chat.createRoute(conversationId))
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToMap = {
                    navController.navigate(Screen.Map.route)
                },
                onNavigateToMessages = {
                    navController.navigate(Screen.ConversationsList.route)
                },
                onNavigateToRideDetail = { ride ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("ride", ride)
                    navController.navigate(Screen.RideDetail.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            UserSettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onNavigateToRideDetail = { ride ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("ride", ride)
                    navController.navigate(Screen.RideDetail.route)
                }
            )
        }

        composable(Screen.Messages.route) {
            MessagesScreen(
                onNavigateToChat = { conversationId ->
                    navController.navigate(Screen.Chat.createRoute(conversationId))
                }
            )
        }

        composable(Screen.Map.route) {
            MapScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLocationSelected = { location ->
                    // Navigate back with selected location
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ConversationsList.route) {
            ConversationsListScreen(
                onNavigateToChat = { conversationId ->
                    navController.navigate(Screen.Chat.createRoute(conversationId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Chat.route) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            ChatScreen(
                conversationId = conversationId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.RideDetail.route) {
            val ride = navController.previousBackStackEntry?.savedStateHandle?.get<RideRequest>("ride")
            ride?.let {
                RideDetailScreen(
                    ride = it,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onBookRide = {
                        navController.currentBackStackEntry?.savedStateHandle?.set("ride", it)
                        navController.navigate(Screen.Booking.route)
                    }
                )
            }
        }

        composable(Screen.Booking.route) {
            val ride = navController.previousBackStackEntry?.savedStateHandle?.get<RideRequest>("ride")
            ride?.let {
                BookingScreen(
                    rideRequest = it,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onBookingComplete = { bookingId ->
                        val bookingDetails = BookingDetails(
                            bookingId = bookingId,
                            rideRequest = it,
                            selectedDate = LocalDate.now(),
                            selectedTime = LocalTime.now(),
                            selectedSeats = 1,
                            paymentMethod = PaymentMethod("1", "Visa ending in 1234", PaymentType.CREDIT_CARD, "1234"),
                            totalAmount = it.price * 1.1
                        )
                        navController.currentBackStackEntry?.savedStateHandle?.set("bookingDetails", bookingDetails)
                        navController.navigate(Screen.BookingConfirmation.route) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onNavigateToPayment = { amount, from, to, time, rideId ->
                        navController.currentBackStackEntry?.savedStateHandle?.set("paymentAmount", amount)
                        navController.currentBackStackEntry?.savedStateHandle?.set("paymentFrom", from)
                        navController.currentBackStackEntry?.savedStateHandle?.set("paymentTo", to)
                        navController.currentBackStackEntry?.savedStateHandle?.set("paymentTime", time)
                        navController.currentBackStackEntry?.savedStateHandle?.set("paymentRideId", rideId)
                        navController.navigate(Screen.Payment.route)
                    }
                )
            }
        }

        composable(Screen.Payment.route) {
            val amount = navController.previousBackStackEntry?.savedStateHandle?.get<Double>("paymentAmount") ?: 0.0
            val from = navController.previousBackStackEntry?.savedStateHandle?.get<String>("paymentFrom") ?: ""
            val to = navController.previousBackStackEntry?.savedStateHandle?.get<String>("paymentTo") ?: ""
            val time = navController.previousBackStackEntry?.savedStateHandle?.get<String>("paymentTime") ?: ""
            val rideId = navController.previousBackStackEntry?.savedStateHandle?.get<String>("paymentRideId") ?: ""
            
            PaymentScreen(
                amount = amount,
                from = from,
                to = to,
                time = time,
                rideId = rideId,
                userId = "user_123",
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPaymentSuccess = { paymentMethodId ->
                    // Navigate back to booking with payment method
                    navController.popBackStack()
                },
                onPaymentError = { error ->
                    // Payment error handling
                }
            )
        }

        composable(Screen.BookingConfirmation.route) {
            val bookingDetails = navController.previousBackStackEntry?.savedStateHandle?.get<BookingDetails>("bookingDetails")
            bookingDetails?.let {
                BookingConfirmationScreen(
                    bookingDetails = it,
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onViewBookingDetails = {
                        // Navigate to booking details
                    },
                    onShareBooking = {
                        // Share booking functionality
                    }
                )
            }
        }
    }
}