package com.example.signbuddy.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.signbuddy.R
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(navController: NavController? = null) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var narrationEnabled by remember { mutableStateOf(true) }

    // 🔹 Initialize TTS
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    // 🔹 Letters
    val letters = ('A'..'Z').map { it.toString() }
    var index by remember { mutableStateOf(0) }
    val total = letters.size
    val progress = (index + 1).toFloat() / total.toFloat()

    var unlockedBeginnerBadge by remember { mutableStateOf(false) }

    // 🔹 Map letters to images (sign_a, sign_b … sign_z)
    val letterImages = mapOf(
        "A" to R.drawable.sign_a,
        "B" to R.drawable.sign_b,
        "C" to R.drawable.sign_c,
        "D" to R.drawable.sign_d,
        "E" to R.drawable.sign_e,
        "F" to R.drawable.sign_f,
        "G" to R.drawable.sign_g,
        "H" to R.drawable.sign_h,
        "I" to R.drawable.sign_i,
        "J" to R.drawable.sign_j,
        "K" to R.drawable.sign_k,
        "L" to R.drawable.sign_l,
        "M" to R.drawable.sign_m,
        "N" to R.drawable.sign_n,
        "O" to R.drawable.sign_o,
        "P" to R.drawable.sign_p,
        "Q" to R.drawable.sign_q,
        "R" to R.drawable.sign_r,
        "S" to R.drawable.sign_s,
        "T" to R.drawable.sign_t,
        "U" to R.drawable.sign_u,
        "V" to R.drawable.sign_v,
        "W" to R.drawable.sign_w,
        "X" to R.drawable.sign_x,
        "Y" to R.drawable.sign_y,
        "Z" to R.drawable.sign_z
    )

    // 🔹 Speak current letter
    LaunchedEffect(index, narrationEnabled) {
        if (narrationEnabled) {
            val letter = letters[index]
            tts?.speak("Letter $letter", TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    // 🔹 Animation for image
    val scale = remember { Animatable(1f) }
    LaunchedEffect(index) {
        scale.snapTo(1f)
        scale.animateTo(
            targetValue = 1.1f,
            animationSpec = tween(300, easing = LinearEasing)
        )
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(300, easing = LinearEasing)
        )
    }

    // 🔹 Background
    val gradientBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFF7AE), Color(0xFFFFE5C2), Color(0xFFE1F5FE))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tutorial: A–Z Signs") },
                navigationIcon = {
                    IconButton(onClick = { navController?.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBackground)
                .padding(innerPadding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 🔹 Title
            Text(
                text = "Learn the Alphabet in Sign Language",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 🔹 Full Image with Rounded Corners + Animation
            val currentLetter = letters[index]
            val imageRes = letterImages[currentLetter]

            if (imageRes != null) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = "Sign for $currentLetter",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((350 * scale.value).dp) // Animate size
                        .clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Fit // show full image inside
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 🔹 Progress Bar
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.LightGray
            )
            Text(
                text = "${index + 1} / $total",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 🔹 Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { if (index > 0) index-- },
                    enabled = index > 0
                ) {
                    Text("Previous")
                }

                IconToggleButton(
                    checked = narrationEnabled,
                    onCheckedChange = { narrationEnabled = it }
                ) {
                    Icon(
                        imageVector = if (narrationEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Narration Toggle",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Button(
                    onClick = {
                        if (index < total - 1) {
                            index++
                        } else {
                            unlockedBeginnerBadge = true
                        }
                    }
                ) {
                    Text(if (index < total - 1) "Next" else "Finish")
                }
            }

            // 🔹 Badge Unlock
            // 🔹 Badge Unlock Popup
            // 🔹 Badge Unlock Popup
            // 🔹 Badge Unlock Popup
            if (unlockedBeginnerBadge) {
                AlertDialog(
                    onDismissRequest = { unlockedBeginnerBadge = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                unlockedBeginnerBadge = false
                                // ✅ Go back into StudentDashboard with the username
                                navController?.navigate("studentDashboard/Student") { // replace "Student" with actual username if stored
                                    popUpTo("studentDashboard/{username}") { inclusive = true }
                                    launchSingleTop = true
                                }
                                // ✅ Navigate to Lessons tab
                                navController?.navigate("lessons") {
                                    launchSingleTop = true
                                }
                            }
                        ) {
                            Text("OK")
                        }
                    },
                    title = {
                        Text("🥇 Beginner Badge Unlocked!")
                    },
                    text = {
                        Text("Congrats on completing the tutorial!")
                    }
                )
            }
        }
    }
}
