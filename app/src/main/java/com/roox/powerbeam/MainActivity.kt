package com.roox.powerbeam

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var wifiManager: WifiManager
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WifiAdapter
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnScan: Button

    private val wifiList = mutableListOf<WifiScanResult>()
    private val handler = Handler(Looper.getMainLooper())
    private var scanRunnable: Runnable? = null

    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val results = wifiManager.scanResults
            displayNetworks(results)
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            runOnUiThread {
                tvStatus.text = "✅ Connected!"
                tvStatus.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.success))
            }
        }

        override fun onLost(network: Network) {
            runOnUiThread {
                tvStatus.text = "Disconnected"
                tvStatus.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
            }
        }

        override fun onUnavailable() {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "Connection failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        initViews()
        checkPermissions()
    }

    private fun initViews() {
        tvStatus = findViewById(R.id.tvStatus)
        progressBar = findViewById(R.id.progressBar)
        btnScan = findViewById(R.id.btnScan)
        recyclerView = findViewById(R.id.recyclerNetworks)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = WifiAdapter(wifiList) { network -> connectToNetwork(network) }
        recyclerView.adapter = adapter

        btnScan.setOnClickListener { scanWifi() }
    }

    private fun checkPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.NEARBY_WIFI_DEVICES
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            scanWifi()
        } else {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            scanWifi()
        } else {
            Toast.makeText(this, "Permissions required for WiFi scan", Toast.LENGTH_LONG).show()
        }
    }

    private fun scanWifi() {
        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(this, "Please enable WiFi", Toast.LENGTH_LONG).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        tvStatus.text = "Scanning..."
        wifiList.clear()
        adapter.notifyDataSetChanged()

        registerReceiver(wifiReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        wifiManager.startScan()

        scanRunnable?.let { handler.removeCallbacks(it) }
        scanRunnable = Runnable {
            unregisterReceiver(wifiReceiver)
            progressBar.visibility = View.GONE
            tvStatus.text = "Scan complete"
        }
        handler.postDelayed(scanRunnable!!, 5000)
    }

    private fun displayNetworks(results: List<ScanResult>) {
        wifiList.clear()
        for (result in results) {
            if (result.SSID.isNotBlank() && !result.SSID.startsWith("\"")) {
                wifiList.add(WifiScanResult(
                    ssid = result.SSID,
                    bssid = result.BSSID,
                    frequency = result.frequency,
                    level = result.level,
                    capabilities = result.capabilities,
                    isPowerBeam = result.frequency > 5000 || result.SSID.contains("ubnt", true) || result.SSID.contains("power", true)
                ))
            }
        }
        // Sort: PowerBeam first, then by signal strength
        wifiList.sortWith(compareByDescending<WifiScanResult> { it.isPowerBeam }.thenByDescending { it.level })
        adapter.notifyDataSetChanged()
        progressBar.visibility = View.GONE
        tvStatus.text = "Found ${wifiList.size} networks"
    }

    private fun connectToNetwork(network: WifiScanResult) {
        val password = showPasswordDialog(network.ssid) { pwd ->
            connectWithPassword(network, pwd)
        }
    }

    private fun showPasswordDialog(ssid: String, onConfirm: (String) -> Unit): String? {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter password for $ssid")
        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        builder.setView(input)
        builder.setPositiveButton("Connect") { _, _ ->
            onConfirm(input.text.toString())
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
        return null
    }

    private fun connectWithPassword(network: WifiScanResult, password: String) {
        tvStatus.text = "Connecting to ${network.ssid}..."

        val specifier = WifiNetworkSpecifier.Builder()
            .setSsid(network.ssid)
            .setBssid(android.net.MacAddress.fromString(network.bssid))
            .setWpa2Passphrase(password)
            .build()

        val request = NetworkRequest.Builder()
            .addTransportType(android.net.NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(specifier)
            .build()

        connectivityManager.requestNetwork(request, networkCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(wifiReceiver) } catch (_: Exception) {}
        scanRunnable?.let { handler.removeCallbacks(it) }
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}
