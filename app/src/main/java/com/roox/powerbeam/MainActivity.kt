package com.roox.powerbeam

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
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
    private var scanTimeout: Runnable? = null

    private lateinit var wifiScanManager: WifiScanManager
    private lateinit var networkConnectionManager: NetworkConnectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        initializeManagers()
        initViews()
        checkAndRequestPermissions()
    }

    private fun initializeManagers() {
        wifiScanManager = WifiScanManager(
            context = this,
            wifiManager = wifiManager,
            onScanResults = { results ->
                runOnUiThread {
                    displayNetworks(results)
                }
            },
            onScanError = { error ->
                runOnUiThread {
                    Toast.makeText(this, "Scan error: $error", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                    tvStatus.text = "Scan failed: $error"
                }
            }
        )

        networkConnectionManager = NetworkConnectionManager(
            context = this,
            connectivityManager = connectivityManager,
            onConnectionStateChanged = { isConnected, message ->
                runOnUiThread {
                    tvStatus.text = message
                    tvStatus.setTextColor(
                        ContextCompat.getColor(
                            this,
                            if (isConnected) R.color.success else R.color.primary
                        )
                    )
                }
            }
        )
    }

    private fun initViews() {
        tvStatus = findViewById(R.id.tvStatus)
        progressBar = findViewById(R.id.progressBar)
        btnScan = findViewById(R.id.btnScan)
        recyclerView = findViewById(R.id.recyclerNetworks)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = WifiAdapter(wifiList) { network ->
            showPasswordDialog(network)
        }
        recyclerView.adapter = adapter

        btnScan.setOnClickListener {
            scanWifi()
        }

        tvStatus.text = getString(R.string.status_scanning)
    }

    private fun checkAndRequestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.NEARBY_WIFI_DEVICES,
                Manifest.permission.CHANGE_WIFI_STATE
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CHANGE_WIFI_STATE
            )
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (missingPermissions.isEmpty()) {
            scanWifi()
        } else {
            ActivityCompat.requestPermissions(this, missingPermissions, REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_CODE_PERMISSIONS -> {
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    scanWifi()
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.error_permissions),
                        Toast.LENGTH_LONG
                    ).show()
                    tvStatus.text = "Permissions denied"
                }
            }
        }
    }

    private fun scanWifi() {
        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(this, getString(R.string.error_wifi_disabled), Toast.LENGTH_SHORT)
                .show()
            return
        }

        progressBar.visibility = View.VISIBLE
        tvStatus.text = getString(R.string.status_scanning)
        wifiList.clear()
        adapter.notifyDataSetChanged()

        wifiScanManager.startScan()

        // Set timeout for scan
        scanTimeout?.let { handler.removeCallbacks(it) }
        scanTimeout = Runnable {
            progressBar.visibility = View.GONE
            tvStatus.text = getString(R.string.status_scan_complete)
            wifiScanManager.stopScan()
        }
        handler.postDelayed(scanTimeout!!, SCAN_TIMEOUT_MS)
    }

    private fun displayNetworks(results: List<WifiScanResult>) {
        wifiList.clear()
        wifiList.addAll(results)
        adapter.notifyDataSetChanged()

        progressBar.visibility = View.GONE
        tvStatus.text = getString(R.string.found_networks, results.size)

        if (results.isEmpty()) {
            Toast.makeText(this, "No networks found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPasswordDialog(network: WifiScanResult) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.dialog_password_title, network.ssid))

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.hint = "Enter network password"

        val container = FrameLayout(this)
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.marginStart = 16
        params.marginEnd = 16
        input.layoutParams = params
        container.addView(input)

        builder.setView(container)
        builder.setPositiveButton(getString(R.string.btn_connect)) { _, _ ->
            val password = input.text.toString()
            if (password.isNotEmpty()) {
                networkConnectionManager.connectToNetwork(network, password)
            } else {
                Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton(getString(R.string.btn_cancel), null)
        builder.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        wifiScanManager.stopScan()
        networkConnectionManager.disconnect()
        scanRunnable?.let { handler.removeCallbacks(it) }
        scanTimeout?.let { handler.removeCallbacks(it) }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 100
        private const val SCAN_TIMEOUT_MS = 8000L
    }
}
