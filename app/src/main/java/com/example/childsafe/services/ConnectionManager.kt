package com.example.childsafe.services

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service to monitor and manage network connectivity
 * Supports both system connectivity checks and active connectivity validation
 */
@Singleton
class ConnectionManager @Inject constructor(
    @ApplicationContext context: Context
) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    // StateFlow for observing current network state
    private val _connectionState = MutableStateFlow(isNetworkAvailable())
    val connectionState: StateFlow<Boolean> = _connectionState
    
    // Scope for background operations
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // URLs for connectivity validation
    private val validationUrls = listOf(
        "https://www.google.com",
        "https://www.apple.com",
        "https://www.microsoft.com",
        "https://www.amazon.com"
    )
    
    /**
     * Private list of connectivity listeners
     */
    private val networkRestoreListeners = mutableListOf<() -> Unit>()
    
    /**
     * Register a listener to be notified when network is restored
     * @param listener Function to call when network is restored
     */
    fun registerNetworkRestoreListener(listener: () -> Unit) {
        synchronized(networkRestoreListeners) {
            networkRestoreListeners.add(listener)
        }
    }
    
    /**
     * Unregister a previously registered network restore listener
     * @param listener Function that was previously registered
     */
    fun unregisterNetworkRestoreListener(listener: () -> Unit) {
        synchronized(networkRestoreListeners) {
            networkRestoreListeners.remove(listener)
        }
    }
    
    /**
     * Notify all registered listeners that network has been restored
     */
    private fun notifyNetworkRestored() {
        synchronized(networkRestoreListeners) {
            Timber.d("Notifying ${networkRestoreListeners.size} listeners about network restoration")
            networkRestoreListeners.forEach { listener ->
                try {
                    listener()
                } catch (e: Exception) {
                    Timber.e(e, "Error in network restore listener")
                }
            }
        }
    }
    
    // Initialize the connection manager
    init {
        registerNetworkCallback()
        startPeriodicConnectivityCheck()
    }
    
    /**
     * Register a network callback to monitor connectivity changes
     */
    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
            
        try {
            connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Timber.d("Network connection available")
                    managerScope.launch {
                        // When a connection becomes available, validate it
                        if (validateActiveConnection()) {
                            val previousState = _connectionState.value
                            _connectionState.value = true
                            
                            // If the state changed from offline to online, trigger immediate message retry
                            if (!previousState) {
                                notifyNetworkRestored()
                            }
                        }
                    }
                }
                
                override fun onLost(network: Network) {
                    Timber.d("Network connection lost")
                    _connectionState.value = false
                }
                
                override fun onUnavailable() {
                    Timber.d("Network connection unavailable")
                    _connectionState.value = false
                }
            })
        } catch (e: Exception) {
            Timber.e(e, "Error registering network callback")
            _connectionState.value = false
        }
    }
    
    /**
     * Start periodic connectivity check
     * This helps detect "captive portals" and other situations where the system
     * thinks it's connected but actual internet connectivity is not available
     */
    private fun startPeriodicConnectivityCheck() {
        managerScope.launch {
            while (true) {
                try {
                    // If system thinks we're connected, validate the connection
                    if (isNetworkAvailable() && _connectionState.value) {
                        val isReallyConnected = validateActiveConnection()
                        
                        if (!isReallyConnected && _connectionState.value) {
                            Timber.d("Internet connectivity validation failed")
                            _connectionState.value = false
                        } else if (isReallyConnected && !_connectionState.value) {
                            Timber.d("Internet connectivity restored")
                            _connectionState.value = true
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error in periodic connectivity check")
                }
                
                // Check every 60 seconds
                delay(60000)
            }
        }
    }
    
    /**
     * Check if network is currently available according to system
     */
    fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && 
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * Validate internet connectivity by making a real network request
     * This is more reliable than just checking system network state
     */
    private suspend fun validateActiveConnection(): Boolean {
        // If system reports no connectivity, don't bother checking
        if (!isNetworkAvailable()) {
            return false
        }
        
        return try {
            // Try connecting to multiple hosts for reliability
            for (urlString in validationUrls) {
                try {
                    val url = URL(urlString)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
                    connection.instanceFollowRedirects = false
                    connection.useCaches = false
                    connection.connect()
                    
                    val responseCode = connection.responseCode
                    connection.disconnect()
                    
                    // If any connection succeeds, we have connectivity
                    if (responseCode == 200 || responseCode == 301 || responseCode == 302) {
                        Timber.d("Connectivity validated with $urlString")
                        return true
                    }
                } catch (e: IOException) {
                    // Try the next URL
                    continue
                }
            }
            
            // All connection attempts failed
            Timber.d("All connectivity validation attempts failed")
            false
        } catch (e: Exception) {
            Timber.e(e, "Error validating connectivity")
            false
        }
    }
    
    /**
     * Observe network connectivity changes as a Flow
     */
    fun observeConnectivity(): Flow<Boolean> = callbackFlow {
        // Send initial state
        trySend(isNetworkAvailable())
        
        // Create network callback
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }
            
            override fun onLost(network: Network) {
                trySend(false)
            }
            
            override fun onUnavailable() {
                trySend(false)
            }
        }
        
        // Register callback
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
            
        connectivityManager.registerNetworkCallback(request, callback)
        
        // Clean up when flow is cancelled
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}
