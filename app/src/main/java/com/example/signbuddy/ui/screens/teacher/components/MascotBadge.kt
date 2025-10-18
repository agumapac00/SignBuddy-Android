package com.example.signbuddy.ui.screens.teacher.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MascotBadge(label: String) {
    val gradient = Brush.radialGradient(listOf(Color(0xFFFFF9C4), Color(0xFFFFE0B2)))
    Surface(shape = CircleShape, shadowElevation = 2.dp, color = Color.Transparent) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(gradient)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.EmojiEmotions, contentDescription = null, tint = Color(0xFFFFB300))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}







