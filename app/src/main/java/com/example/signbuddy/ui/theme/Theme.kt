package com.example.signbuddy.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 🎨 Light Color Palette - Bright, playful, and engaging for kindergarteners
private val LightColors = lightColorScheme(
    primary = Color(0xFF6C63FF),     // Vibrant Purple - fun and engaging
    onPrimary = Color.White,
    secondary = Color(0xFFFF6B6B),   // Bright Coral - warm and friendly
    onSecondary = Color.White,
    tertiary = Color(0xFF4ECDC4),    // Teal - calming and success
    onTertiary = Color.White,
    error = Color(0xFFFF5722),       // Bright Red - clear error indication
    onError = Color.White,
    background = Color(0xFFFFF8E1),  // Warm cream - cozy and inviting
    onBackground = Color(0xFF2D3436), // Dark gray - good contrast
    surface = Color(0xFFFFFFFF),     // Pure white
    onSurface = Color(0xFF2D3436),   // Dark gray
    surfaceVariant = Color(0xFFE8F5E8), // Light green - success hint
    onSurfaceVariant = Color(0xFF2D3436), // Dark gray
    outline = Color(0xFFDDD6FE),     // Light purple
    outlineVariant = Color(0xFFF3F4F6), // Very light gray
)

// 🌙 Dark Color Palette - Playful and engaging for kindergarteners
private val DarkColors = darkColorScheme(
    primary = Color(0xFF8B7FFF),     // Bright Purple
    onPrimary = Color.Black,
    secondary = Color(0xFFFF8A80),   // Bright Coral
    onSecondary = Color.Black,
    tertiary = Color(0xFF80CBC4),    // Light Teal
    onTertiary = Color.Black,
    error = Color(0xFFFFAB91),       // Light Red
    onError = Color.Black,
    background = Color(0xFF1A1A1A),  // Dark background
    onBackground = Color(0xFFE8E8E8), // Light gray
    surface = Color(0xFF2D2D2D),
    onSurface = Color(0xFFE8E8E8),   // Light gray
    surfaceVariant = Color(0xFF3A3A3A), // Dark gray
    onSurfaceVariant = Color(0xFFB8E6B8), // Light green
    outline = Color(0xFF6A5ACD),     // Medium purple
    outlineVariant = Color(0xFF4A4A4A), // Dark gray
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
