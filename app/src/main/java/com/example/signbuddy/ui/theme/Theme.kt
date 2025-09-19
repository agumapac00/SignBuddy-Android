package com.example.signbuddy.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 🎨 Light Color Palette
private val LightColors = lightColorScheme(
    primary = Color(0xFF42A5F5),     // Blue
    onPrimary = Color.White,
    secondary = Color(0xFFFFA726),   // Orange
    onSecondary = Color.White,
    tertiary = Color(0xFF66BB6A),    // Green
    onTertiary = Color.White,
    background = Color(0xFFFDFDFD),  // Light gray-white
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
)

// 🌙 Dark Color Palette
private val DarkColors = darkColorScheme(
    primary = Color(0xFF90CAF9),     // Light Blue
    onPrimary = Color.Black,
    secondary = Color(0xFFFFCC80),   // Light Orange
    onSecondary = Color.Black,
    tertiary = Color(0xFFA5D6A7),    // Light Green
    onTertiary = Color.Black,
    background = Color(0xFF121212),  // Dark background
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
)

@Composable
fun SignBuddyTheme(
    darkTheme: Boolean = false, // change to isSystemInDarkTheme() if you want auto switch
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
