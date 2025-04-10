package com.example.childsafe

import android.app.Application
import com.example.childsafe.data.model.Destination
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Main application class that initializes Dagger Hilt and other app-wide components
 */
@HiltAndroidApp
class ChildSafeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}




