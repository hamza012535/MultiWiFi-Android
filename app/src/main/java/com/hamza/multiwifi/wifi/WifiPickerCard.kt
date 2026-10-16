package com.hamza.multiwifi.wifi

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val MAX_LISTED = 8

/**
 * Lets the user pick nearby Wi-Fi networks and ask Android to connect to them. Apps cannot read
 * the phone's saved-network list, so networks are chosen from the nearby scan.
 * [onLinkChanged] is called whenever a connection state changes so the dashboard can refresh.
 */
@Composable
fun WifiPickerCard(context: Context, onLinkChanged: () -> Unit) {
    val scanner = remember { WifiScanner(context) }
    val linker = remember { WifiLinker(context) }
    var hasPermission by remember { mutableStateOf(scanner.hasPermission()) }
    val nearby = remember { mutableStateListOf<NearbyWifi>() }
    val linkStates = remember { mutableStateMapOf<String, LinkState>() }
    var pending by remember { mutableStateOf<NearbyWifi?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }

    DisposableEffect(hasPermission) {
        if (hasPermission) {
            scanner.start { results ->
                nearby.clear()
                nearby.addAll(results)
            }
        }
        onDispose { scanner.stop() }
    }
    DisposableEffect(Unit) { onDispose { linker.releaseAll() } }

    fun connect(wifi: NearbyWifi, password: String) {
        linker.connect(wifi.ssid, wifi.security, password) { state ->
            linkStates[wifi.ssid] = state
            onLinkChanged()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111E30))
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Nearby Wi-Fi", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(
                "Pick networks to connect to. Whether two Wi-Fi networks can stay connected together depends on your phone. If it can't, Android may switch away from your current Wi-Fi.",
                color = Color(0xFF9FB1C8), fontSize = 12.sp
            )
            if (!hasPermission) {
                Text(
                    "Android needs location permission to list nearby Wi-Fi networks. This app does not read or store your location.",
                    color = Color(0xFFCAD7E7), fontSize = 13.sp
                )
                Button(onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
                    Text("Allow Wi-Fi scan")
                }
            } else {
                OutlinedButton(onClick = { scanner.scanNow() }) { Text("Scan again") }
                if (nearby.isEmpty()) {
                    Text(
                        "No networks found yet. Make sure Wi-Fi and location are turned on.",
                        color = Color(0xFFCAD7E7), fontSize = 13.sp
                    )
                }
                nearby.take(MAX_LISTED).forEach { wifi ->
                    Divider(color = Color(0xFF294356))
                    NearbyRow(wifi, linkStates[wifi.ssid]) {
                        if (wifi.security == WifiSecurity.OPEN) connect(wifi, "") else pending = wifi
                    }
                }
            }
        }
    }

    pending?.let { wifi ->
        var password by remember(wifi.ssid) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Connect to ${wifi.ssid}") },
            text = {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Wi-Fi password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = password.length >= 8,
                    onClick = { connect(wifi, password); pending = null }
                ) { Text("Connect") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun NearbyRow(wifi: NearbyWifi, state: LinkState?, onConnect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(wifi.ssid, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(
                "${wifi.level} dBm • ${securityLabel(wifi.security)}",
                color = Color(0xFF9FB1C8), fontSize = 12.sp
            )
            when (state) {
                LinkState.Connecting -> Text("Waiting for Android…", color = Color(0xFF9BC7FF), fontSize = 12.sp)
                is LinkState.Connected -> Text("Connected", color = Color(0xFF72E5C2), fontSize = 12.sp)
                is LinkState.Failed -> Text(state.reason, color = Color(0xFFFFD28A), fontSize = 12.sp)
                null -> Unit
            }
        }
        OutlinedButton(
            onClick = onConnect,
            enabled = wifi.security != WifiSecurity.UNSUPPORTED && state != LinkState.Connecting
        ) { Text(if (state is LinkState.Connected) "Reconnect" else "Connect") }
    }
}

private fun securityLabel(security: WifiSecurity): String = when (security) {
    WifiSecurity.OPEN -> "Open"
    WifiSecurity.WPA2 -> "WPA2"
    WifiSecurity.WPA3 -> "WPA3"
    WifiSecurity.UNSUPPORTED -> "Not supported"
}
