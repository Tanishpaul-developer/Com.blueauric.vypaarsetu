package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Real-time network connectivity monitor using Android [ConnectivityManager].
 * Emits true when the device has an active internet connection (validated),
 * and false when disconnected or losing network access.
 */
class NetworkConnectivityMonitor(private val context: Context) {

    companion object {
        private const val TAG = "ConnectivityMonitor"

        @Volatile
        private var instance: NetworkConnectivityMonitor? = null

        fun getInstance(context: Context): NetworkConnectivityMonitor {
            return instance ?: synchronized(this) {
                instance ?: NetworkConnectivityMonitor(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private val connectivityManager: ConnectivityManager? =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    /**
     * Checks synchronous internet reachability right now.
     */
    fun isConnected(): Boolean {
        val cm = connectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Observes real-time network connectivity changes as a reactive Flow.
     * Emits immediately with the current state, and thereafter on any network callback.
     */
    val isConnectedFlow: Flow<Boolean> = callbackFlow {
        val cm = connectivityManager
        if (cm == null) {
            trySend(false)
            close()
            return@callbackFlow
        }

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network available: $network")
                val isOnline = isNetworkValidated(cm, network)
                trySend(isOnline)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                Log.d(TAG, "Network capabilities changed: validated=$hasInternet")
                trySend(hasInternet)
            }

            override fun onLost(network: Network) {
                Log.w(TAG, "Network lost: $network")
                trySend(isConnected())
            }

            override fun onUnavailable() {
                Log.w(TAG, "Network unavailable")
                trySend(false)
            }
        }

        // Send initial connection state immediately
        trySend(isConnected())

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            cm.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering network callback: ${e.message}", e)
            trySend(isConnected())
        }

        awaitClose {
            try {
                cm.unregisterNetworkCallback(networkCallback)
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering network callback: ${e.message}")
            }
        }
    }.distinctUntilChanged()

    private fun isNetworkValidated(cm: ConnectivityManager, network: Network): Boolean {
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
