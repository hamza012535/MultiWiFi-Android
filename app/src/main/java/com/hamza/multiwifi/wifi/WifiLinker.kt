package com.hamza.multiwifi.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import androidx.annotation.RequiresApi

sealed interface LinkState {
    data object Connecting : LinkState
    data class Connected(val network: Network) : LinkState
    data class Failed(val reason: String) : LinkState
}

/**
 * Asks Android to connect to a specific Wi-Fi network for this app. Android shows its own
 * approval prompt. On phones that can only hold one Wi-Fi connection, Android may drop the
 * current Wi-Fi to join this one; on phones with Wi-Fi concurrency it can stay alongside it.
 * The connection lasts only while this app holds the request.
 */
class WifiLinker(context: Context) {
    private val manager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val callbacks = HashMap<String, ConnectivityManager.NetworkCallback>()

    fun connect(ssid: String, security: WifiSecurity, passphrase: String, onState: (LinkState) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            onState(LinkState.Failed("Connecting to a chosen Wi-Fi network needs Android 10 or newer."))
            return
        }
        if (security == WifiSecurity.UNSUPPORTED) {
            onState(LinkState.Failed("Enterprise and WEP networks are not supported."))
            return
        }
        release(ssid)
        val request = try {
            buildRequest(ssid, security, passphrase)
        } catch (_: IllegalArgumentException) {
            onState(LinkState.Failed("Invalid network name or password."))
            return
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                onState(LinkState.Connected(network))
            }

            override fun onUnavailable() {
                onState(LinkState.Failed("Could not connect. It was declined, the password is wrong, or the network is out of range."))
                release(ssid)
            }

            override fun onLost(network: Network) {
                onState(LinkState.Failed("Connection lost."))
            }
        }
        callbacks[ssid] = callback
        onState(LinkState.Connecting)
        try {
            manager.requestNetwork(request, callback, REQUEST_TIMEOUT_MS)
        } catch (e: RuntimeException) {
            callbacks.remove(ssid)
            onState(LinkState.Failed("Android refused the request: ${e.message ?: e.javaClass.simpleName}"))
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun buildRequest(ssid: String, security: WifiSecurity, passphrase: String): NetworkRequest {
        val specifier = WifiNetworkSpecifier.Builder().setSsid(ssid).apply {
            when (security) {
                WifiSecurity.WPA2 -> setWpa2Passphrase(passphrase)
                WifiSecurity.WPA3 -> setWpa3Passphrase(passphrase)
                else -> Unit
            }
        }.build()
        return NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(specifier)
            .build()
    }

    fun release(ssid: String) {
        callbacks.remove(ssid)?.let {
            try { manager.unregisterNetworkCallback(it) } catch (_: IllegalArgumentException) { }
        }
    }

    fun releaseAll() {
        callbacks.keys.toList().forEach(::release)
    }

    private companion object {
        const val REQUEST_TIMEOUT_MS = 60_000
    }
}
