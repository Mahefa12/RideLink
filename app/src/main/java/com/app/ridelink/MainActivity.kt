package com.app.ridelink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.app.ridelink.auth.AuthState
import com.app.ridelink.auth.AuthenticationManager
import com.app.ridelink.navigation.RideLinkNavigation
import com.app.ridelink.navigation.Screen
import com.app.ridelink.ui.theme.RideLinkTheme
import com.app.ridelink.ui.theme.ThemeManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var authenticationManager: AuthenticationManager
    
    @Inject
    lateinit var themeManager: ThemeManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDarkTheme by themeManager.isDarkTheme.collectAsState(initial = false)
            val isSystemTheme by themeManager.isSystemTheme.collectAsState(initial = true)
            
            val shouldUseDarkTheme = if (isSystemTheme) {
                isSystemInDarkTheme()
            } else {
                isDarkTheme
            }
            
            RideLinkTheme(darkTheme = shouldUseDarkTheme) {
                val authState by authenticationManager.authState.collectAsState()
                val navController = rememberNavController()
                
                when (authState) {
                    AuthState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is AuthState.Authenticated -> {
                        RideLinkNavigation(
                            navController = navController,
                            startDestination = Screen.Main.route
                        )
                    }
                    AuthState.Unauthenticated -> {
                        RideLinkNavigation(
                            navController = navController,
                            startDestination = Screen.Login.route
                        )
                    }
                    is AuthState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}
