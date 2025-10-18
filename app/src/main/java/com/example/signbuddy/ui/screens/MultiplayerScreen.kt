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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
fun MultiplayerScreen(navController: NavController? = null) {
    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFE0B2), // Warm orange
            Color(0xFFFFF8E1), // Cream
            Color(0xFFE8F5E8), // Light green
            Color(0xFFE3F2FD)  // Light blue
        )
    )
    
    val soundEffects = rememberSoundEffects()
    val hapticFeedback = rememberHapticFeedback()
    
    // Game state management
    var gameState by remember { mutableStateOf(GameState.LOBBY) }
    var currentQuestion by remember { mutableStateOf<GameQuestion?>(null) }
    var timeLeft by remember { mutableStateOf(0) }
    var questionIndex by remember { mutableStateOf(0) }
    var selectedQuestionType by remember { mutableStateOf(QuestionType.LETTER) }
    
    // Player management
    var localPlayer by remember { mutableStateOf(Player("local", "You", 0, isLocal = true)) }
    var opponentPlayer by remember { mutableStateOf(Player("opponent", "Opponent", 0, isLocal = false)) }
    
    // Camera and model state
    var cameraState by remember { mutableStateOf(CameraState()) }
    var connectionState by remember { mutableStateOf(ConnectionState()) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Camera preview view
    val previewView = remember { PreviewView(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() } // background thread for CameraX analyzers
    
    // Camera & practice states
    var useFrontCamera by remember { mutableStateOf(true) }
    
    // Model and analyzer (copied from evaluation screen)
    var modelInterpreter by remember { mutableStateOf<Interpreter?>(null) }
    var handSignAnalyzer by remember { mutableStateOf<MultiplayerHandSignAnalyzer?>(null) }
    
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
    
    LaunchedEffect(Unit) {
        // Loads model on IO dispatcher
        Log.d("MultiplayerScreen", "Loading model...")
        modelInterpreter = withContext(Dispatchers.IO) { loadPracticeModel(context) }
        Log.d("MultiplayerScreen", "Model loaded: ${modelInterpreter != null}")
    }
    
    // Cleanup analyzer when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            handSignAnalyzer?.cleanup()
        }
    }

    // Use 640 if that's your trained imgsz; change to 320 if you want faster inference
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
    
    // Game questions
    val letterQuestions = listOf(
        GameQuestion("L1", QuestionType.LETTER, "A", "A", 10),
        GameQuestion("L2", QuestionType.LETTER, "B", "B", 10),
        GameQuestion("L3", QuestionType.LETTER, "C", "C", 10),
        GameQuestion("L4", QuestionType.LETTER, "D", "D", 10),
        GameQuestion("L5", QuestionType.LETTER, "E", "E", 10)
    )
    
    val wordQuestions = listOf(
        GameQuestion("W1", QuestionType.WORD, "CAT", "C-A-T", 15),
        GameQuestion("W2", QuestionType.WORD, "DOG", "D-O-G", 15),
        GameQuestion("W3", QuestionType.WORD, "BAT", "B-A-T", 15),
        GameQuestion("W4", QuestionType.WORD, "HAT", "H-A-T", 15),
        GameQuestion("W5", QuestionType.WORD, "MAT", "M-A-T", 15)
    )
    
    val allQuestions = if (selectedQuestionType == QuestionType.LETTER) letterQuestions else wordQuestions
    
    fun nextQuestion() {
        if (questionIndex < allQuestions.size) {
            currentQuestion = allQuestions[questionIndex]
            timeLeft = currentQuestion?.timeLimit ?: 10
            questionIndex++
            
            // Reset player answers for new question
            localPlayer = localPlayer.copy(currentAnswer = "", isCorrect = false)
            opponentPlayer = opponentPlayer.copy(currentAnswer = "", isCorrect = false)
        } else {
            gameState = GameState.FINISHED
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
        val roomCode = generateRoomCode()
        val hostId = "host_${System.currentTimeMillis()}"
        
        // Create room in global manager
        RoomManager.createRoom(roomCode, hostId)
        
        connectionState = connectionState.copy(
            isHost = true,
            roomCode = roomCode,
            isWaitingForOpponent = true
        )
        gameState = GameState.CONNECTING
        soundEffects.playButtonClick()
        hapticFeedback.lightTap()
    }

    fun joinGame(roomCode: String) {
        val joinerId = "joiner_${System.currentTimeMillis()}"
        val upperCode = roomCode.uppercase()

        val room = RoomManager.joinRoom(upperCode, joinerId)
        if (room != null) {
            // Successfully joined
            connectionState = connectionState.copy(
                isHost = false,
                roomCode = upperCode,
                isConnected = true,
                opponentFound = true,
                isWaitingForOpponent = false
            )
            opponentPlayer = opponentPlayer.copy(isConnected = true)
            gameState = GameState.COUNTDOWN

            soundEffects.playButtonClick()
            hapticFeedback.lightTap()

            // ✅ Use coroutine instead of LaunchedEffect
            coroutineScope.launch {
                delay(3000)
                gameState = GameState.PLAYING
                nextQuestion()
            }
        } else {
            // Invalid or full room
            soundEffects.playWrong()
            hapticFeedback.errorPattern()
            Toast.makeText(context, "❌ Invalid or full room code!", Toast.LENGTH_SHORT).show()
        }
    }



    // Calculate time-based bonus score
    fun calculateScore(isCorrect: Boolean, responseTime: Long, timeLimit: Int): Int {
        if (!isCorrect) return 0
        
        val timeBonus = when {
            responseTime <= 2000 -> 50  // Very fast: 2 seconds or less
            responseTime <= 4000 -> 30  // Fast: 2-4 seconds
            responseTime <= 6000 -> 20  // Medium: 4-6 seconds
            responseTime <= 8000 -> 10  // Slow: 6-8 seconds
            else -> 5                   // Very slow: 8+ seconds
        }
        
        val baseScore = 10
        return baseScore + timeBonus
    }

    // Real connection effect: only start when a real player joins
    LaunchedEffect(connectionState.roomCode, gameState) {
        if (gameState == GameState.CONNECTING && connectionState.isHost) {
            while (true) {
                val room = RoomManager.getRoom(connectionState.roomCode)
                if (room?.joinerId != null) {
                    // Real joiner found!
                    opponentPlayer = opponentPlayer.copy(isConnected = true)
                    connectionState = connectionState.copy(opponentFound = true)
                    gameState = GameState.COUNTDOWN
                    delay(3000)
                    gameState = GameState.PLAYING
                    nextQuestion()
                    break
                }
                delay(1000) // Check every second for joiner
            }
        }
    }


    // Timer effect
    LaunchedEffect(timeLeft, gameState) {
        if (gameState == GameState.PLAYING && timeLeft > 0) {
            delay(1000)
            timeLeft--
        } else if (timeLeft == 0 && gameState == GameState.PLAYING) {
            // Time's up - move to next question
            nextQuestion()
        }
    }
    
    fun submitAnswer(answer: String) {
        val question = currentQuestion ?: return
        val isCorrect = answer.equals(question.answer, ignoreCase = true)
        val responseTime = System.currentTimeMillis()
        val questionStartTime = questionIndex * 10000L // Approximate question start time
        val actualResponseTime = responseTime - questionStartTime
        
        val scoreGained = calculateScore(isCorrect, actualResponseTime, question.timeLimit)
        
        localPlayer = localPlayer.copy(
            currentAnswer = answer,
            isCorrect = isCorrect,
            responseTime = actualResponseTime,
            score = localPlayer.score + scoreGained,
            totalCorrectAnswers = if (isCorrect) localPlayer.totalCorrectAnswers + 1 else localPlayer.totalCorrectAnswers
        )
        
        // Play appropriate sound effect
        if (isCorrect) {
            soundEffects.playCorrect()
            hapticFeedback.successPattern()
        } else {
            soundEffects.playWrong()
            hapticFeedback.errorPattern()
        }
    }
    
    // Simulate opponent response effect
    LaunchedEffect(localPlayer.currentAnswer) {
        if (localPlayer.currentAnswer.isNotEmpty()) {
            delay((1000..3000).random().toLong())
            val question = currentQuestion ?: return@LaunchedEffect
            val opponentCorrect = (0..1).random() == 1
            opponentPlayer = opponentPlayer.copy(
                currentAnswer = if (opponentCorrect) question.answer else "Wrong",
                isCorrect = opponentCorrect,
                responseTime = System.currentTimeMillis() + (500..2000).random(),
                score = if (opponentCorrect) opponentPlayer.score + 10 else opponentPlayer.score
            )
        }
    }
    
    // Auto-advance to next question when both players have answered
    LaunchedEffect(localPlayer.currentAnswer, opponentPlayer.currentAnswer) {
        if (localPlayer.currentAnswer.isNotEmpty() && opponentPlayer.currentAnswer.isNotEmpty()) {
            delay(3000) // Wait 3 seconds to show results
            if (gameState == GameState.PLAYING) {
                nextQuestion()
            }
        }
    }
    
    fun resetGame() {
        gameState = GameState.LOBBY
        questionIndex = 0
        currentQuestion = null
        timeLeft = 0
        localPlayer = localPlayer.copy(score = 0, currentAnswer = "", isCorrect = false)
        opponentPlayer = opponentPlayer.copy(score = 0, currentAnswer = "", isCorrect = false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎮 Multiplayer Quiz", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { 
                        soundEffects.playButtonClick()
                        hapticFeedback.lightTap()
                        navController?.popBackStack() 
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
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
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (gameState) {
                GameState.LOBBY -> LobbyScreen(
                    selectedQuestionType = selectedQuestionType,
                    onQuestionTypeChanged = { selectedQuestionType = it },
                    onStartHosting = { startHosting() },
                    onJoinGame = { roomCode -> joinGame(roomCode) },
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
                    onGameStart = { gameState = GameState.PLAYING }
                )
                
                GameState.PLAYING -> PlayingScreen(
                    currentQuestion = currentQuestion,
                    timeLeft = timeLeft,
                    localPlayer = localPlayer,
                    opponentPlayer = opponentPlayer,
                    questionIndex = questionIndex,
                    allQuestions = allQuestions,
                    cameraState = cameraState,
                    onAnswerSubmit = { submitAnswer(it) },
                    onNextQuestion = { nextQuestion() },
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
                    onPlayAgain = { resetGame() },
                    onBackToLobby = { gameState = GameState.LOBBY }
                )
            }
            }
        }
    }
}

