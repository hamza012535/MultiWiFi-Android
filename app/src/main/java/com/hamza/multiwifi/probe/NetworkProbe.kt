package com.hamza.multiwifi.probe

import android.net.Network
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.URL
import kotlin.coroutines.coroutineContext

/**
 * Settings for the per-network tests. Point HOST at an endpoint you control before shipping;
 * the default is a public speed-test endpoint so the feature works out of the box.
 */
object ProbeConfig {
    const val HOST = "speed.cloudflare.com"
    const val PORT = 443
    const val DOWNLOAD_BYTES = 2_000_000L
    const val LATENCY_SAMPLES = 5
    const val CONNECT_TIMEOUT_MS = 3_000
    const val READ_TIMEOUT_MS = 8_000
    /** Ignore results from transfers too small to give a meaningful rate. */
    const val MIN_THROUGHPUT_BYTES = 64 * 1024L

    val downloadUrl: String get() = "https://$HOST/__down?bytes=$DOWNLOAD_BYTES"
}

data class ProbeResult(
    val latency: LatencyStats?,
    val downloadMbps: Double?,
    val error: String?
)

sealed interface ProbeUiState {
    data object Running : ProbeUiState
    data class Done(val result: ProbeResult) : ProbeUiState
}

/**
 * Measures one network on its own by binding every socket to [network], so the result reflects
 * that path and not whichever network Android currently treats as default.
 */
suspend fun runProbe(network: Network): ProbeResult = withContext(Dispatchers.IO) {
    val latency = try {
        measureLatency(network)
    } catch (e: IOException) {
        return@withContext ProbeResult(null, null, "Latency test failed: ${e.message ?: e.javaClass.simpleName}")
    }

    val download = try {
        measureDownload(network)
    } catch (e: IOException) {
        return@withContext ProbeResult(latency, null, "Download test failed: ${e.message ?: e.javaClass.simpleName}")
    }

    val error = when {
        latency == null -> "No latency samples succeeded"
        download == null -> "Download too small or incomplete to report a rate"
        else -> null
    }
    ProbeResult(latency, download, error)
}

private suspend fun measureLatency(network: Network): LatencyStats? {
    // Resolve through this network's own DNS, not the default network's.
    val address = network.getAllByName(ProbeConfig.HOST).firstOrNull() ?: return null
    val samples = ArrayList<Double>(ProbeConfig.LATENCY_SAMPLES)
    var failures = 0
    repeat(ProbeConfig.LATENCY_SAMPLES) {
        coroutineContext.ensureActive()
        val socket = network.socketFactory.createSocket()
        try {
            val start = System.nanoTime()
            socket.connect(InetSocketAddress(address, ProbeConfig.PORT), ProbeConfig.CONNECT_TIMEOUT_MS)
            samples += (System.nanoTime() - start) / 1_000_000.0
        } catch (_: IOException) {
            failures++
        } finally {
            try { socket.close() } catch (_: IOException) { }
        }
    }
    return summarizeLatency(samples, failures)
}

private suspend fun measureDownload(network: Network): Double? {
    val connection = network.openConnection(URL(ProbeConfig.downloadUrl)) as HttpURLConnection
    try {
        connection.connectTimeout = ProbeConfig.CONNECT_TIMEOUT_MS
        connection.readTimeout = ProbeConfig.READ_TIMEOUT_MS
        connection.useCaches = false
        connection.requestMethod = "GET"
        if (connection.responseCode !in 200..299) {
            throw IOException("HTTP ${connection.responseCode}")
        }
        return connection.inputStream.use { input ->
            val buffer = ByteArray(32 * 1024)
            // Start the clock after the first bytes arrive so connection setup is not counted.
            if (input.read(buffer) < 0) return@use null
            val start = System.nanoTime()
            var total = 0L
            val cap = ProbeConfig.DOWNLOAD_BYTES * 2
            while (total < cap) {
                coroutineContext.ensureActive()
                val n = input.read(buffer)
                if (n < 0) break
                total += n
            }
            val elapsed = System.nanoTime() - start
            if (total < ProbeConfig.MIN_THROUGHPUT_BYTES) null else throughputMbps(total, elapsed)
        }
    } finally {
        connection.disconnect()
    }
}
