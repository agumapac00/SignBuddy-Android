package com.example.signbuddy.ui.components

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

// 🎵 Sound Effects Manager for Gamification
class SoundEffectsManager(private val context: Context) {
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

    fun playCorrect() {
        // Play a success tone
        try {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 200)
        } catch (e: Exception) {
            // Fallback: no sound if ToneGenerator fails
        }
    }

    fun playWrong() {
        // Play an error tone
        try {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_NACK, 200)
        } catch (e: Exception) {
            // Fallback: no sound if ToneGenerator fails
        }
    }

    fun playButtonClick() {
        // Play a button click tone
        try {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 100)
        } catch (e: Exception) {
            // Fallback: no sound if ToneGenerator fails
        }
    }

    fun playAchievement() {
        // Play an achievement tone
        try {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ANSWER, 300)
        } catch (e: Exception) {
            // Fallback: no sound if ToneGenerator fails
        }
    }

    fun playLevelUp() {
        // Play a level up tone
        try {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 500)
        } catch (e: Exception) {
            // Fallback: no sound if ToneGenerator fails
        }
    }

    fun release() {
        toneGenerator.release()
    }
}

@Composable
fun rememberSoundEffects(): SoundEffectsManager {
    val context = LocalContext.current
    return remember { SoundEffectsManager(context) }
}