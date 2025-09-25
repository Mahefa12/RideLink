package com.app.ridelink

import android.app.Application
import com.app.ridelink.auth.AuthenticationManager
import com.app.ridelink.data.database.DatabaseSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

@HiltAndroidApp
class RideLinkApp : Application() {
    
    @Inject
    lateinit var databaseSeeder: DatabaseSeeder
    
    @Inject
    lateinit var authenticationManager: AuthenticationManager
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onCreate() {
        super.onCreate()
        Log.d("RideLink", "RideLinkApp: onCreate() called")
        
        // Seed database with sample data and auto-login for prototype
        applicationScope.launch {
            try {
                Log.d("RideLink", "RideLinkApp: Starting database seeding...")
                databaseSeeder.seedDatabase()
                Log.d("RideLink", "RideLinkApp: Database seeding completed")
                // Auto-login as the seeded current user for testing
                Log.d("RideLink", "RideLinkApp: Starting auto-login...")
                authenticationManager.bypassLogin()
                Log.d("RideLink", "RideLinkApp: Auto-login completed")
            } catch (e: Exception) {
                Log.e("RideLink", "RideLinkApp: Error during initialization: ${e.message}", e)
            }
        }
    }
}