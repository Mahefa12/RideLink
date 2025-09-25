package com.app.ridelink.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = White,
    onPrimary = Black,
    secondary = Grey300,
    onSecondary = Black,
    tertiary = Grey400,
    onTertiary = Black,
    background = Black,
    onBackground = White,
    surface = Grey900,
    onSurface = White,
    surfaceVariant = Grey800,
    onSurfaceVariant = Grey300,
    outline = Grey600,
    outlineVariant = Grey700,
    error = White,
    onError = Black,
    errorContainer = Grey800,
    onErrorContainer = White
)

private val LightColorScheme = lightColorScheme(
    primary = Black,
    onPrimary = White,
    secondary = Grey700,
    onSecondary = White,
    tertiary = Grey600,
    onTertiary = White,
    background = White,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    surfaceVariant = Grey100,
    onSurfaceVariant = Grey700,
    outline = Grey400,
    outlineVariant = Grey300,
    error = Black,
    onError = White,
    errorContainer = Grey200,
    onErrorContainer = Black
)

@Composable
fun RideLinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamic color to maintain monochromatic theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Always use our custom monochromatic color schemes
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}