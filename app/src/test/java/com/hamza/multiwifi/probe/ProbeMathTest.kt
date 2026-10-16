package com.hamza.multiwifi.probe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ProbeMathTest {

    @Test
    fun emptySamplesGiveNoStats() {
        assertNull(summarizeLatency(emptyList(), failures = 3))
    }

    @Test
    fun medianOfOddCountIsMiddleValue() {
        val stats = summarizeLatency(listOf(30.0, 10.0, 20.0), failures = 0)
        assertNotNull(stats)
        assertEquals(20.0, stats!!.medianMs, 0.0001)
        assertEquals(10.0, stats.minMs, 0.0001)
        assertEquals(30.0, stats.maxMs, 0.0001)
    }

    @Test
    fun medianOfEvenCountAveragesMiddlePair() {
        val stats = summarizeLatency(listOf(10.0, 40.0, 20.0, 30.0), failures = 0)
        assertEquals(25.0, stats!!.medianMs, 0.0001)
    }

    @Test
    fun jitterIsMeanAbsoluteConsecutiveDifference() {
        // |20-10| = 10, |15-20| = 5 -> mean 7.5
        val stats = summarizeLatency(listOf(10.0, 20.0, 15.0), failures = 0)
        assertEquals(7.5, stats!!.jitterMs, 0.0001)
    }

    @Test
    fun singleSampleHasZeroJitter() {
        val stats = summarizeLatency(listOf(42.0), failures = 1)
        assertEquals(0.0, stats!!.jitterMs, 0.0001)
        assertEquals(1, stats.failures)
        assertEquals(1, stats.samples)
    }

    @Test
    fun throughputConvertsBytesAndNanosToMbps() {
        // 1,000,000 bytes in one second = 8 Mbps
        assertEquals(8.0, throughputMbps(1_000_000L, 1_000_000_000L)!!, 0.0001)
    }

    @Test
    fun throughputRejectsZeroInputs() {
        assertNull(throughputMbps(0L, 1_000_000_000L))
        assertNull(throughputMbps(1_000_000L, 0L))
    }
}
