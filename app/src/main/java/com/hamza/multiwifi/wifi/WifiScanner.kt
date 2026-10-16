package com.hamza.multiwifi.wifi

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat

data class NearbyWifi(
    val ssid: String,
    /** Signal strength in dBm; closer to 0 is stronger. */
    val level: Int,
    val security: WifiSecurity
)

/**
 * Lists nearby Wi-Fi networks. Android throttles scans for apps, so [scanNow] may return the
 * previous results instead of starting a new scan.
 */
class WifiScanner(context: Context) {
    private val appContext = context.applicationContext
    private val wifi = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var receiver: BroadcastReceiver? = null

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun start(onResults: (List<NearbyWifi>) -> Unit) {
        stop()
        val r = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                onResults(readResults())
            }
        }
        ContextCompat.registerReceiver(
            appContext, r, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        receiver = r
        onResults(readResults())
        scanNow()
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    fun scanNow(): Boolean = try {
        wifi.startScan()
    } catch (_: SecurityException) {
        false
    }

    fun stop() {
        receiver?.let {
            try { appContext.unregisterReceiver(it) } catch (_: IllegalArgumentException) { }
        }
        receiver = null
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private fun readResults(): List<NearbyWifi> {
        if (!hasPermission()) return emptyList()
        val results = try {
            wifi.scanResults
        } catch (_: SecurityException) {
            return emptyList()
        }
        return results
            .filter { !it.SSID.isNullOrBlank() }
            .groupBy { it.SSID }
            .map { (ssid, group) ->
                val best = group.maxBy { it.level }
                NearbyWifi(ssid, best.level, securityOf(best.capabilities ?: ""))
            }
            .sortedByDescending { it.level }
    }
}
