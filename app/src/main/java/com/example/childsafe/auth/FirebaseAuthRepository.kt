package com.example.childsafe.auth

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that handles Firebase phone authentication operations
 */
@Singleton
class FirebaseAuthRepository @Inject constructor() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    /**
     * Returns the current Firebase user or null if not logged in
     */
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    /**
     * Check if user is currently authenticated
     */
    fun isUserAuthenticated(): Boolean = auth.currentUser != null

    /**
     * Signs out the current user
     */
    fun signOut() = auth.signOut()

    /**
     * Initiates phone number verification process
     * 
     * @param phoneNumber Complete phone number with country code
     * @param activity The activity used for reCAPTCHA verification
     * @param callbacks Callbacks for verification events
     */
    fun verifyPhoneNumber(
        phoneNumber: String,
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    /**
     * Sign in with the phone credential
     * 
     * @param credential Phone auth credential
     * @return Flow containing Result with FirebaseUser on success or exception on failure
     */
    fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential): Flow<Result<FirebaseUser>> = callbackFlow {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        timber.log.Timber.d("Phone authentication successful for user: ${user.uid}")
                        trySend(Result.success(user))
                    } else {
                        timber.log.Timber.e("Authentication successful but user is null")
                        trySend(Result.failure(Exception("Xác thực thành công nhưng không thể lấy thông tin người dùng")))
                    }
                } else {
                    val exception = task.exception
                    timber.log.Timber.e("Phone authentication failed: ${exception?.message}")
                    
                    // Provide more user-friendly error messages
                    val errorMessage = when {
                        exception?.message?.contains("invalid") == true -> "Mã xác thực không hợp lệ. Vui lòng kiểm tra và thử lại."
                        exception?.message?.contains("expired") == true -> "Mã xác thực đã hết hạn. Vui lòng yêu cầu mã mới."
                        exception?.message?.contains("network") == true -> "Lỗi kết nối mạng. Vui lòng kiểm tra kết nối và thử lại."
                        else -> "Xác thực không thành công. Vui lòng thử lại sau."
                    }
                    
                    trySend(Result.failure(Exception(errorMessage)))
                }
            }
        awaitClose()
    }

    /**
     * Signs in with verification ID and SMS code
     * 
     * @param verificationId The verification ID from onCodeSent callback
     * @param code The verification code from SMS
     * @return Flow containing Result with FirebaseUser on success or exception on failure
     */
    fun signInWithCode(verificationId: String, code: String): Flow<Result<FirebaseUser>> {
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        return signInWithPhoneAuthCredential(credential)
    }

    /**
     * Set up debug logging for auth state changes
     */
    fun setupAuthStateListener() {
        auth.addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                // User is signed in
                timber.log.Timber.d("Auth State: User signed in: ${user.uid}, phone: ${user.phoneNumber}")
            } else {
                // User is signed out
                timber.log.Timber.d("Auth State: User signed out")
            }
        }
    }

    /**
     * Uses credential from onVerificationCompleted for automatic sign-in
     */
    suspend fun signInWithCredentialAsync(credential: PhoneAuthCredential): Result<FirebaseUser> {
        return try {
            timber.log.Timber.d("Attempting sign in with credential...")
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user != null) {
                // Log detailed information about the user
                timber.log.Timber.d("Sign in successful for user: ${user.uid}")
                timber.log.Timber.d("User details - isNew: ${authResult.additionalUserInfo?.isNewUser}")
                timber.log.Timber.d("User details - providerId: ${authResult.additionalUserInfo?.providerId}")
                timber.log.Timber.d("User details - phone: ${user.phoneNumber}")
                timber.log.Timber.d("User details - displayName: ${user.displayName}")
                timber.log.Timber.d("User details - phoneNumber: ${user.phoneNumber}")
                
                Result.success(user)
            } else {
                timber.log.Timber.e("Authentication successful but user is null")
                Result.failure(Exception("Xác thực thành công nhưng không thể lấy thông tin người dùng"))
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Sign in failed")
            
            // Provide more user-friendly error messages
            val errorMessage = when {
                e.message?.contains("invalid") == true -> "Mã xác thực không hợp lệ. Vui lòng kiểm tra và thử lại."
                e.message?.contains("expired") == true -> "Mã xác thực đã hết hạn. Vui lòng yêu cầu mã mới."
                e.message?.contains("network") == true -> "Lỗi kết nối mạng. Vui lòng kiểm tra kết nối và thử lại."
                else -> "Xác thực không thành công. Vui lòng thử lại sau."
            }
            
            Result.failure(Exception(errorMessage))
        }
    }
}
