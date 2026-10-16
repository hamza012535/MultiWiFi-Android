package com.hamza.multiwifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class NetworkRow(
    val id: String,
    val transport: String,
    val validated: Boolean,
    val metered: Boolean,
    val downKbps: Int?,
    val upKbps: Int?
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFF72E5C2),
                secondary = Color(0xFF9BC7FF),
                background = Color(0xFF08111F),
                surface = Color(0xFF111E30)
            )) {
                MultiWiFiScreen(applicationContext)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun MultiWiFiScreen(context: Context) {
    val manager = remember {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    val networks = remember { mutableStateListOf<NetworkRow>() }
    var lastUpdated by remember { mutableStateOf("Waiting for network information") }

    fun refreshSnapshot() {
        val current = manager.allNetworks.mapNotNull { network ->
            val caps = manager.getNetworkCapabilities(network) ?: return@mapNotNull null
            if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return@mapNotNull null
            caps.toRow(network)
        }
        networks.clear()
        networks.addAll(current.distinctBy { it.id })
        lastUpdated = "Network snapshot refreshed"
    }

    DisposableEffect(manager) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { refreshSnapshot() }
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) { refreshSnapshot() }
            override fun onLost(network: Network) { refreshSnapshot() }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        var registered = false
        try {
            manager.registerNetworkCallback(request, callback)
            registered = true
        } catch (_: RuntimeException) {
            lastUpdated = "Network monitoring unavailable; manual refresh remains available"
        }
        refreshSnapshot()
        onDispose {
            if (registered) {
                try { manager.unregisterNetworkCallback(callback) } catch (_: RuntimeException) { }
            }
        }
    }

    val wifiCount = networks.count { it.transport == "Wi-Fi" }
    val otherCount = networks.size - wifiCount

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF08111F), Color(0xFF12243A))))
            .padding(horizontal = 20.dp)
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Spacer(Modifier.height(24.dp))
                Text("MultiWiFi", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Network insights • early access", color = Color(0xFF9FB1C8), fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF142B3B))
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("CONNECTION OVERVIEW", color = Color(0xFF72E5C2), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(if (networks.isNotEmpty()) "Internet network detected" else "No internet network detected",
                            fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text("${networks.size} internet-capable network path(s) visible to this app",
                            color = Color(0xFFCAD7E7), fontSize = 14.sp)
                        Divider(color = Color(0xFF294356))
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            Metric(value = "$wifiCount", label = "Wi-Fi paths")
                            Metric(value = "$otherCount", label = "Other paths")
                        }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111E30))
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Aggregation status", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text(
                            if (networks.size > 1) "Multiple paths detected — bandwidth is NOT combined yet."
                            else "Waiting for additional network paths. Bandwidth is not aggregated.",
                            color = Color(0xFFFFD28A), fontSize = 14.sp
                        )
                        Text("True aggregation needs a compatible multipath client/server protocol and supported network paths.",
                            color = Color(0xFF9FB1C8), fontSize = 13.sp)
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Available paths", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Button(onClick = { refreshSnapshot() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Text(" Refresh")
                    }
                }
            }
            if (networks.isEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111E30))) {
                        Text(
                            "Android has not exposed an internet-capable network to this app yet. Check your connection and refresh.",
                            modifier = Modifier.padding(18.dp),
                            color = Color(0xFFCAD7E7)
                        )
                    }
                }
            } else {
                items(networks, key = { it.id }) { row -> NetworkCard(row) }
            }
            item {
                Text(lastUpdated, color = Color(0xFF71849B), fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Text("Privacy: this initial version reads network state only. It does not capture traffic, request Wi-Fi passwords, or start a VPN.",
                    color = Color(0xFF71849B), fontSize = 12.sp)
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

private fun NetworkCapabilities.toRow(network: Network): NetworkRow {
    val transport = when {
        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data"
        hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "Bluetooth"
        else -> "Other network"
    }
    return NetworkRow(
        id = network.toString(),
        transport = transport,
        validated = hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
        metered = !hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
        downKbps = linkDownstreamBandwidthKbps.takeIf { it > 0 },
        upKbps = linkUpstreamBandwidthKbps.takeIf { it > 0 }
    )
}

@androidx.compose.runtime.Composable
private fun Metric(value: String, label: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, fontSize = 12.sp, color = Color(0xFF9FB1C8))
    }
}

@androidx.compose.runtime.Composable
private fun NetworkCard(row: NetworkRow) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111E30))
    ) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(row.transport, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(if (row.validated) "Validated" else "Unverified",
                    color = if (row.validated) Color(0xFF72E5C2) else Color(0xFFFFD28A), fontSize = 12.sp)
            }
            Text("Estimated link bandwidth (Android hint, not a speed test)", color = Color(0xFF9FB1C8), fontSize = 12.sp)
            Text("↓ ${row.downKbps?.let(::formatKbps) ?: "Not reported"}    ↑ ${row.upKbps?.let(::formatKbps) ?: "Not reported"}",
                color = Color.White, fontSize = 14.sp)
            Text(if (row.metered) "Metered network" else "Reported as unmetered",
                color = Color(0xFF9FB1C8), fontSize = 12.sp)
        }
    }
}

private fun formatKbps(kbps: Int): String =
    if (kbps >= 1000) String.format("%.1f Mbps", kbps / 1000.0) else "$kbps Kbps"
