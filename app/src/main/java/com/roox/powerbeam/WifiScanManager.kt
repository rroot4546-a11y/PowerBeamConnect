package com.roox.powerbeam

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build

class WifiScanManager(
    private val context: Context,
    private val wifiManager: WifiManager,
    private val onScanResults: (List<WifiScanResult>) -> Unit,
    private val onScanError: (String) -> Unit
) {

    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            if (success) {
                val results = wifiManager.scanResults
                processScanResults(results)
            } else {
                onScanError("Scan failed")
            }
        }
    }

    fun startScan() {
        try {
            if (!wifiManager.isWifiEnabled) {
                onScanError("WiFi is disabled")
                return
            }

            val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(wifiReceiver, intentFilter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(wifiReceiver, intentFilter)
            }

            val success = wifiManager.startScan()
            if (!success) {
                onScanError("Failed to start WiFi scan")
            }
        } catch (e: Exception) {
            onScanError(e.message ?: "Unknown error during scan")
        }
    }

    fun stopScan() {
        try {
            context.unregisterReceiver(wifiReceiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
    }

    private fun processScanResults(results: List<ScanResult>) {
        val wifiResults = mutableListOf<WifiScanResult>()

        for (result in results) {
            if (result.SSID.isNotBlank() && !result.SSID.startsWith("\"")) {
                wifiResults.add(
                    WifiScanResult(
                        ssid = result.SSID,
                        bssid = result.BSSID,
                        frequency = result.frequency,
                        level = result.level,
                        capabilities = result.capabilities,
                        isPowerBeam = isPowerBeamNetwork(result),
                        signalStrength = calculateSignalStrength(result.level)
                    )
                )
            }
        }

        // Sort by PowerBeam first, then by signal strength
        wifiResults.sortWith(
            compareByDescending<WifiScanResult> { it.isPowerBeam }
                .thenByDescending { it.level }
        )

        onScanResults(wifiResults)
    }

    private fun isPowerBeamNetwork(result: ScanResult): Boolean {
        return result.frequency >= 5000 ||
                result.SSID.contains("ubnt", ignoreCase = true) ||
                result.SSID.contains("power", ignoreCase = true) ||
                result.SSID.contains("beam", ignoreCase = true)
    }

    private fun calculateSignalStrength(level: Int): String {
        return when {
            level >= -50 -> "Excellent"
            level >= -60 -> "Good"
            level >= -70 -> "Fair"
            level >= -80 -> "Weak"
            else -> "Very Weak"
        }
    }
}
