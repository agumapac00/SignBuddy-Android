package com.example.signbuddy.services

import android.util.Log
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

data class MultiplayerRoom(
    var roomCode: String = "",
    var hostId: String = "",
    var hostName: String = "",
    var joinerId: String? = null,
    var joinerName: String? = null,
    var isActive: Boolean = true,
    var createdAt: Long = 0L,
    var gameState: String = "WAITING", // WAITING, PLAYING, FINISHED
    var currentQuestion: Int = 0,
    var questionType: String = "LETTER"
) {
    // No-arg constructor required by Firebase
    constructor(): this("", "", "", null, null, true, 0L, "WAITING", 0, "LETTER")
}

data class PlayerAnswer(
    var playerId: String = "",
    var playerName: String = "",
    var answer: String = "",
    var isCorrect: Boolean = false,
    var responseTime: Long = 0L,
    var timestamp: Long = 0L
) {
    constructor(): this("", "", "", false, 0L, 0L)
}

data class GameMessage(
    var type: String = "", // PLAYER_JOINED, PLAYER_LEFT, ANSWER_SUBMITTED, GAME_START, GAME_END, QUESTION_CHANGE
    var playerId: String = "",
    var playerName: String = "",
    var data: String = "",
    var timestamp: Long = 0L
) {
    constructor(): this("", "", "", "", 0L)
}

class MultiplayerService {
    private val database = FirebaseDatabase.getInstance()
    private val roomsRef = database.getReference("multiplayer_rooms")
    private val messagesRef = database.getReference("multiplayer_messages")
    
