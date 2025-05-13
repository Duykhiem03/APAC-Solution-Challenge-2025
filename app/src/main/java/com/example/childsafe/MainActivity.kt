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
        
        // Log authentication state - using real phone authentication now
        // The mock auto-login with test@childsafe.com has been removed
        if (firebaseAuth.currentUser != null) {
            Timber.d("User already logged in: ${firebaseAuth.currentUser?.uid}")
        } else {
            Timber.d("No user logged in, will redirect to authentication screen")
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
