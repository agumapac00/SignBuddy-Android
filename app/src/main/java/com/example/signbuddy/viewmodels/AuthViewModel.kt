package com.example.signbuddy.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.signbuddy.data.User
import com.example.signbuddy.data.UserType
import com.example.signbuddy.data.FirestoreService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestoreService = FirestoreService()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isRegistrationSuccessful = MutableStateFlow(false)
    val isRegistrationSuccessful: StateFlow<Boolean> = _isRegistrationSuccessful.asStateFlow()

    private val _isLoginSuccessful = MutableStateFlow(false)
    val isLoginSuccessful: StateFlow<Boolean> = _isLoginSuccessful.asStateFlow()

    init {
        _currentUser.value = auth.currentUser
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                android.util.Log.d("AuthViewModel", "Attempting login with email: $email")

                // Directly sign in with email and password
                val result = auth.signInWithEmailAndPassword(email, password).await()
                android.util.Log.d("AuthViewModel", "Sign in successful")
                
                _currentUser.value = result.user
                _isLoginSuccessful.value = true
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Login error", e)
                _errorMessage.value = when {
                    e.message?.contains("user not found") == true || e.message?.contains("no user") == true -> 
                        "No account found with this email. Please register first."
                    e.message?.contains("wrong password") == true || e.message?.contains("password is invalid") == true || e.message?.contains("INVALID_PASSWORD") == true -> 
                        "Incorrect password. Please try again."
                    e.message?.contains("network") == true || e.message?.contains("network error") == true -> 
                        "Network error. Please check your internet connection."
                    e.message?.contains("invalid") == true || e.message?.contains("malformed") == true ->
                        "Invalid email format. Please check your credentials."
                    else -> "Login failed: ${e.message ?: "Unknown error"}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(
        email: String,
        password: String,
        displayName: String,
        username: String,
        userType: UserType
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                // Check if username is available
                val isUsernameAvailable = firestoreService.isUsernameAvailable(username)
                if (!isUsernameAvailable.getOrNull()!!) {
                    _errorMessage.value = "Username is already taken. Please choose another one."
                    return@launch
                }

                // Create Firebase user
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user

                if (user != null) {
                    // Update user profile
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()
                    user.updateProfile(profileUpdates).await()

                    // Create user account in Firestore
                    val userAccount = User(
                        uid = user.uid,
                        email = email,
                        username = username,
                        displayName = displayName,
                        userType = userType,
                        createdAt = Date()
                    )

                    firestoreService.createUser(userAccount).getOrThrow()

                    // Create teacher profile if it's a teacher
                    if (userType == UserType.TEACHER) {
                        val teacherProfile = hashMapOf(
                            "uid" to user.uid,
                            "username" to username,
                            "displayName" to displayName,
                            "email" to email,
                            "schoolName" to "", // Will be updated separately
                            "createdAt" to Date()
                        )
                        val teacherResult = firestoreService.createTeacherProfile(teacherProfile).getOrNull()
                        if (teacherResult == null) {
                            android.util.Log.e("AuthViewModel", "Failed to create teacher profile")
                        } else {
                            android.util.Log.d("AuthViewModel", "Teacher profile created successfully for username: $username")
                        }
                    }

                    _currentUser.value = user
                    _isRegistrationSuccessful.value = true
                } else {
                    _errorMessage.value = "Failed to create user account"
                }
            } catch (e: Exception) {
                _errorMessage.value = when {
                    e.message?.contains("email address is already in use") == true -> 
                        "This email address is already registered. Please use a different email or try logging in instead."
                    e.message?.contains("password") == true -> 
                        "Password should be at least 6 characters long."
                    e.message?.contains("network") == true -> 
                        "Network error. Please check your internet connection."
                    e.message?.contains("username") == true -> 
                        "Username is already taken. Please choose a different username."
                    else -> "Registration failed: ${e.message ?: "Unknown error"}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _currentUser.value = null
        _errorMessage.value = null
        _isRegistrationSuccessful.value = false
        _isLoginSuccessful.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearUserState() {
        _currentUser.value = null
        _errorMessage.value = null
        _isRegistrationSuccessful.value = false
        _isLoginSuccessful.value = false
    }

    suspend fun getCurrentTeacherInfo(): Map<String, Any>? {
        val user = _currentUser.value
        return if (user != null) {
            try {
                firestoreService.findTeacherByUid(user.uid)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
}
