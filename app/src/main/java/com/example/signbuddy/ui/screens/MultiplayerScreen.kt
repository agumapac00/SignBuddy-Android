package com.example.signbuddy.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.DashPathEffect
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.signbuddy.ml.ASLModelHelper
import com.example.signbuddy.ui.components.*
import com.example.signbuddy.viewmodels.MultiplayerViewModel
import com.example.signbuddy.viewmodels.MultiplayerGameState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.signbuddy.services.ProgressTrackingService
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

// Data classes for multiplayer game state
data class Player(
    val id: String,
    val name: String,
    val score: Int = 0,
    val isConnected: Boolean = false,
    val currentAnswer: String = "",
    val isCorrect: Boolean = false,
    val responseTime: Long = 0L,
    val totalCorrectAnswers: Int = 0,
    val averageResponseTime: Long = 0L,
    val isLocal: Boolean = false
)

// Global room management
object RoomManager {
    private val activeRooms = mutableMapOf<String, Room>()
    
    data class Room(
        val code: String,
        val hostId: String,
        val joinerId: String? = null,
        val isActive: Boolean = true
    )
    
    fun createRoom(code: String, hostId: String): Room {
        val room = Room(code, hostId)
        activeRooms[code] = room
        return room
    }

    fun joinRoom(code: String, joinerId: String): Room? {
        val room = activeRooms[code]
        return if (room != null && room.joinerId == null) {
            val updatedRoom = room.copy(joinerId = joinerId)
            activeRooms[code] = updatedRoom

            // ✅ Add this log line here
            Log.d("RoomManager", "Player joined room: $code (joinerId=$joinerId)")

            updatedRoom
        } else null
    }


    fun getRoom(code: String): Room? = activeRooms[code]
    
    fun removeRoom(code: String) {
        activeRooms.remove(code)
    }
}

data class GameQuestion(
    val id: String,
    val type: QuestionType,
    val content: String,
    val answer: String,
    val timeLimit: Int = 10
)

enum class QuestionType {
    LETTER, WORD
}

