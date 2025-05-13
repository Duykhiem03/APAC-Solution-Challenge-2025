package com.example.childsafe.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class PhoneAuthViewModel @Inject constructor(
    private val authRepository: FirebaseAuthRepository,
    private val userRepository: com.example.childsafe.domain.repository.UserRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _verificationId = MutableStateFlow<String?>(null)
    private val _resendToken = MutableStateFlow<PhoneAuthProvider.ForceResendingToken?>(null)
    
    private var currentPhoneNumber: String = ""

    private val verificationCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // This callback will be invoked in two situations:
            // 1 - Instant verification. In some cases the phone number can be instantly
            //     verified without needing to send or enter a verification code.
            // 2 - Auto-retrieval. On some devices, Google Play services can automatically
            //     detect the incoming verification SMS and perform verification without
            //     user action.
            _authState.value = AuthState.Loading
            viewModelScope.launch {
                val result = authRepository.signInWithCredentialAsync(credential)
                result.fold(
                    onSuccess = { user ->
                        // Create or update user profile with phone number
                        try {
                            val phoneNum = if (currentPhoneNumber.isNotBlank()) {
                                currentPhoneNumber
                            } else {
                                user.phoneNumber ?: ""
                            }
                            
                            if (phoneNum.isNotBlank()) {
                                timber.log.Timber.d("Creating user profile after auto-verification with phone: $phoneNum")
                                
                                // Use a dedicated non-cancellable coroutine for profile creation
                                // This ensures the profile creation completes even if the parent coroutine is cancelled
                                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                                    try {
                                        var userProfile = userRepository.createUserAfterPhoneAuth(phoneNum)
                                        if (userProfile != null) {
                                            timber.log.Timber.d("User profile created successfully: $userProfile")
                                        } else {
                                            // If first attempt fails, wait a bit and try again
                                            timber.log.Timber.w("First attempt to create user profile failed during auto-verification, retrying...")
                                            kotlinx.coroutines.delay(1500)  // Wait 1.5 seconds
                                            
                                            userProfile = userRepository.createUserAfterPhoneAuth(phoneNum)
                                            if (userProfile != null) {
                                                timber.log.Timber.d("User profile created successfully on retry: $userProfile")
                                            } else {
                                                timber.log.Timber.w("Retry attempt to create user profile failed")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        timber.log.Timber.e(e, "Error in profile creation: ${e.message}")
                                    }
                                }
                            } else {
                                timber.log.Timber.e("No phone number available for profile creation during auto-verification")
                            }
                        } catch (e: Exception) {
                            timber.log.Timber.e(e, "Error creating user profile during auto-verification")
                        }
                        _authState.value = AuthState.Success(user)
                    },
                    onFailure = { exception ->
                        _authState.value = AuthState.Error(exception.message ?: "Authentication failed")
                    }
                )
            }
        }

        override fun onVerificationFailed(exception: FirebaseException) {
            // Called when verification fails (invalid phone number, etc)
            _authState.value = AuthState.Error(exception.message ?: "Verification failed")
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            // SMS verification code sent. Save ID and token for later use
            _verificationId.value = verificationId
            _resendToken.value = token
            _authState.value = AuthState.CodeSent
        }

        override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
            // Called when the timeout for auto-retrieval has been reached
            // User may need to manually enter the code
            _verificationId.value = verificationId
            _authState.value = AuthState.CodeSent
        }
    }

    /**
     * Starts phone number verification
     */
    fun verifyPhoneNumber(phoneNumber: String, activity: Activity) {
        if (phoneNumber.isBlank()) {
            _authState.value = AuthState.Error("Phone number cannot be empty")
            return
        }
        
        // Format phone number to ensure it has + prefix
        val formattedNumber = if (phoneNumber.startsWith("+")) phoneNumber else "+$phoneNumber"
        
        // Store the phone number for profile creation after successful auth
        currentPhoneNumber = formattedNumber
        
        _authState.value = AuthState.Loading
        authRepository.verifyPhoneNumber(formattedNumber, activity, verificationCallbacks)
    }

    /**
     * Submits the verification code received via SMS
     */
    fun submitVerificationCode(code: String) {
        val verificationId = _verificationId.value
        if (verificationId == null) {
            _authState.value = AuthState.Error("Verification ID not found. Please try again.")
            return
        }

        if (code.isBlank() || code.length < 6) {
            _authState.value = AuthState.Error("Please enter a valid verification code")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            authRepository.signInWithCode(verificationId, code).collect { result ->
                result.fold(
                    onSuccess = { user ->
                        // Create or update user profile with phone number
                        try {
                            val phoneNum = if (currentPhoneNumber.isNotBlank()) {
                                currentPhoneNumber
                            } else {
                                user.phoneNumber ?: ""
                            }
                            
                            if (phoneNum.isNotBlank()) {
                                timber.log.Timber.d("Creating user profile with phone: $phoneNum")
                                
                                // Use a dedicated non-cancellable coroutine for profile creation
                                // This ensures the profile creation completes even if the parent coroutine is cancelled
                                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                                    try {
                                        // Ensure the user is properly authenticated before trying to write to Firestore
                                        val currentUser = authRepository.getCurrentUser()
                                        if (currentUser == null) {
                                            timber.log.Timber.e("Cannot create profile: getCurrentUser() returned null")
                                        } else {
                                            timber.log.Timber.d("Authenticated user confirmed before profile creation: ${currentUser.uid}")

                                            // First attempt - with delay to ensure Firebase Auth is fully initialized
                                            kotlinx.coroutines.delay(1000)  // Wait 1 second for auth to fully initialize
                                            timber.log.Timber.d("Starting first attempt to create profile")
                                            var userProfile = userRepository.createUserAfterPhoneAuth(phoneNum)
                                            
                                            if (userProfile != null) {
                                                timber.log.Timber.d("User profile created successfully on first attempt: $userProfile")
                                            } else {
                                                // Second attempt with increased delay
                                                timber.log.Timber.w("First attempt to create user profile failed, retrying after delay...")
                                                kotlinx.coroutines.delay(3000)  // Wait 3 seconds
                                                
                                                // Recheck authentication to ensure it's still valid
                                                val recheckUser = authRepository.getCurrentUser()
                                                if (recheckUser != null) {
                                                    timber.log.Timber.d("Authentication still valid for second attempt")
                                                    userProfile = userRepository.createUserAfterPhoneAuth(phoneNum)
                                                    
                                                    if (userProfile != null) {
                                                        timber.log.Timber.d("User profile created successfully on second attempt: $userProfile")
                                                    } else {
                                                        timber.log.Timber.w("Second attempt to create user profile failed")
                                                    }
                                                } else {
                                                    timber.log.Timber.e("Auth user became null before second attempt")
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        timber.log.Timber.e(e, "Error in profile creation: ${e.message}")
                                        e.printStackTrace()
                                    }
                                    
                                    // Always emit success even if profile creation failed - user can update profile later
                                    timber.log.Timber.d("Proceeding with auth success regardless of profile creation result")
                                }
                            } else {
                                timber.log.Timber.e("No phone number available")
                            }
                        } catch (e: Exception) {
                            timber.log.Timber.e(e, "Error creating user profile")
                        }
                        
                        _authState.value = AuthState.Success(user)
                    },
                    onFailure = { exception ->
                        _authState.value = AuthState.Error(exception.message ?: "Failed to verify code")
                    }
                )
            }
        }
    }

    /**
     * Resends verification code if previous attempt timed out
     */
    fun resendVerificationCode(phoneNumber: String, activity: Activity) {
        val token = _resendToken.value ?: run {
            _authState.value = AuthState.Error("Cannot resend code at this time")
            return
        }

        _authState.value = AuthState.Loading
        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)            .setActivity(activity)
            .setCallbacks(verificationCallbacks)
            .setForceResendingToken(token)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    /**
     * Checks if user is authenticated
     */
    fun isUserAuthenticated(): Boolean = authRepository.isUserAuthenticated()

    /**
     * Sign out the current user
     */
    fun signOut() {
        authRepository.signOut()
        _authState.value = AuthState.Idle
    }

    /**
     * Get current user
     */
    fun getCurrentUser(): FirebaseUser? = authRepository.getCurrentUser()

    /**
     * Reset authentication state
     */
    fun resetState() {
        _authState.value = AuthState.Idle
    }
}

/**
 * Authentication states
 */
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object CodeSent : AuthState()
    data class Success(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}
