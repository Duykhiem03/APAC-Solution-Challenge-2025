package com.example.childsafe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.childsafe.ui.navigation.ChildSafeNavigation
import com.example.childsafe.ui.theme.ChildSafeTheme
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Main activity for the ChildSafe app
 * This is the entry point of the application
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var firebaseAuth: FirebaseAuth
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Temporary fix: Sign in with the test account you created in Firebase
        if (firebaseAuth.currentUser == null) {
            Timber.d("No user logged in, performing automatic test login")
            
            // Sign in with the test account you created in Firebase Console
            firebaseAuth.signInWithEmailAndPassword("test@childsafe.com", "testpassword123")
                .addOnSuccessListener {
                    Timber.d("Test login successful, UID: ${it.user?.uid}")
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "Test login failed: ${e.message}")
                    // Even if sign-in fails, the app will continue to run with the mock user ID
                }
        } else {
            Timber.d("User already logged in: ${firebaseAuth.currentUser?.uid}")
        }
        
        setContent {
            ChildSafeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChildSafeNavigation()
                }
            }
        }
    }
}