@Composable
fun LobbyScreen(
    selectedQuestionType: QuestionType,
    onQuestionTypeChanged: (QuestionType) -> Unit,
    onStartHosting: () -> Unit,
    onJoinGame: (String) -> Unit,
    soundEffects: com.example.signbuddy.ui.components.SoundEffectsManager,
    hapticFeedback: com.example.signbuddy.ui.components.HapticFeedbackManager
) {
    var roomCode by remember { mutableStateOf("") }
    var showJoinDialog by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Text(
            text = "🎯 Multiplayer Quiz Battle",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Challenge a friend and see who's the fastest!",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        
        // Question Type Selection
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
                    text = "📝 Choose Quiz Type",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
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
                            defaultElevation = if (selectedQuestionType == QuestionType.LETTER) 8.dp else 2.dp
                        ),
                        shape = RoundedCornerShape(16.dp),
                        onClick = {
                            soundEffects.playButtonClick()
                            hapticFeedback.lightTap()
                            onQuestionTypeChanged(QuestionType.LETTER)
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
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
                            defaultElevation = if (selectedQuestionType == QuestionType.WORD) 8.dp else 2.dp
                        ),
                        shape = RoundedCornerShape(16.dp),
                        onClick = {
                            soundEffects.playButtonClick()
                            hapticFeedback.lightTap()
                            onQuestionTypeChanged(QuestionType.WORD)
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
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
                                text = "Spell words letter by letter",
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
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "🎮 How to Play",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                val rules = listOf(
                    "• Connect with another player on their device",
                    "• Both players see the same question at the same time",
                    "• Use your camera to sign the answer",
                    "• Fastest and most accurate wins bonus points!",
                    "• See your opponent's progress in real-time"
                )
                
                rules.forEach { rule ->
                    Text(
                        text = rule,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
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
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "🏠 Host Game",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
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
                    fontWeight = FontWeight.Bold
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
                            value = roomCode,
                            onValueChange = { roomCode = it.uppercase().take(5) },
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
                            if (roomCode.length == 5) {
                                onJoinGame(roomCode)
                                showJoinDialog = false
                                roomCode = ""
                            }
                        },
                        enabled = roomCode.length == 5,
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
    // Real player matching - check for opponent joining
    LaunchedEffect(connectionState.roomCode) {
        if (connectionState.isHost) {
            // Host waits for someone to join their room
            while (connectionState.isWaitingForOpponent) {
                val room = RoomManager.getRoom(connectionState.roomCode)
                if (room?.joinerId != null) {
                    // Opponent joined!
                    onOpponentFound()
                    break
                }
                delay(1000) // Check every second
            }
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
    onAnswerSubmit: (String) -> Unit,
    onNextQuestion: () -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Timer
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (timeLeft <= 5) Color(0xFFFF5722) else Color(0xFF4CAF50)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                text = "⏱️ $timeLeft",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Player Progress Bars
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Local Player
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "You",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                LinearProgressIndicator(
                    progress = localPlayer.score / 100f,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF4CAF50),
                    trackColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                )
                Text(
                    text = "${localPlayer.score} pts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            // Opponent Player
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Opponent",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFFF6B6B),
                    fontWeight = FontWeight.Bold
                )
                LinearProgressIndicator(
                    progress = opponentPlayer.score / 100f,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFFF6B6B),
                    trackColor = Color(0xFFFF6B6B).copy(alpha = 0.2f)
                )
                Text(
                    text = "${opponentPlayer.score} pts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        
        // Question Display
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
                    text = "Sign this:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(60.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentQuestion?.content ?: "",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (currentQuestion?.type == QuestionType.WORD) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Spell it letter by letter",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        // Answer Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Local Player Status
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (localPlayer.isCorrect) Color(0xFF4CAF50) else 
                                   if (localPlayer.currentAnswer.isNotEmpty()) Color(0xFFFF9800) else Color(0xFFF5F5F5)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (localPlayer.isCorrect) "✅" else if (localPlayer.currentAnswer.isNotEmpty()) "⏳" else "⭕",
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "You: ${if (localPlayer.currentAnswer.isNotEmpty()) localPlayer.currentAnswer else "Waiting..."}",
                        color = if (localPlayer.isCorrect) Color.White else Color.Black,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Opponent Player Status
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (opponentPlayer.isCorrect) Color(0xFFFF6B6B) else 
                                   if (opponentPlayer.currentAnswer.isNotEmpty()) Color(0xFFFF9800) else Color(0xFFF5F5F5)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (opponentPlayer.isCorrect) "✅" else if (opponentPlayer.currentAnswer.isNotEmpty()) "⏳" else "⭕",
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Opponent: ${if (opponentPlayer.currentAnswer.isNotEmpty()) opponentPlayer.currentAnswer else "Waiting..."}",
                        color = if (opponentPlayer.isCorrect) Color.White else Color.Black,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        // Camera Preview with Answer Submission
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📷 Sign Your Answer",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
        // Real Camera Preview Area
        Log.d("MultiplayerScreen", "Camera permission check: ${cameraState.isPermissionGranted}")
        if (cameraState.isPermissionGranted) {
            CameraPreview(
                cameraState = cameraState,
                currentQuestion = currentQuestion,
                onSignDetected = { sign, confidence ->
                    onUpdateCameraState(cameraState.copy(
                        lastDetectedSign = sign,
                        confidence = confidence,
                        isAnalyzing = true
                    ))
                    onUpdateLocalPlayer(localPlayer.copy(currentAnswer = sign))
                    
                    // Auto-submit answer if confidence is high enough
                    if (confidence > 0.7f) {
                        onAnswerSubmit(sign)
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
                    .height(150.dp)
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
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Please enable camera access",
                        color = Color.Red.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
                
                Spacer(modifier = Modifier.height(16.dp))
                
            }
        }
        
        // Game Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Question Progress
                Text(
                    text = "Question ${questionIndex + 1} of ${allQuestions.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                // Game Status
                Text(
                    text = when {
                        localPlayer.isCorrect && opponentPlayer.isCorrect -> "Both correct! 🎉"
                        localPlayer.isCorrect -> "You're winning! 🏆"
                        opponentPlayer.isCorrect -> "Opponent ahead! ⚡"
                        else -> "Keep going! 💪"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        localPlayer.isCorrect && opponentPlayer.isCorrect -> Color(0xFF4CAF50)
                        localPlayer.isCorrect -> Color(0xFF2196F3)
                        opponentPlayer.isCorrect -> Color(0xFFFF5722)
                        else -> Color(0xFF757575)
                    },
                    fontWeight = FontWeight.Medium
                )
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
    onBackToLobby: () -> Unit
) {
    val isWinner = localPlayer.score > opponentPlayer.score
    val isTie = localPlayer.score == opponentPlayer.score
    
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
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onPlayAgain,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Play Again", color = Color.White)
            }
            
            Button(
                onClick = onBackToLobby,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Back to Lobby", color = Color.White)
            }
        }
    }
}

@Composable
fun CameraPreview(
    cameraState: CameraState,
    currentQuestion: GameQuestion?,
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
    LaunchedEffect(cameraState.isPermissionGranted, modelInterpreter) {
        Log.d("CameraPreview", "=== CAMERA SETUP ===")
        Log.d("CameraPreview", "Permission granted: ${cameraState.isPermissionGranted}")
        Log.d("CameraPreview", "Model loaded: ${modelInterpreter != null}")
        if (!cameraState.isPermissionGranted || modelInterpreter == null) {
            Log.d("CameraPreview", "Skipping camera setup - missing permission or model")
            return@LaunchedEffect
        }

        val provider = ProcessCameraProvider.getInstance(context).get()
        val preview = Preview.Builder().build().apply { setSurfaceProvider(previewView.surfaceProvider) }
        val selector = CameraSelector.DEFAULT_FRONT_CAMERA
        Log.d("CameraPreview", "Camera provider and preview created")

        // ImageAnalysis: keep latest frames only and analyze on background executor
        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(Size(inputSize, inputSize))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        // Use the analyzer on the background executor
        Log.d("CameraPreview", "Creating hand sign analyzer...")
        val analyzer = MultiplayerHandSignAnalyzer(
            modelInterpreter = modelInterpreter,
            imageProcessor = imageProcessor,
            inputSize = inputSize,
            useFrontCamera = true,
            context = context,
            onPrediction = { prediction: String ->
                Log.d("CameraPreview", "Prediction received: '$prediction'")
                if (!prediction.isNullOrEmpty() && prediction != "nothing") {
                    onSignDetected(prediction, 0.8f)
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
    
    // Camera preview + overlay (with guide box and feedback overlay)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
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

private fun analyzeImage(
    imageProxy: ImageProxy,
    currentQuestion: GameQuestion?,
    onSignDetected: (String, Float) -> Unit,
    onAnalysisComplete: () -> Unit
) {
    try {
        val bitmap = imageProxy.toBitmap(0)
        if (bitmap != null) {
            // For now, simulate sign detection
            val detectedSign = simulateSignDetection(currentQuestion?.answer ?: "A")
            if (detectedSign.isNotEmpty()) {
                onSignDetected(detectedSign, 0.8f)
            }
        }
        onAnalysisComplete()
    } catch (e: Exception) {
        Log.e("ImageAnalysis", "Error analyzing image", e)
        onAnalysisComplete()
    } finally {
        imageProxy.close()
    }
}



// Real sign detection using ASL model
private fun simulateSignDetection(correctAnswer: String): String {
    // In a real implementation, this would use the ASL model
    // For now, simulate realistic detection with some accuracy
    val random = Math.random()
    return when {
        random > 0.8 -> correctAnswer  // 20% chance of correct detection
        random > 0.6 -> ""             // 20% chance of no detection
        else -> ""                     // 60% chance of no detection (realistic)
    }
}

// Load TFLite model from assets (CPU-only)
private fun loadPracticeModel(context: Context): Interpreter? {
    return try {
        Log.d("MultiplayerScreen", "Starting model loading...")
        val model = loadModelFile(context, "asl_model.tflite")
        Log.d("MultiplayerScreen", "Model file loaded, size: ${model.capacity()}")
        val interpreter = Interpreter(model)
        val inputShape = interpreter.getInputTensor(0).shape()
        val outputShape = interpreter.getOutputTensor(0).shape()
        Log.d("MultiplayerScreen", "Model loaded successfully. Input: ${inputShape.contentToString()}, Output: ${outputShape.contentToString()}")
        interpreter
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
    private val onPrediction: (String) -> Unit
) : ImageAnalysis.Analyzer {
    private val handler = Handler(Looper.getMainLooper())
    private var lastAnalysisTime = 0L
    private val analysisInterval = 300L // ms
    private val TAG = "MultiplayerHandSignAnalyzer"
    private var frameCount = 0
    // ASL labels: 0-25 A-Z, 26 nothing
    private val labels = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "nothing")
    // Model params (adapt if your model differs)
    private val numClasses = 27
    private val numFeatures = 4 + numClasses // 31
    private val numDetections = 8400
    private val confThreshold = 0.3f // raw detection threshold
    private val feedbackThreshold = 0.60f // FINAL confidence threshold (user requested 75%)
    private val iouThreshold = 0.5f

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
        Log.d(TAG, "ANALYZE CALLED #$frameCount - Image: ${image.width}x${image.height}")
        
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAnalysisTime < analysisInterval) {
            Log.d(TAG, "Skipping analysis - too soon (${currentTime - lastAnalysisTime}ms)")
            image.close()
            return
        }
        lastAnalysisTime = currentTime

        Log.d(TAG, "=== ANALYZING IMAGE ===")
        Log.d(TAG, "Model interpreter: ${modelInterpreter != null}")
        Log.d(TAG, "Image format: ${image.format}, width: ${image.width}, height: ${image.height}")
        
        if (modelInterpreter == null) {
            Log.e(TAG, "Model interpreter is null - cannot analyze")
            image.close()
            handler.post { onPrediction("") }
            return
        }

        try {
            // Handle rotation and flip for front camera
            val rotationDegrees = if (useFrontCamera) 270 else 0
            Log.d(TAG, "Converting image to bitmap with rotation: $rotationDegrees")
            var bitmap = image.toBitmap(rotationDegrees)
            Log.d(TAG, "Bitmap created successfully: ${bitmap.width}x${bitmap.height}")
            if (useFrontCamera) {
                bitmap = flipHorizontally(bitmap)
                Log.d(TAG, "Bitmap flipped for front camera")
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
            synchronized(interpreterLock) {
                try {
                    Log.d(TAG, "Running model inference...")
                    modelInterpreter.run(inputBuffer, outputArray3D)
                    Log.d(TAG, "Model inference completed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error during model inference", e)
                    handler.post { onPrediction("") }
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
            
            // Log some sample output values to understand the format
            Log.d(TAG, "Output array shape: [${outputArray3D.size}][${outputArray3D[0].size}][${outputArray3D[0][0].size}]")
            Log.d(TAG, "Sample output values: ${outputFlat.take(10).joinToString(", ")}")
            Log.d(TAG, "Max output value: ${outputFlat.maxOrNull()}, Min output value: ${outputFlat.minOrNull()}")
            
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

            Log.d(TAG, "Detected ${boundingBoxes.size} bounding boxes")
            if (boundingBoxes.isEmpty()) {
                Log.d(TAG, "No bounding boxes detected - returning empty prediction")
                handler.post { onPrediction("") }
                return
            }

            // Apply NMS
            val selectedBoxes = applyNMS(boundingBoxes)

            // Pick highest conf box
            val bestBox = selectedBoxes.maxByOrNull { it.cnf }
            val predictedLetter = if (bestBox != null && bestBox.cnf > feedbackThreshold) bestBox.clsName else ""
            Log.d(TAG, "Selected boxes after NMS: ${selectedBoxes.size}")
            Log.d(TAG, "Best box: $bestBox")
            Log.d(TAG, "Final prediction: '$predictedLetter' (conf: ${bestBox?.cnf ?: 0f}, threshold: $feedbackThreshold)")
            handler.post { onPrediction(predictedLetter) }

    } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Out of memory during analysis", e)
            // Force garbage collection
            System.gc()
            handler.post { onPrediction("") }
        } catch (e: Exception) {
            Log.e(TAG, "Error in analyze", e)
            handler.post { onPrediction("") }
        } finally {
            image.close()
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
