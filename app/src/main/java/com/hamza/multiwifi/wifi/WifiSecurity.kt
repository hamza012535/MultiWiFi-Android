package com.hamza.multiwifi.wifi

enum class WifiSecurity { OPEN, WPA2, WPA3, UNSUPPORTED }

/**
 * Maps a scan result's capabilities string (for example "[WPA2-PSK-CCMP][ESS]") to the security
 * type this app can connect with. Enterprise (EAP), WEP and Enhanced Open (OWE) networks are not
 * supported.
 */
fun securityOf(capabilities: String): WifiSecurity = when {
    // WPA2/WPA3 transition networks list both; WPA2 works for them.
    capabilities.contains("PSK") -> WifiSecurity.WPA2
    capabilities.contains("SAE") -> WifiSecurity.WPA3
    capabilities.contains("EAP") || capabilities.contains("WEP") || capabilities.contains("OWE") ->
        WifiSecurity.UNSUPPORTED
    else -> WifiSecurity.OPEN
}
