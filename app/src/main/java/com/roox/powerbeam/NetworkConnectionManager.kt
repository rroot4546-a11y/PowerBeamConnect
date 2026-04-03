package com.roox.powerbeam

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.util.Log

class NetworkConnectionManager(
    private val context: Context,
    private val connectivityManager: ConnectivityManager,
    private val onConnectionStateChanged: (Boolean, String) -> Unit
) {

    private var currentNetworkCallback: ConnectivityManager.NetworkCallback? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.d("NetworkConnectionManager", "Network available")
            onConnectionStateChanged(true, "Connected successfully")
        }

        override fun onCapabilitiesChanged(network: Network, nc: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, nc)
            Log.d("NetworkConnectionManager", "Capabilities changed")
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            Log.d("NetworkConnectionManager", "Network lost")
            onConnectionStateChanged(false, "Connection lost")
        }

        override fun onUnavailable() {
            super.onUnavailable()
            Log.d("NetworkConnectionManager", "Network unavailable")
            onConnectionStateChanged(false, "Connection failed - Network unavailable")
        }
    }

    fun connectToNetwork(network: WifiScanResult, password: String) {
        try {
            // Remove previous callback if exists
            currentNetworkCallback?.let { callback ->
                try {
                    connectivityManager.unregisterNetworkCallback(callback)
                } catch (e: Exception) {
                    Log.e("NetworkConnectionManager", "Error unregistering callback", e)
                }
            }

            val macAddress = try {
                android.net.MacAddress.fromString(network.bssid)
            } catch (e: Exception) {
                Log.e("NetworkConnectionManager", "Invalid MAC address: ${network.bssid}", e)
                onConnectionStateChanged(false, "Invalid network MAC address")
                return
            }

            val specifier = WifiNetworkSpecifier.Builder()
                .setSsid(network.ssid)
                .setBssid(macAddress)

            // Determine security type and set passphrase
            when {
                network.capabilities.contains("WPA3") -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        specifier.setWpa3Passphrase(password)
                    } else {
                        specifier.setWpa2Passphrase(password)
                    }
                }
                network.capabilities.contains("WPA2") -> {
                    specifier.setWpa2Passphrase(password)
                }
                network.capabilities.contains("WPA") -> {
                    specifier.setWpa2Passphrase(password)
                }
                network.capabilities.contains("WEP") -> {
                    // WEP is deprecated and not supported by WifiNetworkSpecifier
                    specifier.setWpa2Passphrase(password)
                }
                else -> {
                    // Open network
                }
            }

            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier.build())
                .build()

            currentNetworkCallback = networkCallback
            connectivityManager.requestNetwork(request, networkCallback)

            Log.d("NetworkConnectionManager", "Connection request sent to ${network.ssid}")
            onConnectionStateChanged(false, "Connecting to ${network.ssid}...")

        } catch (e: Exception) {
            Log.e("NetworkConnectionManager", "Connection error", e)
            onConnectionStateChanged(false, "Connection error: ${e.message}")
        }
    }

    fun disconnect() {
        currentNetworkCallback?.let { callback ->
            try {
                connectivityManager.unregisterNetworkCallback(callback)
                onConnectionStateChanged(false, "Disconnected")
            } catch (e: Exception) {
                Log.e("NetworkConnectionManager", "Error disconnecting", e)
            }
        }
    }
}
