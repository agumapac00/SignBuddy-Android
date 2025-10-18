package com.example.signbuddy.ui.components

import android.content.Context
import android.content.pm.PackageManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

// 📳 Haptic Feedback Manager for Enhanced Interactions
class HapticFeedbackManager(private val context: Context) {
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private val hasVibrator = vibrator.hasVibrator()
    private val hasVibratePermission = ContextCompat.checkSelfPermission(
        context, 
        android.Manifest.permission.VIBRATE
    ) == PackageManager.PERMISSION_GRANTED

    fun lightTap() {
        if (!hasVibrator || !hasVibratePermission) return
        
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        } catch (e: SecurityException) {
            // Permission was revoked or vibrator is not available
        }
    }

    fun mediumTap() {
        if (!hasVibrator || !hasVibratePermission) return
        
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(100)
            }
        } catch (e: SecurityException) {
            // Permission was revoked or vibrator is not available
        }
    }

    fun successPattern() {
        if (!hasVibrator || !hasVibratePermission) return
        
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 100, 50, 100)
                val amplitudes = intArrayOf(0, 255, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 100, 50, 100), -1)
            }
        } catch (e: SecurityException) {
            // Permission was revoked or vibrator is not available
        }
    }

    fun errorPattern() {
        if (!hasVibrator || !hasVibratePermission) return
        
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 200, 100, 200)
                val amplitudes = intArrayOf(0, 128, 0, 128)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 200, 100, 200), -1)
            }
        } catch (e: SecurityException) {
            // Permission was revoked or vibrator is not available
        }
    }

    fun achievementPattern() {
        if (!hasVibrator || !hasVibratePermission) return
        
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 50, 50, 50, 50, 50, 50, 50, 50)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0, 255, 0)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 50, 50, 50, 50, 50, 50, 50, 50), -1)
            }
        } catch (e: SecurityException) {
            // Permission was revoked or vibrator is not available
        }
    }
}

@Composable
fun rememberHapticFeedback(): HapticFeedbackManager {
    val context = LocalContext.current
    return remember { HapticFeedbackManager(context) }
}
