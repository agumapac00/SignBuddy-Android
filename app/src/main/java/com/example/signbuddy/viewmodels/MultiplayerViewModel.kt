package com.example.signbuddy.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.signbuddy.services.MultiplayerService
import com.example.signbuddy.services.MultiplayerRoom
import com.example.signbuddy.services.GameMessage
import com.example.signbuddy.services.PlayerAnswer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.UUID

// Data class for score updates
data class ScoreData(
    val playerId: String,
    val playerName: String,
    val score: Int,
    val timestamp: Long
)

// Data class to hold all multiplayer game state for the UI
data class MultiplayerGameState(
    val isHost: Boolean = false,
    val roomCode: String = "",
    val localPlayerId: String = "",
    val localPlayerName: String = "",
    val localPlayerScore: Int = 0,
    val opponentPlayerId: String = "",
    val opponentPlayerName: String = "",
    val opponentPlayerScore: Int = 0,
    val isConnected: Boolean = false, // True if local player is in a room
    val isWaitingForOpponent: Boolean = false,
    val opponentFound: Boolean = false,
    val gameStarted: Boolean = false,
    val gameFinished: Boolean = false,
    val currentQuestionIndex: Int = 0,
    val currentQuestionContent: String = "",
    val currentQuestionAnswer: String = "",
    val opponentCurrentAnswer: String = "",
    val opponentIsCorrect: Boolean = false
)

class MultiplayerViewModel : ViewModel() {
    private val multiplayerService = MultiplayerService()
    // Track awarded letters per question to prevent duplicate scoring
    private var lastQuestionIndex: Int = -1
    private val localAwardedLetters: MutableSet<String> = mutableSetOf()
    private val opponentAwardedLetters: MutableSet<String> = mutableSetOf()
    
    private val _gameState = MutableStateFlow(MultiplayerGameState())
    val gameState: StateFlow<MultiplayerGameState> = _gameState.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _room = MutableStateFlow<MultiplayerRoom?>(null)
    val room: StateFlow<MultiplayerRoom?> = _room.asStateFlow()
    
    fun createRoom(playerName: String) {
        viewModelScope.launch {
            Log.d("MultiplayerViewModel", "Creating room for player: $playerName")
            _isLoading.value = true
            _errorMessage.value = null
            val roomCode = generateRoomCode()

            // Immediately update UI state to show room code (defer localPlayerId until room is created)
            _gameState.update {
                it.copy(
                    isHost = true,
                    roomCode = roomCode,
                    localPlayerName = playerName,
                    isConnected = true,
                    isWaitingForOpponent = true
                )
            }
            Log.d("MultiplayerViewModel", "UI state updated with room code: $roomCode")

            multiplayerService.createRoomWithCode(roomCode, playerName)
                .onSuccess { room ->
                    Log.d("MultiplayerViewModel", "Room created successfully: $roomCode (hostId=${room.hostId})")
                    _room.value = room
                    // Ensure localPlayerId matches the room's hostId so opponent filtering works
                    _gameState.update { it.copy(localPlayerId = room.hostId) }
                    listenToRoom(roomCode)
                }
                .onFailure { e ->
                    Log.e("MultiplayerViewModel", "Failed to create room: ${e.message}", e)
                    _errorMessage.value = "Failed to create room: ${e.message}"
                    _gameState.update { MultiplayerGameState() } // Reset state on failure
                }
            _isLoading.value = false
        }
    }
    
    fun joinRoom(roomCode: String, playerName: String) {
        viewModelScope.launch {
            Log.d("MultiplayerViewModel", "Attempting to join room: $roomCode with player: $playerName")
            _isLoading.value = true
            _errorMessage.value = null
            
            multiplayerService.joinRoom(roomCode.uppercase(), playerName)
                .onSuccess { room ->
                    Log.d("MultiplayerViewModel", "Successfully joined room: $roomCode")
                    _room.value = room
                    _gameState.update {
                        it.copy(
                            isHost = false,
                            roomCode = room.roomCode,
                            isConnected = true,
                            isWaitingForOpponent = false,
                            opponentFound = true,
                            localPlayerName = room.joinerName ?: "",
                            localPlayerId = room.joinerId ?: "",
                            opponentPlayerName = room.hostName,
                            opponentPlayerId = room.hostId
                        )
                    }
                    listenToRoom(room.roomCode)
                }
                .onFailure { exception ->
                    Log.e("MultiplayerViewModel", "Failed to join room: ${exception.message}", exception)
                    _errorMessage.value = when {
                        exception.message?.contains("not found") == true -> "Room not found. Please check the room code."
                        exception.message?.contains("full") == true -> "Room is full. Please try another room."
                        exception.message?.contains("not active") == true -> "Room is not active. Please try another room."
                        else -> "Failed to join room: ${exception.message}"
                    }
                }
            _isLoading.value = false
        }
    }
    