    // Generate random 5-character room code
    fun generateRoomCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..5)
            .map { chars.random() }
            .joinToString("")
    }
    
    // Create a new room with server-generated code
    suspend fun createRoom(hostName: String): Result<MultiplayerRoom> {
        return try {
            val roomCode = generateRoomCode()
            val hostId = "host_${System.currentTimeMillis()}_${UUID.randomUUID().toString().substring(0, 8)}"
            
            val room = MultiplayerRoom(
                roomCode = roomCode,
                hostId = hostId,
                hostName = hostName
            )
            
            roomsRef.child(roomCode).setValue(room).await()
            
            // Set up room cleanup after 30 minutes
            Timer().schedule(object : TimerTask() {
                override fun run() {
                    roomsRef.child(roomCode).removeValue()
                }
            }, 30 * 60 * 1000) // 30 minutes
            
            Result.success(room)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Create a new room using a pre-generated code (so UI can show it immediately)
    suspend fun createRoomWithCode(roomCode: String, hostName: String): Result<MultiplayerRoom> {
        return try {
            Log.d("MultiplayerService", "Creating room with code: $roomCode")
            val hostId = "host_${System.currentTimeMillis()}_${UUID.randomUUID().toString().substring(0, 8)}"
            val room = MultiplayerRoom(
                roomCode = roomCode,
                hostId = hostId,
                hostName = hostName
            )
            
            Log.d("MultiplayerService", "Setting room data to Firebase...")
            roomsRef.child(roomCode).setValue(room).await()
            Log.d("MultiplayerService", "Room created successfully: $roomCode")

            // Auto-cleanup timer (same as default)
            Timer().schedule(object : TimerTask() {
                override fun run() {
                    Log.d("MultiplayerService", "Cleaning up room: $roomCode")
                    roomsRef.child(roomCode).removeValue()
                }
            }, 30 * 60 * 1000)

            Result.success(room)
        } catch (e: Exception) {
            Log.e("MultiplayerService", "Error creating room: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // Join an existing room
    suspend fun joinRoom(roomCode: String, joinerName: String): Result<MultiplayerRoom> {
        return try {
            Log.d("MultiplayerService", "Attempting to join room: $roomCode")
            val roomSnapshot = roomsRef.child(roomCode).get().await()
            
            if (!roomSnapshot.exists()) {
                Log.d("MultiplayerService", "Room not found: $roomCode")
                return Result.failure(Exception("Room not found"))
            }
            
            val room = roomSnapshot.getValue(MultiplayerRoom::class.java)
                ?: return Result.failure(Exception("Invalid room data"))
            
            if (room.joinerId != null) {
                Log.d("MultiplayerService", "Room is full: $roomCode")
                return Result.failure(Exception("Room is full"))
            }
            
            if (!room.isActive) {
                Log.d("MultiplayerService", "Room is not active: $roomCode")
                return Result.failure(Exception("Room is not active"))
            }
            
            val joinerId = "joiner_${System.currentTimeMillis()}_${UUID.randomUUID().toString().substring(0, 8)}"
            val updatedRoom = room.copy(
                joinerId = joinerId,
                joinerName = joinerName
            )
            
            Log.d("MultiplayerService", "Updating room with joiner: $roomCode")
            roomsRef.child(roomCode).setValue(updatedRoom).await()
            
            // Send player joined message
            sendMessage(roomCode, GameMessage(
                type = "PLAYER_JOINED",
                playerId = joinerId,
                playerName = joinerName,
                data = "Player joined the room"
            ))
            
            Log.d("MultiplayerService", "Successfully joined room: $roomCode")
            Result.success(updatedRoom)
        } catch (e: Exception) {
            Log.e("MultiplayerService", "Error joining room: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // Listen to room changes
    fun listenToRoom(roomCode: String): Flow<MultiplayerRoom?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val room = snapshot.getValue(MultiplayerRoom::class.java)
                trySend(room)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        roomsRef.child(roomCode).addValueEventListener(listener)
        
        awaitClose {
            roomsRef.child(roomCode).removeEventListener(listener)
        }
    }
    
    // Listen to game messages
    fun listenToMessages(roomCode: String): Flow<GameMessage> = callbackFlow {
        Log.d("MultiplayerService", "=== STARTING MESSAGE LISTENER ===")
        Log.d("MultiplayerService", "Room: $roomCode")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("MultiplayerService", "=== MESSAGE RECEIVED ===")
                Log.d("MultiplayerService", "Snapshot children count: ${snapshot.children.count()}")
                
                snapshot.children.forEach { child ->
                    val message = child.getValue(GameMessage::class.java)
                    if (message != null) {
                        Log.d("MultiplayerService", "Processing message: ${message.type} from ${message.playerName}")
                        trySend(message)
                    }
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("MultiplayerService", "Message listener cancelled: ${error.message}")
                close(error.toException())
            }
        }
        
        messagesRef.child(roomCode).addValueEventListener(listener)
        Log.d("MultiplayerService", "Message listener added for room: $roomCode")
        
        awaitClose {
            Log.d("MultiplayerService", "Closing message listener for room: $roomCode")
            messagesRef.child(roomCode).removeEventListener(listener)
        }
    }
    
    // Send a game message
    suspend fun sendMessage(roomCode: String, message: GameMessage) {
        try {
            Log.d("MultiplayerService", "=== SENDING MESSAGE ===")
            Log.d("MultiplayerService", "Room: $roomCode, Type: ${message.type}, From: ${message.playerName}")
            Log.d("MultiplayerService", "Data: ${message.data}")
            
            messagesRef.child(roomCode).child(message.timestamp.toString()).setValue(message).await()
            
            Log.d("MultiplayerService", "Message sent successfully to room: $roomCode")
        } catch (e: Exception) {
            Log.e("MultiplayerService", "Error sending message: ${e.message}", e)
        }
    }
    
    // Submit an answer
    suspend fun submitAnswer(roomCode: String, playerId: String, playerName: String, answer: String, isCorrect: Boolean, responseTime: Long) {
        Log.d("MultiplayerService", "=== SUBMITTING ANSWER ===")
        Log.d("MultiplayerService", "Room: $roomCode, Player: $playerName, Answer: $answer, Correct: $isCorrect")
        
        val answerData = PlayerAnswer(
            playerId = playerId,
            playerName = playerName,
            answer = answer,
            isCorrect = isCorrect,
            responseTime = responseTime,
            timestamp = System.currentTimeMillis()
        )
        
        // Format answer data as a parseable string
        val dataString = "playerId=$playerId,playerName=$playerName,answer=$answer,isCorrect=$isCorrect,responseTime=$responseTime,timestamp=${answerData.timestamp}"
        
        Log.d("MultiplayerService", "Formatted data string: $dataString")
        
        sendMessage(roomCode, GameMessage(
            type = "ANSWER_SUBMITTED",
            playerId = playerId,
            playerName = playerName,
            data = dataString,
            timestamp = System.currentTimeMillis()
        ))
        
        Log.d("MultiplayerService", "Answer message sent successfully")
    }
    
    // Update game state
    suspend fun updateGameState(roomCode: String, gameState: String, currentQuestion: Int = 0) {
        try {
            val updates = mapOf(
                "gameState" to gameState,
                "currentQuestion" to currentQuestion
            )
            roomsRef.child(roomCode).updateChildren(updates).await()
        } catch (e: Exception) {
            // Handle error silently for now
        }
    }
    
    // Start the game
    suspend fun startGame(roomCode: String) {
        updateGameState(roomCode, "PLAYING", 0)
        sendMessage(roomCode, GameMessage(
            type = "GAME_START",
            playerId = "system",
            playerName = "System",
            data = "Game started!"
        ))
    }
    
    // End the game
    suspend fun endGame(roomCode: String) {
        updateGameState(roomCode, "FINISHED")
        sendMessage(roomCode, GameMessage(
            type = "GAME_END",
            playerId = "system",
            playerName = "System",
            data = "Game ended!"
        ))
    }
    
    // Leave the room
    suspend fun leaveRoom(roomCode: String, playerId: String, playerName: String) {
        try {
            // Send leave message
            sendMessage(roomCode, GameMessage(
                type = "PLAYER_LEFT",
                playerId = playerId,
                playerName = playerName,
                data = "Player left the room"
            ))
            
            // If it's the host leaving, close the room
            val roomSnapshot = roomsRef.child(roomCode).get().await()
            val room = roomSnapshot.getValue(MultiplayerRoom::class.java)
            
            if (room?.hostId == playerId) {
                roomsRef.child(roomCode).removeValue()
            } else if (room?.joinerId == playerId) {
                // Remove joiner from room
                val updatedRoom = room.copy(joinerId = null, joinerName = null)
                roomsRef.child(roomCode).setValue(updatedRoom).await()
            }
        } catch (e: Exception) {
            // Handle error silently for now
        }
    }
    
    // Clean up old messages (keep only last 100 messages per room)
    suspend fun cleanupMessages(roomCode: String) {
        try {
            val snapshot = messagesRef.child(roomCode).get().await()
            val messages = snapshot.children.toList()
            
            if (messages.size > 100) {
                val messagesToDelete = messages.take(messages.size - 100)
                messagesToDelete.forEach { child ->
                    child.ref.removeValue()
                }
            }
        } catch (e: Exception) {
            // Handle error silently for now
        }
    }
}
