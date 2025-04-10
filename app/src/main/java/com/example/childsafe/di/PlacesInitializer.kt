package com.example.childsafe.di

import android.content.Context
import com.google.android.libraries.places.api.Places
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Singleton class to handle initialization of the Places SDK
 */
@Singleton
class PlacesInitializer @Inject constructor(@ApplicationContext private val context: Context) {

    /**
     * Initialize the Places SDK with the API key from the manifest
     */
    fun initialize() {
        try {
            if (!Places.isInitialized()) {
                Places.initialize(context, getApiKeyFromManifest())
                Timber.d("Places SDK initialized successfully")
            } else {
                Timber.d("Places SDK was already initialized")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error initializing Places SDK")
        }
    }

    /**
     * Get the API key from metadata in AndroidManifest.xml
     * You need to set com.google.android.geo.API_KEY in your manifest
     */
    private fun getApiKeyFromManifest(): String {
        val appInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            android.content.pm.PackageManager.GET_META_DATA
        )
        
        return appInfo.metaData.getString("com.google.android.geo.API_KEY") 
            ?: throw IllegalStateException(
                "Google Maps API key not found in AndroidManifest.xml. " +
                "Add <meta-data android:name=\"com.google.android.geo.API_KEY\" " +
                "android:value=\"YOUR_API_KEY\"/> to your manifest."
            )
    }
}