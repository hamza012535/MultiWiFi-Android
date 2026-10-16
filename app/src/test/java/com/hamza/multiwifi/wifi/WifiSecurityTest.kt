package com.hamza.multiwifi.wifi

import org.junit.Assert.assertEquals
import org.junit.Test

class WifiSecurityTest {

    @Test
    fun openNetworkHasNoSecurityTokens() {
        assertEquals(WifiSecurity.OPEN, securityOf("[ESS]"))
        assertEquals(WifiSecurity.OPEN, securityOf(""))
    }

    @Test
    fun wpa2PskIsWpa2() {
        assertEquals(WifiSecurity.WPA2, securityOf("[WPA2-PSK-CCMP][ESS]"))
    }

    @Test
    fun wpa3SaeAloneIsWpa3() {
        assertEquals(WifiSecurity.WPA3, securityOf("[RSN-SAE-CCMP][ESS]"))
    }

    @Test
    fun transitionNetworkUsesWpa2() {
        assertEquals(WifiSecurity.WPA2, securityOf("[WPA2-PSK-CCMP][RSN-SAE-CCMP][ESS]"))
    }

    @Test
    fun enterpriseAndWepAreUnsupported() {
        assertEquals(WifiSecurity.UNSUPPORTED, securityOf("[WPA2-EAP-CCMP][ESS]"))
        assertEquals(WifiSecurity.UNSUPPORTED, securityOf("[WEP][ESS]"))
        assertEquals(WifiSecurity.UNSUPPORTED, securityOf("[RSN-OWE-CCMP][ESS]"))
    }
}