// Network communication data classes
data class NetworkMessage(
    val type: MessageType,
    val playerId: String,
    val data: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageType {
    PLAYER_JOINED,
    PLAYER_LEFT,
    ANSWER_SUBMITTED,
    GAME_START,
    GAME_END,
    QUESTION_CHANGE,
    HEARTBEAT
}

// Camera and model state
data class CameraState(
    val isPermissionGranted: Boolean = false,
    val isCameraInitialized: Boolean = false,
    val isAnalyzing: Boolean = false,
    val lastDetectedSign: String = "",
    val confidence: Float = 0f
)

// Connection and matching state
data class ConnectionState(
    val isHost: Boolean = false,
    val roomCode: String = "",
    val isConnected: Boolean = false,
    val isWaitingForOpponent: Boolean = false,
    val opponentFound: Boolean = false
)

enum class GameState {
    LOBBY, CONNECTING, COUNTDOWN, PLAYING, RESULTS, FINISHED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplayerScreen(navController: NavController? = null, multiplayerViewModel: MultiplayerViewModel? = null, username: String = "") {
    // Kindergarten-friendly colors
    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFB6C1), // Light pink
            Color(0xFFFFD1DC), // Pale pink
            Color(0xFFFFF0F5)  // Lavender blush
        )
    )
    
    val soundEffects = rememberSoundEffects()
    val hapticFeedback = rememberHapticFeedback()
    
    // ViewModel state
    val multiplayerGameState by multiplayerViewModel?.gameState?.collectAsState() ?: remember { mutableStateOf(MultiplayerGameState()) }
    val isLoading by multiplayerViewModel?.isLoading?.collectAsState() ?: remember { mutableStateOf(false) }
    val errorMessage by multiplayerViewModel?.errorMessage?.collectAsState() ?: remember { mutableStateOf(null) }
    
    // Game state management
    var gameState by remember { mutableStateOf(GameState.LOBBY) }
    var currentQuestion by remember { mutableStateOf<GameQuestion?>(null) }
    var timeLeft by remember { mutableStateOf(0) }
    var questionIndex by remember { mutableStateOf(0) }
    var selectedQuestionType by remember { mutableStateOf(QuestionType.LETTER) }
    
    // Player management
    var localPlayer by remember { mutableStateOf(Player("local", "You", 0, isLocal = true)) }
    var opponentPlayer by remember { mutableStateOf(Player("opponent", "Opponent", 0, isLocal = false)) }
    var streak by remember { mutableStateOf(0) }
    
    // Word progress tracking (for word quiz type)
    var localWordProgress by remember { mutableStateOf("") } // e.g., "C", "CA", "CAT"
    var opponentWordProgress by remember { mutableStateOf("") }
    
    // Last letter feedback (for word quiz type)
    var lastSignedLetter by remember { mutableStateOf("") } // Last letter signed
    var lastLetterWasCorrect by remember { mutableStateOf(false) } // Was last letter correct?
    
    // Camera and model state
    var cameraState by remember { mutableStateOf(CameraState()) }
    var connectionState by remember { mutableStateOf(ConnectionState()) }
    
    // Room code input
    var roomCodeInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Progress tracking
    val progressTrackingService = remember { ProgressTrackingService() }
    var sessionStartTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var lettersCompleted by remember { mutableStateOf(0) }
    var perfectSigns by remember { mutableStateOf(0) }
    var mistakes by remember { mutableStateOf(0) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var progressUpdate by remember { mutableStateOf<ProgressTrackingService.ProgressUpdate?>(null) }
    var hasShownExitSummary by remember { mutableStateOf(false) }
    var hasShownResultsOnce by remember { mutableStateOf(false) }
    var isExitHandled by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Sync ViewModel state with local state
    LaunchedEffect(multiplayerGameState) {
        // Update connection state
        connectionState = connectionState.copy(
            isHost = multiplayerGameState.isHost,
            roomCode = multiplayerGameState.roomCode,
            isConnected = multiplayerGameState.isConnected,
            isWaitingForOpponent = multiplayerGameState.isWaitingForOpponent,
            opponentFound = multiplayerGameState.opponentFound
        )
        
        // Update player info with force synchronization
        Log.d("MultiplayerScreen", "=== FORCE SYNCING SCORES FROM VIEWMODEL ===")
        Log.d("MultiplayerScreen", "Local score from ViewModel: ${multiplayerGameState.localPlayerScore}")
        Log.d("MultiplayerScreen", "Opponent score from ViewModel: ${multiplayerGameState.opponentPlayerScore}")
        
        val oldLocalScore = localPlayer.score
        val oldOpponentScore = opponentPlayer.score
        
        localPlayer = localPlayer.copy(
            name = multiplayerGameState.localPlayerName,
            score = multiplayerGameState.localPlayerScore,
            isConnected = multiplayerGameState.isConnected
        )
        
        opponentPlayer = opponentPlayer.copy(
            name = multiplayerGameState.opponentPlayerName,
            score = multiplayerGameState.opponentPlayerScore,
            isConnected = multiplayerGameState.opponentFound,
            currentAnswer = multiplayerGameState.opponentCurrentAnswer,
            isCorrect = multiplayerGameState.opponentIsCorrect
        )
        
        // Update opponent word progress if it's a word question
        if (currentQuestion?.type == QuestionType.WORD && multiplayerGameState.opponentCurrentAnswer.isNotEmpty()) {
            opponentWordProgress = multiplayerGameState.opponentCurrentAnswer
        } else if (currentQuestion?.type != QuestionType.WORD) {
            opponentWordProgress = ""
        }
        
        Log.d("MultiplayerScreen", "=== FORCE SCORE SYNC COMPLETE ===")
        Log.d("MultiplayerScreen", "Local score: $oldLocalScore -> ${localPlayer.score}")
        Log.d("MultiplayerScreen", "Opponent score: $oldOpponentScore -> ${opponentPlayer.score}")
        Log.d("MultiplayerScreen", "Both devices should now show same scores!")
        
        // Update game state - both players should have the same game state
        when {
            multiplayerGameState.gameFinished -> gameState = GameState.FINISHED
            multiplayerGameState.gameStarted -> gameState = GameState.PLAYING
            multiplayerGameState.opponentFound && !multiplayerGameState.gameStarted -> {
                // Both players should go to countdown when opponent is found
                gameState = GameState.COUNTDOWN
            }
            multiplayerGameState.isConnected && multiplayerGameState.isWaitingForOpponent -> gameState = GameState.CONNECTING
            else -> gameState = GameState.LOBBY
        }
    }
    
    // Periodic score synchronization to ensure both devices stay in sync
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000) // Sync every 2 seconds
            if (multiplayerGameState.isConnected && multiplayerGameState.opponentFound) {
                Log.d("MultiplayerScreen", "=== PERIODIC SCORE SYNC ===")
                Log.d("MultiplayerScreen", "Local score from ViewModel: ${multiplayerGameState.localPlayerScore}")
                Log.d("MultiplayerScreen", "Opponent score from ViewModel: ${multiplayerGameState.opponentPlayerScore}")
                
                // Force update local player score from ViewModel
                val oldLocalScore = localPlayer.score
                localPlayer = localPlayer.copy(
                    score = multiplayerGameState.localPlayerScore
                )
                
                // Force update opponent player score from ViewModel
                val oldOpponentScore = opponentPlayer.score
                opponentPlayer = opponentPlayer.copy(
                    score = multiplayerGameState.opponentPlayerScore
                )
                
                if (oldLocalScore != localPlayer.score || oldOpponentScore != opponentPlayer.score) {
                    Log.d("MultiplayerScreen", "Scores updated during periodic sync:")
                    Log.d("MultiplayerScreen", "Local: $oldLocalScore -> ${localPlayer.score}")
                    Log.d("MultiplayerScreen", "Opponent: $oldOpponentScore -> ${opponentPlayer.score}")
                }
            }
        }
    }
    
    // Handle error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            multiplayerViewModel?.clearError()
        }
    }
    
    // Listen to messages when connected
    LaunchedEffect(multiplayerGameState.roomCode) {
        if (multiplayerGameState.roomCode.isNotEmpty()) {
            multiplayerViewModel?.listenToMessages(multiplayerGameState.roomCode)
        }
    }
    
    // Camera & practice states
    var useFrontCamera by remember { mutableStateOf(true) }
    
    // Model and analyzer (copied from evaluation screen)
    var modelInterpreter by remember { mutableStateOf<Interpreter?>(null) }
    var handSignAnalyzer by remember { mutableStateOf<MultiplayerHandSignAnalyzer?>(null) }
    
    // Reset one-time flags when entering screen
    LaunchedEffect(Unit) {
        hasShownExitSummary = false
        hasShownResultsOnce = false
        isExitHandled = false
        showProgressDialog = false
        progressUpdate = null
    }
    
    // Ensure both players have the same question type
    LaunchedEffect(multiplayerGameState.roomCode) {
        if (multiplayerGameState.roomCode.isNotEmpty()) {
            // Sync question type with opponent if needed
            // For now, both players use the same question type selection
        }
    }
    
    // Permissions
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Log.d("MultiplayerScreen", "Permission result: $granted")
        hasPermission = granted
        cameraState = cameraState.copy(isPermissionGranted = granted)
        Log.d("MultiplayerScreen", "Updated camera state permission: ${cameraState.isPermissionGranted}")
    }
    
    // Initialize camera state with permission status
    LaunchedEffect(Unit) {
        Log.d("MultiplayerScreen", "=== INITIAL SETUP ===")
        Log.d("MultiplayerScreen", "Player: ${multiplayerGameState.localPlayerName}")
        Log.d("MultiplayerScreen", "Is Host: ${multiplayerGameState.isHost}")
        Log.d("MultiplayerScreen", "Initial permission check: $hasPermission")
        cameraState = cameraState.copy(isPermissionGranted = hasPermission)
        if (!hasPermission) {
            Log.d("MultiplayerScreen", "Requesting camera permission")
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    // Also check permission when hasPermission changes
    LaunchedEffect(hasPermission) {
        Log.d("MultiplayerScreen", "Permission changed to: $hasPermission")
        cameraState = cameraState.copy(isPermissionGranted = hasPermission)
    }
    
    LaunchedEffect(gameState) {
        Log.d("MultiplayerScreen", "=== GAME STATE CHANGE ===")
        Log.d("MultiplayerScreen", "Player: ${multiplayerGameState.localPlayerName}")
        Log.d("MultiplayerScreen", "Game State: $gameState")
        Log.d("MultiplayerScreen", "Model Interpreter: ${modelInterpreter != null}")
        Log.d("MultiplayerScreen", "Camera Permission: ${cameraState.isPermissionGranted}")
        
        if (gameState == GameState.PLAYING && modelInterpreter == null) {
            Log.d("MultiplayerScreen", "Loading model (PLAYING state) for player: ${multiplayerGameState.localPlayerName}")
            modelInterpreter = withContext(Dispatchers.IO) { loadPracticeModel(context) }
            Log.d("MultiplayerScreen", "Model loaded: ${modelInterpreter != null} for player: ${multiplayerGameState.localPlayerName}")
        }
    }
    
    // Cleanup analyzer when composable is disposed
    DisposableEffect(modelInterpreter) {
        onDispose {
            handSignAnalyzer?.cleanup()
        }
    }

    // Keep 640 input to match the model's training resolution for accuracy
    val inputSize = 640
    val imageProcessor = remember {
        ImageProcessor.Builder()
            .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
            .build()
    }

    // Audio feedback
    val correctToneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }
    val incorrectToneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }
    val coroutineScope = rememberCoroutineScope()

    // Note: Camera setup is handled in CameraPreview component
    
    // Game questions - use room code as seed so both players get same questions
    var gameSeed by remember { mutableStateOf(0) }
    val roomCodeSeed = multiplayerGameState.roomCode.hashCode()
    
    val letterQuestions = remember(selectedQuestionType, gameSeed, roomCodeSeed) {
        // Use room code + gameSeed as seed for consistent random across both players
        val random = java.util.Random((roomCodeSeed + gameSeed).toLong())
        val letters = ('A'..'Z').toList().shuffled(random).take(10)
        letters.mapIndexed { index, ch ->
            GameQuestion(
                id = "L${index + 1}",
                type = QuestionType.LETTER,
                content = ch.toString(),
                answer = ch.toString(),
                timeLimit = 10
            )
        }
    }
    
    // Word pool for generating different sets of questions
    val wordPool = listOf(
        "CAT" to "C-A-T",
        "DOG" to "D-O-G",
        "BAT" to "B-A-T",
        "HAT" to "H-A-T",
        "MAT" to "M-A-T",
        "RAT" to "R-A-T",
        "SAT" to "S-A-T",
        "BAG" to "B-A-G",
        "TAG" to "T-A-G",
        "BED" to "B-E-D",
        "RED" to "R-E-D",
        "BIG" to "B-I-G",
        "PIG" to "P-I-G",
        "LOG" to "L-O-G",
        "FOG" to "F-O-G"
    )
    
    val wordQuestions = remember(selectedQuestionType, gameSeed, roomCodeSeed) {
        // Use room code + gameSeed as seed for consistent random across both players
        val random = java.util.Random((roomCodeSeed + gameSeed).toLong())
        wordPool.shuffled(random).take(10).mapIndexed { index, (word, answer) ->
            GameQuestion(
                id = "W${index + 1}",
                type = QuestionType.WORD,
                content = word,
                answer = answer,
                timeLimit = 15
            )
        }
    }
    
    val allQuestions = if (selectedQuestionType == QuestionType.LETTER) letterQuestions else wordQuestions
    
    fun nextQuestion() {
        if (questionIndex < allQuestions.size) {
            currentQuestion = allQuestions[questionIndex]
            timeLeft = currentQuestion?.timeLimit ?: 10
            questionIndex++
            
            // Reset player answers for new question
            localPlayer = localPlayer.copy(currentAnswer = "", isCorrect = false)
            opponentPlayer = opponentPlayer.copy(currentAnswer = "", isCorrect = false)
            
            // Reset word progress for new question
            localWordProgress = ""
            opponentWordProgress = ""
            lastSignedLetter = ""
            lastLetterWasCorrect = false
            
            // Clear opponent answer in ViewModel
            multiplayerViewModel?.clearOpponentAnswer()
        } else {
            // All questions completed - check if last word was incomplete
            val lastQuestion = currentQuestion
            if (lastQuestion?.type == QuestionType.WORD) {
                val expectedWord = lastQuestion.content.uppercase()
                if (localWordProgress.length < expectedWord.length) {
                    // Last word was not completed - count as mistake
                    mistakes += 1
                    lettersCompleted += 1
                }
            }
            gameState = GameState.FINISHED
        }
    }
    
    fun startFirstQuestion() {
        if (questionIndex == 0 && currentQuestion == null) {
            currentQuestion = allQuestions[0]
            timeLeft = currentQuestion?.timeLimit ?: 10
            questionIndex = 1
            Log.d("MultiplayerScreen", "Started first question for player: ${multiplayerGameState.localPlayerName}")
            Log.d("MultiplayerScreen", "Question: ${currentQuestion?.content}, Answer: ${currentQuestion?.answer}")
        }
    }
    
    // Generate random 5-character room code
    fun generateRoomCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..5)
            .map { chars.random() }
            .joinToString("")
    }
    
    // Start hosting a game
    fun startHosting() {
        if (username.isBlank()) {
            Toast.makeText(context, "Username not available!", Toast.LENGTH_SHORT).show()
            return
        }
        
        multiplayerViewModel?.createRoom(username)
        // Immediately show connecting screen while room is being created
        gameState = GameState.CONNECTING
        soundEffects.playButtonClick()
        hapticFeedback.lightTap()
    }

    fun joinGame(roomCode: String) {
        if (username.isBlank()) {
            Toast.makeText(context, "Username not available!", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (roomCode.isBlank()) {
            Toast.makeText(context, "Please enter a room code!", Toast.LENGTH_SHORT).show()
            return
        }
        
        multiplayerViewModel?.joinRoom(roomCode, username)
        soundEffects.playButtonClick()
        hapticFeedback.lightTap()
    }



    // Calculate time-based bonus score
    fun calculateScore(isCorrect: Boolean, responseTime: Long, timeLimit: Int): Int {
        if (!isCorrect) return 0
        
        // Base score: 10 points per correct answer
        val baseScore = 10
        
        // Time bonus: Higher time = higher bonus (max 10 points)
        // Convert response time to seconds for easier calculation
        val responseTimeSeconds = responseTime / 1000.0
        
        val timeBonus = when {
            responseTimeSeconds <= 1.0 -> 1   // Very fast: 1 point
            responseTimeSeconds <= 2.0 -> 2   // Fast: 2 points
            responseTimeSeconds <= 3.0 -> 3   // Medium: 3 points
            responseTimeSeconds <= 4.0 -> 4   // Slower: 4 points
            responseTimeSeconds <= 5.0 -> 5   // Slow: 5 points
            responseTimeSeconds <= 6.0 -> 6   // Slower: 6 points
            responseTimeSeconds <= 7.0 -> 7   // Slow: 7 points
            responseTimeSeconds <= 8.0 -> 8   // Slower: 8 points
            responseTimeSeconds <= 9.0 -> 9   // Slow: 9 points
            else -> 10                        // Very slow: 10 points (max bonus)
        }
        
        val totalScore = baseScore + timeBonus
        Log.d("MultiplayerScreen", "Score calculation: Base=$baseScore, Time=${responseTimeSeconds}s, Bonus=$timeBonus, Total=$totalScore")
        return totalScore
    }

    // Advance to countdown when opponentFound toggles true (from realtime ViewModel)
    LaunchedEffect(connectionState.opponentFound, gameState) {
        if (gameState == GameState.CONNECTING && connectionState.opponentFound) {
            opponentPlayer = opponentPlayer.copy(isConnected = true)
            gameState = GameState.COUNTDOWN
        }
    }


    // Timer effect
    LaunchedEffect(timeLeft, gameState) {
        if (gameState == GameState.PLAYING && timeLeft > 0) {
            delay(1000)
            timeLeft--
        } else if (timeLeft == 0 && gameState == GameState.PLAYING) {
            // Time's up - check if word was incomplete (for word quiz type)
            val question = currentQuestion
            if (question?.type == QuestionType.WORD) {
                val expectedWord = question.content.uppercase()
                if (localWordProgress.length < expectedWord.length) {
                    // Word was not completed - count as mistake
                    mistakes += 1
                    lettersCompleted += 1 // Count as attempted/completed (even if wrong)
                }
            }
            // Move to next question
            nextQuestion()
        }
    }
    
    fun submitAnswer(answer: String) {
        val question = currentQuestion ?: return
        
        val responseTime = System.currentTimeMillis()
        val questionStartTime = questionIndex * 10000L // Approximate question start time
        val actualResponseTime = responseTime - questionStartTime
        
        Log.d("MultiplayerScreen", "=== ANSWER SUBMISSION ===")
        Log.d("MultiplayerScreen", "Player: ${multiplayerGameState.localPlayerName}")
        Log.d("MultiplayerScreen", "Question: ${currentQuestion?.content}, Type: ${currentQuestion?.type}")
        Log.d("MultiplayerScreen", "Letter Signed: '$answer'")
        
        val isCorrect: Boolean
        val newAnswer: String
        
        if (question.type == QuestionType.WORD) {
            // Word quiz: letter-by-letter spelling
            val expectedWord = question.content.uppercase()
            val currentProgress = localWordProgress.uppercase()
            val nextExpectedLetter = if (currentProgress.length < expectedWord.length) {
                expectedWord[currentProgress.length].toString()
            } else ""
            
            if (answer.uppercase() == nextExpectedLetter) {
                // Correct letter - add to progress
                localWordProgress = currentProgress + answer.uppercase()
                newAnswer = localWordProgress
                // Only mark as correct when word is fully spelled
                isCorrect = localWordProgress.length == expectedWord.length
                
                // Track last letter feedback
                lastSignedLetter = answer.uppercase()
                lastLetterWasCorrect = true
                
                Log.d("MultiplayerScreen", "✅ Correct letter! Progress: '$localWordProgress'")
            } else {
                // Wrong letter - keep current progress (don't reset), just don't advance
                newAnswer = currentProgress
                isCorrect = false
                
                // Track last letter feedback
                lastSignedLetter = answer.uppercase()
                lastLetterWasCorrect = false
                
                Log.d("MultiplayerScreen", "❌ Wrong letter! Expected '$nextExpectedLetter', got '$answer'. Progress kept: '$currentProgress'")
            }
        } else {
            // Letter quiz: single letter answer
            isCorrect = answer.equals(question.answer, ignoreCase = true)
            newAnswer = answer
        }

        // Update streak: increment on correct, reset on wrong
        if (isCorrect) {
            streak += 1
        } else {
            streak = 0
        }

        // Update local player state WITHOUT adding score (ViewModel will do that)
        localPlayer = localPlayer.copy(
            currentAnswer = newAnswer,
            isCorrect = isCorrect,
            responseTime = actualResponseTime,
            totalCorrectAnswers = if (isCorrect) localPlayer.totalCorrectAnswers + 1 else localPlayer.totalCorrectAnswers
        )

        // Track session stats for summary
        if (question.type == QuestionType.WORD) {
            // For word quiz: only count when word is fully completed
            if (isCorrect) {
                // Word is fully spelled correctly
                perfectSigns += 1
                lettersCompleted += 1 // Count as one completed word
            }
            // Don't count mistakes for individual wrong letters in word quiz
            // Only count if word is incomplete when time runs out (handled in nextQuestion)
        } else {
            // For letter quiz: count each submission
            if (isCorrect) {
                perfectSigns += 1
            } else {
                mistakes += 1
            }
            lettersCompleted += 1
        }

        Log.d("MultiplayerScreen", "Score will be updated by ViewModel after sync")

        // Submit answer to ViewModel for synchronization and score calculation
        multiplayerViewModel?.submitAnswer(newAnswer, isCorrect, actualResponseTime)

        // Play audio and haptics based on correctness
        if (isCorrect) {
            soundEffects.playCorrect()
            hapticFeedback.successPattern()
            Log.d("MultiplayerScreen", "✅ Correct: '$newAnswer'")
        } else {
            incorrectToneGenerator.startTone(ToneGenerator.TONE_SUP_ERROR, 150)
            hapticFeedback.lightTap()
            Log.d("MultiplayerScreen", "❌ Incorrect or incomplete")
        }
    }
    
    // Note: Opponent responses are now handled through ViewModel message listening
    
    // Removed auto-advance - players must manually proceed to next question
    
    fun resetGame() {
        questionIndex = 0
        currentQuestion = null
        timeLeft = 0
        localPlayer = localPlayer.copy(score = 0, currentAnswer = "", isCorrect = false)
        opponentPlayer = opponentPlayer.copy(score = 0, currentAnswer = "", isCorrect = false)
        lettersCompleted = 0
        perfectSigns = 0
        mistakes = 0
        streak = 0
        // Reset word progress
        localWordProgress = ""
        opponentWordProgress = ""
        lastSignedLetter = ""
        lastLetterWasCorrect = false
        // Regenerate questions for a fresh session
        gameSeed += 1
        
        // Clear opponent answer in ViewModel
        multiplayerViewModel?.clearOpponentAnswer()
    }

    fun restartMultiplayerGame() {
        resetGame() // This already increments gameSeed to regenerate questions
        // Clear finished flag and scores in ViewModel, then start countdown
        multiplayerViewModel?.restartGameForBothPlayers()
        gameState = GameState.COUNTDOWN
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎮", fontSize = 26.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Play Together!", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("👫", fontSize = 22.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        soundEffects.playButtonClick()
                        hapticFeedback.lightTap()
                        // If we're in CONNECTING state (host game code), go back to lobby
                        if (gameState == GameState.CONNECTING) {
                            gameState = GameState.LOBBY
                            connectionState = ConnectionState()
                            multiplayerViewModel?.leaveRoom()
                        }
                        // If we're in-game (PLAYING, COUNTDOWN, RESULTS), go back to lobby
                        else if (gameState == GameState.PLAYING || gameState == GameState.COUNTDOWN || gameState == GameState.RESULTS) {
                            if (!isExitHandled) {
                                isExitHandled = true
                                showProgressDialog = false
                                progressUpdate = null
                                hasShownExitSummary = false
                                hasShownResultsOnce = false
                                gameState = GameState.LOBBY
                                resetGame()
                                multiplayerViewModel?.leaveRoom()
                            }
                        }
                        // If we're in lobby or not connected, go back to lessons screen
                        else if (gameState == GameState.LOBBY || !(multiplayerGameState?.isConnected ?: false)) {
                            navController?.navigate("studentDashboard/$username") {
                                popUpTo("studentDashboard/{username}") { inclusive = false }
                                launchSingleTop = true
                            }
                        } else {
                            // Otherwise (FINISHED state), terminate the session and return to lessons
                            if (!isExitHandled) {
                                isExitHandled = true
                                showProgressDialog = false
                                progressUpdate = null
                                hasShownExitSummary = false
                                hasShownResultsOnce = false
                                gameState = GameState.LOBBY
                                resetGame()
                                multiplayerViewModel?.leaveRoom()
                                navController?.navigate("studentDashboard/$username") {
                                    popUpTo("studentDashboard/{username}") { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        }
                    }) {
                        Text("⬅️", fontSize = 26.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFE91E63),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBackground)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (gameState) {
                GameState.LOBBY -> LobbyScreen(
                    selectedQuestionType = selectedQuestionType,
                    onQuestionTypeChanged = { selectedQuestionType = it },
                    onStartHosting = { startHosting() },
                    onJoinGame = { roomCode -> joinGame(roomCode) },
                    roomCodeInput = roomCodeInput,
                    onRoomCodeChanged = { roomCodeInput = it },
                    isLoading = isLoading,
                    soundEffects = soundEffects,
                    hapticFeedback = hapticFeedback
                )
                
                GameState.CONNECTING -> ConnectingScreen(
                    connectionState = connectionState,
                    onCancel = { 
                        gameState = GameState.LOBBY
                        connectionState = ConnectionState()
                    },
                    onOpponentFound = {
                        connectionState = connectionState.copy(
                            opponentFound = true,
                            isWaitingForOpponent = false
                        )
                        opponentPlayer = opponentPlayer.copy(isConnected = true)
                        gameState = GameState.COUNTDOWN
                    }
                )
                
                GameState.COUNTDOWN -> CountdownScreen(
                    onGameStart = { 
                        // Start game for both players
                        multiplayerViewModel?.startGameForBothPlayers()
                        startFirstQuestion()
                        gameState = GameState.PLAYING
                    }
                )
                
                GameState.PLAYING -> PlayingScreen(
                    currentQuestion = currentQuestion,
                    timeLeft = timeLeft,
                    localPlayer = localPlayer,
                    opponentPlayer = opponentPlayer,
                    questionIndex = questionIndex,
                    allQuestions = allQuestions,
                    cameraState = cameraState,
                    streak = streak,
                    useFrontCamera = useFrontCamera,
                    localWordProgress = localWordProgress,
                    opponentWordProgress = opponentWordProgress,
                    lastSignedLetter = lastSignedLetter,
                    lastLetterWasCorrect = lastLetterWasCorrect,
                    onAnswerSubmit = { submitAnswer(it) },
                    onNextQuestion = { nextQuestion() },
                    onSkip = { nextQuestion() },
                    onCameraSwitch = { useFrontCamera = !useFrontCamera },
                    onUpdateLocalPlayer = { localPlayer = it },
                    onUpdateCameraState = { cameraState = it },
                    soundEffects = soundEffects,
                    hapticFeedback = hapticFeedback,
                    modelInterpreter = modelInterpreter,
                    imageProcessor = imageProcessor,
                    inputSize = inputSize
                )
                
                GameState.RESULTS -> ResultsScreen(
                    localPlayer = localPlayer,
                    opponentPlayer = opponentPlayer,
                    onNextQuestion = { nextQuestion() },
                    onFinishGame = { gameState = GameState.FINISHED }
                )
                
                GameState.FINISHED -> FinalResultsScreen(
                    localPlayer = localPlayer,
                    opponentPlayer = opponentPlayer,
                    onPlayAgain = { restartMultiplayerGame() },
                    onBackToLobby = {
                        // Leave the room and go back to multiplayer lobby view
                        multiplayerViewModel?.leaveRoom()
                        resetGame()
                        gameState = GameState.LOBBY
                        // Reset flags so summary won't reappear spuriously
                        hasShownExitSummary = false
                        hasShownResultsOnce = false
                        isExitHandled = false
                    },
                    username = username,
                    progressTrackingService = progressTrackingService,
                    scope = scope,
                    lettersCompleted = lettersCompleted,
                    perfectSigns = perfectSigns,
                    mistakes = mistakes,
                    sessionStartTime = sessionStartTime,
                    onProgressUpdate = { update ->
                        if (!hasShownExitSummary) {
                            progressUpdate = update
                            showProgressDialog = true
                            hasShownExitSummary = true
                        }
                    }
                )
            }
            }
        }
        
        // Progress Update Dialog
        if (showProgressDialog && progressUpdate != null) {
            AlertDialog(
                onDismissRequest = { showProgressDialog = false },
                title = { Text("🎉 Great Job!") },
                text = {
                    Column {
                        Text("You completed the multiplayer game!")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("XP Gained: ${progressUpdate!!.xpGained}")
                        Text("Score Gained: ${progressUpdate!!.scoreGained}")
                        if (progressUpdate!!.achievementsUnlocked.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Achievements Unlocked:")
                            progressUpdate!!.achievementsUnlocked.forEach { achievementId ->
                                val (title, description) = progressTrackingService.getAchievementDetails(achievementId)
                                Text("• $title: $description", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (progressUpdate!!.levelUp) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("🎊 Level Up! You're now level ${progressUpdate!!.newLevel}!", 
                                color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            // Close dialog and show results once
                            showProgressDialog = false
                            progressUpdate = null
                            if (!hasShownResultsOnce) {
                                gameState = GameState.FINISHED
                                hasShownResultsOnce = true
                            }
                        }
                    ) {
                        Text("Continue")
                    }
                }
            )
        }
    }
}

@Composable
fun LobbyScreen(
    selectedQuestionType: QuestionType,
    onQuestionTypeChanged: (QuestionType) -> Unit,
    onStartHosting: () -> Unit,
    onJoinGame: (String) -> Unit,
    roomCodeInput: String,
    onRoomCodeChanged: (String) -> Unit,
    isLoading: Boolean,
    soundEffects: com.example.signbuddy.ui.components.SoundEffectsManager,
    hapticFeedback: com.example.signbuddy.ui.components.HapticFeedbackManager
) {
    var showJoinDialog by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "🎯 Multiplayer Quiz",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        // Engaging subtitle
        Text(
            text = "Challenge a friend and see who's the fastest! 🏆",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        // Question Type Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📝 Choose Quiz Type",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Letters Option
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedQuestionType == QuestionType.LETTER) 
                                MaterialTheme.colorScheme.primary else Color(0xFFF5F5F5)
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (selectedQuestionType == QuestionType.LETTER) 6.dp else 2.dp
                        ),
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            soundEffects.playButtonClick()
                            hapticFeedback.lightTap()
                            onQuestionTypeChanged(QuestionType.LETTER)
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🔤", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Letters",
                                color = if (selectedQuestionType == QuestionType.LETTER) 
                                    Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Individual letters",
                                color = if (selectedQuestionType == QuestionType.LETTER) 
                                    Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }
                    
                    // Words Option
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedQuestionType == QuestionType.WORD) 
                                MaterialTheme.colorScheme.primary else Color(0xFFF5F5F5)
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (selectedQuestionType == QuestionType.WORD) 6.dp else 2.dp
                        ),
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            soundEffects.playButtonClick()
                            hapticFeedback.lightTap()
                            onQuestionTypeChanged(QuestionType.WORD)
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📝", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Words",
                                color = if (selectedQuestionType == QuestionType.WORD) 
                                    Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Spell words",
                                color = if (selectedQuestionType == QuestionType.WORD) 
                                    Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
        
        // Game Rules
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🎮 How to Play",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                val rules = listOf(
                    "• Connect with another player",
                    "• Both see same question",
                    "• Use camera to sign answer",
                    "• Fastest & accurate wins!"
                )
                
                rules.forEach { rule ->
                    Text(
                        text = rule,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
        
        // Multiplayer Options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Host Game Button
            Button(
                onClick = {
                    onStartHosting()
                    soundEffects.playButtonClick()
                    hapticFeedback.lightTap()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = "🏠 Host Game",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
            
            // Join Game Button
            Button(
                onClick = {
                    showJoinDialog = true
                    soundEffects.playButtonClick()
                    hapticFeedback.lightTap()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "🔗 Join Game",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
        
        // Join Game Dialog
        if (showJoinDialog) {
            AlertDialog(
                onDismissRequest = { showJoinDialog = false },
                title = {
                    Text(
                        text = "Join Game",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Enter the 5-character room code:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = roomCodeInput,
                            onValueChange = { onRoomCodeChanged(it.uppercase().take(5)) },
                            label = { Text("Room Code") },
                            placeholder = { Text("ABC12") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (roomCodeInput.length == 5) {
                                onJoinGame(roomCodeInput)
                                showJoinDialog = false
                                onRoomCodeChanged("")
                            }
                        },
                        enabled = roomCodeInput.length == 5,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Join", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showJoinDialog = false },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ConnectingScreen(
    connectionState: ConnectionState,
    onCancel: () -> Unit,
    onOpponentFound: () -> Unit
) {
    // React to realtime opponent join from ViewModel state
    LaunchedEffect(connectionState.opponentFound) {
        if (connectionState.opponentFound) {
            onOpponentFound()
        }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Animated connecting indicator
        CircularProgressIndicator(
            modifier = Modifier.size(80.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 6.dp
        )
        
        Text(
            text = if (connectionState.isHost) "🏠 Hosting Game" else "🔗 Joining Game",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        
        // Room Code Display
        if (connectionState.roomCode.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Room Code",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = connectionState.roomCode,
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color(0xFF2196F3),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    )
                    if (connectionState.isHost) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Share this code with your friend",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
        
        Text(
            text = if (connectionState.isHost) 
                "Waiting for a real player to join using your room code..." 
            else 
                "Connecting to host...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        
        Button(
            onClick = onCancel,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Cancel", color = Color.White)
        }
    }
}

@Composable
fun CountdownScreen(onGameStart: () -> Unit) {
    var countdown by remember { mutableStateOf(3) }
    
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        delay(1000)
        onGameStart()
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Get Ready!",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (countdown > 0) countdown.toString() else "GO!",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PlayingScreen(
    currentQuestion: GameQuestion?,
    timeLeft: Int,
    localPlayer: Player,
    opponentPlayer: Player,
    questionIndex: Int,
    allQuestions: List<GameQuestion>,
    cameraState: CameraState,
    streak: Int,
    useFrontCamera: Boolean,
    localWordProgress: String,
    opponentWordProgress: String,
    lastSignedLetter: String,
    lastLetterWasCorrect: Boolean,
    onAnswerSubmit: (String) -> Unit,
    onNextQuestion: () -> Unit,
    onSkip: () -> Unit,
    onCameraSwitch: () -> Unit,
    onUpdateLocalPlayer: (Player) -> Unit,
    onUpdateCameraState: (CameraState) -> Unit,
    soundEffects: com.example.signbuddy.ui.components.SoundEffectsManager,
    hapticFeedback: com.example.signbuddy.ui.components.HapticFeedbackManager,
    modelInterpreter: Interpreter?,
    imageProcessor: ImageProcessor,
    inputSize: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // White Card with everything inside
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top: Timer, You Score, Opponent Score
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Timer - Compact
                    Card(
                        modifier = Modifier.weight(0.3f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (timeLeft <= 5) Color(0xFFFF5722) else Color(0xFF4CAF50)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "⏱️ $timeLeft",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    
                    // You Score - Compact
                    Column(
                        modifier = Modifier.weight(0.35f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "You",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                        LinearProgressIndicator(
                            progress = localPlayer.score / 100f,
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF4CAF50),
                            trackColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${localPlayer.score}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 9.sp
                            )
                            Text(
                                text = "🔥$streak",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF6F00),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    }
                    
                    // Opponent Score - Compact
                    Column(
                        modifier = Modifier.weight(0.35f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Opponent",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF6B6B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                        LinearProgressIndicator(
                            progress = opponentPlayer.score / 100f,
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFFF6B6B),
                            trackColor = Color(0xFFFF6B6B).copy(alpha = 0.2f)
                        )
                        Text(
                            text = "${opponentPlayer.score}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 9.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Center: You feedback (left) | Letter (center) | Opponent feedback (right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // You feedback - Left
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentQuestion?.type == QuestionType.WORD) {
                                // For word quiz: green if last letter correct, red if wrong, gray if no letter
                                if (lastSignedLetter.isNotEmpty()) {
                                    if (lastLetterWasCorrect) Color(0xFF4CAF50) else Color(0xFFFF5722)
                                } else Color(0xFFF5F5F5)
                            } else {
                                // For letter quiz: original logic
                                if (localPlayer.isCorrect) Color(0xFF4CAF50) else 
                                if (localPlayer.currentAnswer.isNotEmpty()) Color(0xFFFF9800) else Color(0xFFF5F5F5)
                            }
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (currentQuestion?.type == QuestionType.WORD) {
                                    if (localWordProgress.length == (currentQuestion?.content?.length ?: 0)) "✅"
                                    else if (lastSignedLetter.isNotEmpty()) if (lastLetterWasCorrect) "✓" else "✗"
                                    else "⭕"
                                } else {
                                    if (localPlayer.isCorrect) "✅" else if (localPlayer.currentAnswer.isNotEmpty()) "⏳" else "⭕"
                                },
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentQuestion?.type == QuestionType.WORD) {
                                    // For words, show last letter signed with color feedback
                                    if (lastSignedLetter.isNotEmpty()) {
                                        "You: $lastSignedLetter"
                                    } else {
                                        "You: ..."
                                    }
                                } else {
                                    "You: ${if (localPlayer.currentAnswer.isNotEmpty()) localPlayer.currentAnswer else "..."}"
                                },
                                color = if (currentQuestion?.type == QuestionType.WORD) {
                                    if (lastSignedLetter.isNotEmpty()) {
                                        if (lastLetterWasCorrect) Color.White else Color.White
                                    } else Color.Black
                                } else {
                                    if (localPlayer.isCorrect) Color.White else Color.Black
                                },
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        }
                    }
                    
                    // Letter/Word to be signed - Center
                    Box(
                        modifier = Modifier
                            .size(if (currentQuestion?.type == QuestionType.WORD) 90.dp else 80.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(40.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentQuestion?.type == QuestionType.WORD) {
                            // For words: show each letter with opacity based on completion
                            val word = currentQuestion.content.uppercase()
                            val completedCount = localWordProgress.length
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                word.forEachIndexed { index, char ->
                                    val isCompleted = index < completedCount
                                    Text(
                                        text = char.toString(),
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = MaterialTheme.colorScheme.primary.copy(
                                            alpha = if (isCompleted) 0.4f else 1.0f
                                        ),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 28.sp
                                    )
                                }
                                // Show check icon when word is complete
                                if (completedCount == word.length) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "✓",
                                        fontSize = 28.sp,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                            }
                        } else {
                            // For letters: original display
                            Text(
                                text = currentQuestion?.content ?: "",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 40.sp
                            )
                        }
                    }
                    
                    // Opponent feedback - Right
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (opponentPlayer.isCorrect) Color(0xFFFF6B6B) else 
                                           if (opponentPlayer.currentAnswer.isNotEmpty()) Color(0xFFFF9800) else Color(0xFFF5F5F5)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (opponentPlayer.isCorrect) "✅" else if (opponentPlayer.currentAnswer.isNotEmpty()) "⏳" else "⭕",
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentQuestion?.type == QuestionType.WORD) {
                                    // For words, show progress: "CA" or "CAT"
                                    "Opp: ${if (opponentWordProgress.isNotEmpty()) opponentWordProgress else "..."}"
                                } else {
                                    "Opp: ${if (opponentPlayer.currentAnswer.isNotEmpty()) opponentPlayer.currentAnswer else "..."}"
                                },
                                color = if (opponentPlayer.isCorrect) Color.White else Color.Black,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Question Progress - Centered below the letter
                Text(
                    text = "Question ${questionIndex + 1} of ${allQuestions.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        
        // Camera Preview - Bigger
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Real Camera Preview Area
                Log.d("MultiplayerScreen", "Camera permission check: ${cameraState.isPermissionGranted}")
                if (cameraState.isPermissionGranted) {
                    CameraPreview(
                        cameraState = cameraState,
                        currentQuestion = currentQuestion,
                        useFrontCamera = useFrontCamera,
                        onSignDetected = { sign, confidence ->
                            Log.d("MultiplayerScreen", "=== ON SIGN DETECTED ===")
                            Log.d("MultiplayerScreen", "Sign: '$sign', Confidence: $confidence")
                            
                            onUpdateCameraState(cameraState.copy(
                                lastDetectedSign = sign,
                                confidence = confidence,
                                isAnalyzing = true
                            ))
                            onUpdateLocalPlayer(localPlayer.copy(currentAnswer = sign))
                            
                            // Real-time sign detection and scoring
                            if (confidence > 0.75f) {
                                Log.d("MultiplayerScreen", "Confidence check passed - calling onAnswerSubmit")
                                onAnswerSubmit(sign)
                            } else {
                                Log.d("MultiplayerScreen", "Confidence too low - not submitting answer")
                            }
                        },
                        onAnalysisComplete = {
                            onUpdateCameraState(cameraState.copy(isAnalyzing = false))
                        },
                        modelInterpreter = modelInterpreter,
                        imageProcessor = imageProcessor,
                        inputSize = inputSize
                    )
                } else {
                    // Camera permission denied placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color.Red.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🚫", fontSize = 48.sp)
                            Text(
                                text = "Camera Permission Required",
                                color = Color.Red,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
            }
        }
        
        // Essential Game Controls - Skip and Camera Switch (same style as EvaluationScreen)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Skip Button
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(10.dp),
                onClick = {
                    soundEffects.playButtonClick()
                    hapticFeedback.lightTap()
                    onSkip()
                }
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("⏭️  Skip", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            
            // Camera Switch Button
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF9C27B0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(10.dp),
                onClick = {
                    soundEffects.playButtonClick()
                    hapticFeedback.lightTap()
                    onCameraSwitch()
                }
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (useFrontCamera) "📷  Back" else "📷  Front",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ResultsScreen(
    localPlayer: Player,
    opponentPlayer: Player,
    onNextQuestion: () -> Unit,
    onFinishGame: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "🎯 Round Results",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        
        // Results comparison
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Local Player Result
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (localPlayer.isCorrect) Color(0xFF4CAF50) else Color(0xFFFF5722)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("You", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (localPlayer.isCorrect) "✅ Correct!" else "❌ Wrong",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                    Text("${localPlayer.score} pts", color = Color.White)
                }
            }
            
            // Opponent Player Result
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (opponentPlayer.isCorrect) Color(0xFFFF6B6B) else Color(0xFFFF5722)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Opponent", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (opponentPlayer.isCorrect) "✅ Correct!" else "❌ Wrong",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                    Text("${opponentPlayer.score} pts", color = Color.White)
                }
            }
        }
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onNextQuestion,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Next Question", color = Color.White)
            }
            
            Button(
                onClick = onFinishGame,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Finish Game", color = Color.White)
            }
        }
    }
}

@Composable
fun FinalResultsScreen(
    localPlayer: Player,
    opponentPlayer: Player,
    onPlayAgain: () -> Unit,
    onBackToLobby: () -> Unit,
    username: String = "",
    progressTrackingService: ProgressTrackingService? = null,
    scope: kotlinx.coroutines.CoroutineScope? = null,
    lettersCompleted: Int = 0,
    perfectSigns: Int = 0,
    mistakes: Int = 0,
    sessionStartTime: Long = 0L,
    onProgressUpdate: (ProgressTrackingService.ProgressUpdate) -> Unit = {}
) {
    val isWinner = localPlayer.score > opponentPlayer.score
    val isTie = localPlayer.score == opponentPlayer.score
    
    // Track progress when game ends
    LaunchedEffect(Unit) {
        if (username.isNotEmpty() && progressTrackingService != null && scope != null) {
            scope.launch {
                val sessionResult = ProgressTrackingService.SessionResult(
                    mode = "multiplayer",
                    accuracy = if (lettersCompleted > 0) (perfectSigns.toFloat() / lettersCompleted.toFloat()).coerceAtMost(1.0f) else 0f,
                    timeSpent = if (sessionStartTime > 0) (System.currentTimeMillis() - sessionStartTime) / 1000 else 0,
                    lettersCompleted = lettersCompleted,
                    perfectSigns = perfectSigns,
                    mistakes = mistakes,
                    actualScore = localPlayer.score
                )
                
                progressTrackingService.updateProgress(username, sessionResult)
                    .onSuccess { update ->
                        onProgressUpdate(update)
                    }
                    .onFailure { /* Handle error */ }
            }
        }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Winner announcement
        Text(
            text = when {
                isTie -> "🤝 It's a Tie!"
                isWinner -> "🎉 You Win!"
                else -> "😔 You Lost"
            },
            style = MaterialTheme.typography.headlineLarge,
            color = if (isTie) Color(0xFF9C27B0) else if (isWinner) Color(0xFF4CAF50) else Color(0xFFFF5722),
            fontWeight = FontWeight.Bold
        )
        
        // Final scores
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🏆 Final Scores",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("You", fontWeight = FontWeight.Bold)
                        Text("${localPlayer.score} pts", fontSize = 24.sp, color = Color(0xFF4CAF50))
                    }
                    
                    Text("VS", fontSize = 20.sp, color = Color.Gray)
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Opponent", fontWeight = FontWeight.Bold)
                        Text("${opponentPlayer.score} pts", fontSize = 24.sp, color = Color(0xFFFF6B6B))
                    }
                }
            }
        }
        
        // Continue and Play Again buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Continue button
            Button(
                onClick = onBackToLobby,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Continue", color = Color.White)
            }
            
            // Play Again button
            Button(
                onClick = onPlayAgain,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("🔄 Play Again", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CameraPreview(
    cameraState: CameraState,
    currentQuestion: GameQuestion?,
    useFrontCamera: Boolean,
    onSignDetected: (String, Float) -> Unit,
    onAnalysisComplete: () -> Unit,
    modelInterpreter: Interpreter?,
    imageProcessor: ImageProcessor,
    inputSize: Int
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var handSignAnalyzer by remember { mutableStateOf<MultiplayerHandSignAnalyzer?>(null) }
    
    // CameraX setup: runs only when permission granted and model loaded
    LaunchedEffect(cameraState.isPermissionGranted, modelInterpreter, useFrontCamera) {
        Log.d("CameraPreview", "=== CAMERA SETUP ===")
        Log.d("CameraPreview", "Permission granted: ${cameraState.isPermissionGranted}")
        Log.d("CameraPreview", "Model loaded: ${modelInterpreter != null}")
        Log.d("CameraPreview", "Use front camera: $useFrontCamera")
        if (!cameraState.isPermissionGranted || modelInterpreter == null) {
            Log.d("CameraPreview", "Skipping camera setup - missing permission or model")
            return@LaunchedEffect
        }

        val provider = ProcessCameraProvider.getInstance(context).get()
        val preview = Preview.Builder().build().apply { setSurfaceProvider(previewView.surfaceProvider) }
        val selector = if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        Log.d("CameraPreview", "Camera provider and preview created, using ${if (useFrontCamera) "front" else "back"} camera")

        // ImageAnalysis: keep latest frames only and analyze on background executor
        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(Size(320, 320))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        // Use the analyzer on the background executor
        Log.d("CameraPreview", "Creating hand sign analyzer...")
        val analyzer = MultiplayerHandSignAnalyzer(
            modelInterpreter = modelInterpreter,
            imageProcessor = imageProcessor,
            inputSize = inputSize,
            useFrontCamera = useFrontCamera,
            context = context,
            onPrediction = { prediction: String, confidence: Float ->
                Log.d("CameraPreview", "=== PREDICTION RECEIVED ===")
                Log.d("CameraPreview", "Prediction: '$prediction'")
                Log.d("CameraPreview", "Confidence: $confidence")
                Log.d("CameraPreview", "Is empty: ${prediction.isNullOrEmpty()}")
                Log.d("CameraPreview", "Is not 'nothing': ${prediction != "nothing"}")
                
                if (!prediction.isNullOrEmpty() && prediction != "nothing") {
                    Log.d("CameraPreview", "Calling onSignDetected with: '$prediction'")
                    onSignDetected(prediction, confidence)
                } else {
                    Log.d("CameraPreview", "Prediction ignored - empty or 'nothing'")
                }
                onAnalysisComplete()
            }
        )
        
        // Store analyzer instance for cleanup
        handSignAnalyzer = analyzer
        Log.d("CameraPreview", "Setting analyzer on image analyzer...")
        imageAnalyzer.setAnalyzer(cameraExecutor, analyzer)

        try {
            Log.d("CameraPreview", "Binding camera to lifecycle...")
            provider.unbindAll()
            val camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalyzer)
            Log.d("CameraPreview", "Camera bound successfully: $camera")
            
            // Add a callback to check if the camera is actually active
            camera.cameraInfo.cameraState.observe(lifecycleOwner) { state ->
                Log.d("CameraPreview", "Camera state: $state")
            }
        } catch (e: Exception) {
            Log.e("CameraPreview", "Error binding camera", e)
            e.printStackTrace()
        }
    }
    
    // Cleanup camera on dispose
    DisposableEffect(Unit) {
        onDispose {
            handSignAnalyzer?.cleanup()
            cameraExecutor.shutdown()
        }
    }
    
    // Camera preview + overlay (with guide box and feedback overlay) - 300dp to fit buttons
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = {
                val frame = FrameLayout(context).apply {
                    addView(previewView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                    val overlay = MultiplayerOverlayView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                    }
                    addView(overlay)
                }
                frame
            },
            modifier = Modifier.matchParentSize()
        )
        
        // Analysis indicator
        if (cameraState.isAnalyzing) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp
                )
            }
        }
        
        // Detected sign overlay
        if (cameraState.lastDetectedSign.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .background(
                        Color.Black.copy(alpha = 0.7f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp)
            ) {
                Text(
                    text = "Detected: ${cameraState.lastDetectedSign} (${(cameraState.confidence * 100).toInt()}%)",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Removed unused analyzeImage function - using MultiplayerHandSignAnalyzer instead



// Removed unused preprocessImageForModel function - using MultiplayerHandSignAnalyzer instead

// Load TFLite model from assets with NNAPI acceleration
private fun loadPracticeModel(context: Context): Interpreter? {
    return try {
        val model = loadModelFile(context, "asl_model.tflite")
        val options = Interpreter.Options().apply {
            setNumThreads(4)
            try {
                val nnApiDelegate = org.tensorflow.lite.nnapi.NnApiDelegate()
                addDelegate(nnApiDelegate)
            } catch (e: Exception) { }
        }
        Interpreter(model, options)
    } catch (e: Exception) {
        Log.e("MultiplayerScreen", "Failed to load model", e)
        null
    }
}

private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
    val fileDescriptor = context.assets.openFd(modelPath)
    val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
    val fileChannel = inputStream.channel
    val startOffset = fileDescriptor.startOffset
    val declaredLength = fileDescriptor.declaredLength
    return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
}

// Function to horizontally flip bitmap for front camera mirror effect
private fun flipHorizontally(bitmap: Bitmap): Bitmap {
    val matrix = Matrix()
    matrix.preScale(-1f, 1f)
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

// Note: Uses the single package-level extension `ImageProxy.toBitmap(rotationDegrees: Int)`
// defined in `PracticeScreen.kt` to avoid duplicate overloads.

// Hand sign analyzer for YOLOv8-style TFLite (post-processing + confidence threshold 0.75)
class MultiplayerHandSignAnalyzer(
    private val modelInterpreter: Interpreter?,
    private val imageProcessor: ImageProcessor,
    private val inputSize: Int,
    private val useFrontCamera: Boolean,
    private val context: Context,
    private val onPrediction: (String, Float) -> Unit
) : ImageAnalysis.Analyzer {
    private val handler = Handler(Looper.getMainLooper())
    private var lastAnalysisTime = 0L
    private val analysisInterval = 1500L // ms - analyze every 1.5 seconds for smooth camera
    private val TAG = "MultiplayerHandSignAnalyzer"
    private var frameCount = 0
    // ASL labels: 0-25 A-Z, 26 nothing
    private val labels = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "nothing")
    // Model params (adapt if your model differs)
    private val numClasses = 27
    private val numFeatures = 4 + numClasses // 31
    private val numDetections = 8400
    private val confThreshold = 0.30f // raw detection threshold (filter noise early)
    private val feedbackThreshold = 0.70f // FINAL confidence threshold (70%)
    private val iouThreshold = 0.5f
    
    // Single high-confidence detection (no consecutive required at 70%+)
    @Volatile private var lastPredictionTime = 0L
    private val predictionCooldown = 2500L // 2.5 sec cooldown after prediction to avoid double

    // Reusable buffers to reduce allocations and GC pressure
    private val inputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4).apply {
        order(ByteOrder.nativeOrder())
    }
    private val outputArray3D = Array(1) { Array(numFeatures) { FloatArray(numDetections) } }
    private val pixels = IntArray(inputSize * inputSize)
    private val inputArray = FloatArray(inputSize * inputSize * 3)
    
    // Thread safety for interpreter access
    private val interpreterLock = Any()

    override fun analyze(image: ImageProxy) {
        frameCount++
        
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAnalysisTime < analysisInterval) {
            image.close()
            return
        }
        lastAnalysisTime = currentTime
        
        if (modelInterpreter == null) {
            image.close()
            handler.post { onPrediction("", 0f) }
            return
        }

        try {
            // Handle rotation and flip for front/back camera
            val rotationDegrees = if (useFrontCamera) 270 else 90
            var bitmap = image.toBitmap(rotationDegrees)
            if (useFrontCamera) {
                bitmap = flipHorizontally(bitmap)
            }

            // Prepare TensorImage and process
            val tensorImage = TensorImage.fromBitmap(bitmap)
            val processedImage = imageProcessor.process(tensorImage)

            // Prepare float input buffer (NHWC float [0,1] normalized)
            inputBuffer.rewind()
            processedImage.bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
            var floatIndex = 0
            for (pixel in pixels) {
                val r = (pixel shr 16 and 0xFF) / 255.0f
                val g = (pixel shr 8 and 0xFF) / 255.0f
                val b = (pixel and 0xFF) / 255.0f
                inputArray[floatIndex++] = r
                inputArray[floatIndex++] = g
                inputArray[floatIndex++] = b
            }
            inputBuffer.asFloatBuffer().put(inputArray)

            // Run inference (output shape expected [1, numFeatures, numDetections])
            // Use synchronized block to prevent concurrent access
            // Check interpreter is not null before using it
            val interpreter = modelInterpreter
            if (interpreter == null) {
                image.close()
                return
            }
            
            synchronized(interpreterLock) {
                if (modelInterpreter == null) {
                    image.close()
                    return
                }
                try {
                    modelInterpreter!!.run(inputBuffer, outputArray3D)
                } catch (e: IllegalStateException) {
                    image.close()
                    return
                } catch (e: Exception) {
                    image.close()
                    return
                }
            }

            // Flatten output for processing
            val outputFlat = FloatArray(numFeatures * numDetections)
            var idx = 0
            for (batch in outputArray3D) {
                for (feat in batch) {
                    for (det in feat) {
                        outputFlat[idx++] = det
                    }
                }
            }
            
            // Post-process: decode detections
            val boundingBoxes = mutableListOf<BoundingBox>()
            for (c in 0 until numDetections) {
                var maxConf = 0f
                var maxIdx = -1
                var j = 4
                var arrayIdx = c + numDetections * j
                while (j < numFeatures) {
                    if (outputFlat[arrayIdx] > maxConf) {
                        maxConf = outputFlat[arrayIdx]
                        maxIdx = j - 4
                    }
                    j++
                    arrayIdx += numDetections
                }
                if (maxConf > confThreshold && maxIdx != 26) { // ignore "nothing"
                    val cx = outputFlat[c]
                    val cy = outputFlat[c + numDetections]
                    val w = outputFlat[c + numDetections * 2]
                    val h = outputFlat[c + numDetections * 3]
                    val x1 = cx - (w / 2f)
                    val y1 = cy - (h / 2f)
                    val x2 = cx + (w / 2f)
                    val y2 = cy + (h / 2f)
                    if (x1 >= 0f && y1 >= 0f && x2 <= 1f && y2 <= 1f) {
                        val clsName = labels[maxIdx]
                        boundingBoxes.add(BoundingBox(x1, y1, x2, y2, cx, cy, w, h, maxConf, maxIdx, clsName))
                    }
                }
            }

            if (boundingBoxes.isEmpty()) {
                handler.post { onPrediction("", 0f) }
                image.close()
                return
            }

            // Apply NMS
            val selectedBoxes = applyNMS(boundingBoxes)

            // Pick highest conf box
            val bestBox = selectedBoxes.maxByOrNull { it.cnf }
            
            // Check if we're in cooldown period (avoid double prediction)
            val now = System.currentTimeMillis()
            val inCooldown = (now - lastPredictionTime) < predictionCooldown
            
            val predictedLetter = if (bestBox != null && bestBox.cnf >= feedbackThreshold && !inCooldown) {
                lastPredictionTime = now // Start cooldown
                bestBox.clsName
            } else {
                ""
            }
            
            handler.post { onPrediction(predictedLetter, bestBox?.cnf ?: 0f) }
            // Close image after successful analysis
            image.close()

    } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Out of memory during analysis", e)
            // Force garbage collection
            System.gc()
            handler.post { onPrediction("", 0f) }
        } catch (e: IllegalStateException) {
            // Interpreter or camera was closed
            Log.w(TAG, "Illegal state during analysis (likely closed)", e)
            handler.post { onPrediction("", 0f) }
        } catch (e: NullPointerException) {
            // Something became null during analysis
            Log.w(TAG, "Null pointer during analysis", e)
            handler.post { onPrediction("", 0f) }
        } catch (e: Exception) {
            Log.e(TAG, "Error in analyze", e)
            handler.post { onPrediction("", 0f) }
        } finally {
            // Always close the image, even if there was an error
            try {
                image.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing image", e)
            }
        }
    }

    data class BoundingBox(
        val x1: Float, val y1: Float, val x2: Float, val y2: Float,
        val cx: Float, val cy: Float, val w: Float, val h: Float,
        val cnf: Float, val cls: Int, val clsName: String
    )

    private fun applyNMS(boxes: List<BoundingBox>): List<BoundingBox> {
        val sortedBoxes = boxes.sortedByDescending { it.cnf }.toMutableList()
        val selectedBoxes = mutableListOf<BoundingBox>()
        while (sortedBoxes.isNotEmpty()) {
            val first = sortedBoxes.removeAt(0)
            selectedBoxes.add(first)
            val iterator = sortedBoxes.iterator()
            while (iterator.hasNext()) {
                val nextBox = iterator.next()
                val iou = calculateIoU(first, nextBox)
                if (iou >= iouThreshold) {
                    iterator.remove()
                }
            }
        }
        return selectedBoxes
    }

    private fun calculateIoU(box1: BoundingBox, box2: BoundingBox): Float {
        val x1 = maxOf(box1.x1, box2.x1)
        val y1 = maxOf(box1.y1, box2.y1)
        val x2 = minOf(box1.x2, box2.x2)
        val y2 = minOf(box1.y2, box2.y2)
        val interArea = maxOf(0f, x2 - x1) * maxOf(0f, y2 - y1)
        val unionArea = (box1.x2 - box1.x1) * (box1.y2 - box1.y1) + (box2.x2 - box2.x1) * (box2.y2 - box2.y1) - interArea
        return if (unionArea > 0) interArea / unionArea else 0f
    }


    fun cleanup() {
        try {
            // Clear buffers to free memory
            inputBuffer.clear()
            inputArray.fill(0f)
            pixels.fill(0)
            
            // Clear output array
            for (batch in outputArray3D) {
                for (feat in batch) {
                    feat.fill(0f)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}

// Custom overlay view with static guide box at center
class MultiplayerOverlayView(context: Context) : View(context) {
    private val paintGuide = Paint().apply {
        color = android.graphics.Color.argb(150, 173, 216, 230) // Semi-transparent light blue
        style = Paint.Style.STROKE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f) // Dashed for guide
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        invalidate() // Ensure draw on layout
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        // Draw static guide box at center (40% of view size for hand fitting)
        val guideSize = 0.4f * min(width, height).toFloat()
        val guideLeft = (width / 2f - guideSize / 2f)
        val guideTop = (height / 2f - guideSize / 2f)
        val guideRight = guideLeft + guideSize
        val guideBottom = guideTop + guideSize
        canvas.drawRect(guideLeft, guideTop, guideRight, guideBottom, paintGuide)
    }
}