    private fun listenToRoom(roomCode: String) {
        viewModelScope.launch {
            multiplayerService.listenToRoom(roomCode).collect { room ->
                if (room != null) {
                    _room.value = room
                    
                    // Update game state based on room changes
                    _gameState.update {
                        it.copy(
                            opponentFound = room.joinerId != null,
                            isWaitingForOpponent = room.joinerId == null && it.isHost,
                            gameStarted = room.gameState == "PLAYING",
                            currentQuestionIndex = room.currentQuestion,
                            gameFinished = room.gameState == "FINISHED"
                        )
                    }

                    // Reset awarded letters when question changes
                    if (room.currentQuestion != lastQuestionIndex) {
                        lastQuestionIndex = room.currentQuestion
                        localAwardedLetters.clear()
                        opponentAwardedLetters.clear()
                        Log.d("MultiplayerViewModel", "Question changed to index ${room.currentQuestion}. Cleared awarded letters.")
                    }
                    
                    // Update opponent info
                    if (!_gameState.value.isHost && room.joinerId != null) {
                        _gameState.update {
                            it.copy(
                                opponentPlayerName = room.hostName,
                                opponentPlayerId = room.hostId
                            )
                        }
                    } else if (_gameState.value.isHost) {
                        val joinerIdSnapshot = room.joinerId
                        if (joinerIdSnapshot != null) {
                            _gameState.update {
                                it.copy(
                                    opponentPlayerName = room.joinerName ?: "",
                                    opponentPlayerId = joinerIdSnapshot
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    fun listenToMessages(roomCode: String) {
        viewModelScope.launch {
            multiplayerService.listenToMessages(roomCode).collect { message ->
                handleGameMessage(message)
            }
        }
    }
    
    private fun handleGameMessage(message: GameMessage) {
        Log.d("MultiplayerViewModel", "=== MESSAGE RECEIVED ===")
        Log.d("MultiplayerViewModel", "Player: ${_gameState.value.localPlayerName}")
        Log.d("MultiplayerViewModel", "Message type: ${message.type}")
        Log.d("MultiplayerViewModel", "Message data: ${message.data}")
        Log.d("MultiplayerViewModel", "From player: ${message.playerName}")
        
        when (message.type) {
            "PLAYER_JOINED" -> {
                Log.d("MultiplayerViewModel", "Player joined message received")
                if (_gameState.value.isHost) {
                    _gameState.update {
                        it.copy(
                            opponentFound = true,
                            isWaitingForOpponent = false,
                            opponentPlayerName = message.playerName,
                            opponentPlayerId = message.playerId
                        )
                    }
                    Log.d("MultiplayerViewModel", "Updated opponent info for host")
                }
            }
            "PLAYER_LEFT" -> {
                if (message.playerId == _gameState.value.opponentPlayerId) {
                    _gameState.update {
                        it.copy(
                            opponentFound = false,
                            isWaitingForOpponent = it.isHost,
                            opponentPlayerName = "",
                            opponentPlayerId = ""
                        )
                    }
                }
            }
            "ANSWER_SUBMITTED" -> {
                Log.d("MultiplayerViewModel", "=== ANSWER SUBMITTED MESSAGE RECEIVED ===")
                Log.d("MultiplayerViewModel", "Message from: ${message.playerName} (${message.playerId})")
                Log.d("MultiplayerViewModel", "Message data: ${message.data}")
                Log.d("MultiplayerViewModel", "Current opponent ID: ${_gameState.value.opponentPlayerId}")
                
                // Parse answer data and update opponent scores and answers
                try {
                    val answerData = parseAnswerData(message.data)
                    Log.d("MultiplayerViewModel", "Parsed answer data: $answerData")
                    
                    if (answerData != null) {
                        // Check if this is from the opponent
                        val isFromOpponent = answerData.playerId == _gameState.value.opponentPlayerId
                        Log.d("MultiplayerViewModel", "Is from opponent: $isFromOpponent")
                        
                        if (isFromOpponent) {
                            val normalized = answerData.answer.trim().uppercase()
                            val alreadyAwarded = opponentAwardedLetters.contains(normalized)
                            if (!alreadyAwarded) opponentAwardedLetters.add(normalized)
                            val scoreGained = if (alreadyAwarded) 0 else calculateScore(answerData.isCorrect, answerData.responseTime)
                            Log.d("MultiplayerViewModel", "Calculated score gain: $scoreGained")
                            
                            _gameState.update { currentState ->
                                val newOpponentScore = currentState.opponentPlayerScore + scoreGained
                                Log.d("MultiplayerViewModel", "Updating opponent score: ${currentState.opponentPlayerScore} + $scoreGained = $newOpponentScore")
                                
                                currentState.copy(
                                    opponentPlayerScore = newOpponentScore,
                                    opponentCurrentAnswer = answerData.answer,
                                    opponentIsCorrect = answerData.isCorrect
                                )
                            }
                            
                            Log.d("MultiplayerViewModel", "=== OPPONENT SCORE UPDATED ===")
                            Log.d("MultiplayerViewModel", "Opponent: ${answerData.playerName}")
                            Log.d("MultiplayerViewModel", "Sign: '${answerData.answer}'")
                            Log.d("MultiplayerViewModel", "Score Gained: $scoreGained")
                            Log.d("MultiplayerViewModel", "New Opponent Total Score: ${_gameState.value.opponentPlayerScore}")
                            Log.d("MultiplayerViewModel", "=== DEVICE SYNC: Both devices now have same scores ===")
                        } else {
                            Log.d("MultiplayerViewModel", "Message from unknown player, ignoring")
                        }
                    } else {
                        Log.e("MultiplayerViewModel", "Failed to parse answer data")
                    }
                } catch (e: Exception) {
                    Log.e("MultiplayerViewModel", "Error parsing answer data: ${e.message}", e)
                }
            }
            "GAME_START" -> {
                _gameState.update { it.copy(gameStarted = true) }
                Log.d("MultiplayerViewModel", "Game started for player: ${_gameState.value.localPlayerName}")
            }
            "GAME_END" -> {
                _gameState.update { it.copy(gameFinished = true) }
            }
        }
    }
    
    private fun parseAnswerData(data: String): PlayerAnswer? {
        return try {
            Log.d("MultiplayerViewModel", "Parsing answer data: $data")
            // Simple parsing - in a real app you'd use proper JSON parsing
            // For now, we'll extract basic info from the data string
            val parts = data.split(",")
            if (parts.size >= 6) {
                val answerData = PlayerAnswer(
                    playerId = parts[0].substringAfter("playerId=").trim(),
                    playerName = parts[1].substringAfter("playerName=").trim(),
                    answer = parts[2].substringAfter("answer=").trim(),
                    isCorrect = parts[3].substringAfter("isCorrect=").trim().toBoolean(),
                    responseTime = parts[4].substringAfter("responseTime=").trim().toLong(),
                    timestamp = parts[5].substringAfter("timestamp=").trim().toLong()
                )
                Log.d("MultiplayerViewModel", "Parsed answer data successfully: $answerData")
                answerData
            } else {
                Log.e("MultiplayerViewModel", "Invalid answer data format: $data")
                null
            }
        } catch (e: Exception) {
            Log.e("MultiplayerViewModel", "Error parsing answer data: ${e.message}", e)
            null
        }
    }
    
    private fun parseScoreData(data: String): ScoreData? {
        return try {
            Log.d("MultiplayerViewModel", "Parsing score data: $data")
            val parts = data.split(",")
            if (parts.size >= 4) {
                val scoreData = ScoreData(
                    playerId = parts[0].substringAfter("playerId=").trim(),
                    playerName = parts[1].substringAfter("playerName=").trim(),
                    score = parts[2].substringAfter("score=").trim().toInt(),
                    timestamp = parts[3].substringAfter("timestamp=").trim().toLong()
                )
                Log.d("MultiplayerViewModel", "Parsed score data successfully: $scoreData")
                scoreData
            } else {
                Log.e("MultiplayerViewModel", "Invalid score data format: $data")
                null
            }
        } catch (e: Exception) {
            Log.e("MultiplayerViewModel", "Error parsing score data: ${e.message}", e)
            null
        }
    }
    
    fun startGame() {
        viewModelScope.launch {
            val roomCode = _gameState.value.roomCode
            if (roomCode.isNotEmpty() && _gameState.value.isHost) {
                multiplayerService.startGame(roomCode)
            }
        }
    }
    
    fun startGameForBothPlayers() {
        viewModelScope.launch {
            val roomCode = _gameState.value.roomCode
            if (roomCode.isNotEmpty()) {
                // Update local game state to started
                _gameState.update { it.copy(gameStarted = true) }
                
                // If host, also update the room state
                if (_gameState.value.isHost) {
                    multiplayerService.startGame(roomCode)
                }
                // Reset per-question tracking at game start
                lastQuestionIndex = -1
                localAwardedLetters.clear()
                opponentAwardedLetters.clear()
            }
        }
    }

    fun endGameForBothPlayers() {
        viewModelScope.launch {
            val roomCode = _gameState.value.roomCode
            if (roomCode.isNotEmpty()) {
                // Send GAME_END to room so both devices transition to finished
                multiplayerService.endGame(roomCode)
                // Update local state
                _gameState.update { it.copy(gameFinished = true, gameStarted = false) }
            }
        }
    }
    
    fun submitAnswer(answer: String, isCorrect: Boolean, responseTime: Long) {
        viewModelScope.launch {
            val roomCode = _gameState.value.roomCode
            val playerId = _gameState.value.localPlayerId
            val playerName = _gameState.value.localPlayerName
            
            Log.d("MultiplayerViewModel", "=== REAL-TIME ANSWER SUBMISSION ===")
            Log.d("MultiplayerViewModel", "Player: $playerName, Sign: '$answer'")
            
            if (roomCode.isNotEmpty() && playerId.isNotEmpty()) {
                // Send answer to other players for real-time sync
                multiplayerService.submitAnswer(roomCode, playerId, playerName, answer, isCorrect, responseTime)
                
                // Calculate score (10 points per letter) only once per unique letter per question
                val normalized = answer.trim().uppercase()
                val alreadyAwarded = localAwardedLetters.contains(normalized)
                if (!alreadyAwarded) localAwardedLetters.add(normalized)
                val scoreGained = if (alreadyAwarded) 0 else calculateScore(isCorrect, responseTime)
                
                // Update local state immediately
                _gameState.update { currentState ->
                    val newScore = currentState.localPlayerScore + scoreGained
                    Log.d("MultiplayerViewModel", "=== FORCE SCORE UPDATE ===")
                    Log.d("MultiplayerViewModel", "Old score: ${currentState.localPlayerScore}")
                    Log.d("MultiplayerViewModel", "Score gained: $scoreGained")
                    Log.d("MultiplayerViewModel", "New score: $newScore")
                    currentState.copy(localPlayerScore = newScore)
                }
                
                Log.d("MultiplayerViewModel", "Final synchronized score: ${_gameState.value.localPlayerScore}")
            } else {
                Log.e("MultiplayerViewModel", "Cannot submit answer - missing roomCode or playerId")
            }
        }
    }
    
    private fun calculateScore(isCorrect: Boolean, responseTime: Long, timeLimit: Int = 10): Int {
        // Letter-based scoring: 10 points per letter signed
        // No need to check isCorrect since all letter signing gives points
        val letterScore = 10
        
        Log.d("MultiplayerViewModel", "Letter-based scoring: $letterScore points per letter")
        return letterScore
    }
    
    fun leaveRoom() {
        viewModelScope.launch {
            val roomCode = _gameState.value.roomCode
            val playerId = _gameState.value.localPlayerId
            val playerName = _gameState.value.localPlayerName
            
            if (roomCode.isNotEmpty() && playerId.isNotEmpty()) {
                multiplayerService.leaveRoom(roomCode, playerId, playerName)
            }
            
            // Reset state
            _gameState.update { MultiplayerGameState() }
            _room.value = null
            lastQuestionIndex = -1
            localAwardedLetters.clear()
            opponentAwardedLetters.clear()
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun resetGame() {
        _gameState.update { MultiplayerGameState() }
        _room.value = null
        _errorMessage.value = null
    }
    
    fun clearOpponentAnswer() {
        _gameState.update {
            it.copy(
                opponentCurrentAnswer = "",
                opponentIsCorrect = false
            )
        }
    }
    
    fun validateScoreConsistency() {
        Log.d("MultiplayerViewModel", "=== SCORE VALIDATION ===")
        Log.d("MultiplayerViewModel", "Local Player: ${_gameState.value.localPlayerName}")
        Log.d("MultiplayerViewModel", "Local Score: ${_gameState.value.localPlayerScore}")
        Log.d("MultiplayerViewModel", "Opponent: ${_gameState.value.opponentPlayerName}")
        Log.d("MultiplayerViewModel", "Opponent Score: ${_gameState.value.opponentPlayerScore}")
        Log.d("MultiplayerViewModel", "Room Code: ${_gameState.value.roomCode}")
    }
    
    // Removed broadcastScoreUpdate to prevent score duplication
    // Scores are now synchronized through the answer submission mechanism
    
    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..5)
            .map { chars.random() }
            .joinToString("")
    }
}
