package com.hamza.multiwifi.probe

import kotlin.math.abs

/** Summary of repeated TCP connect-time samples, in milliseconds. */
data class LatencyStats(
    val samples: Int,
    val failures: Int,
    val minMs: Double,
    val medianMs: Double,
    val maxMs: Double,
    /** Mean absolute difference between consecutive samples. */
    val jitterMs: Double
)

fun summarizeLatency(samplesMs: List<Double>, failures: Int): LatencyStats? {
    if (samplesMs.isEmpty()) return null
    val sorted = samplesMs.sorted()
    val mid = sorted.size / 2
    val median = if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2.0
    val jitter = if (samplesMs.size < 2) 0.0 else samplesMs.zipWithNext { a, b -> abs(b - a) }.average()
    return LatencyStats(
        samples = samplesMs.size,
        failures = failures,
        minMs = sorted.first(),
        medianMs = median,
        maxMs = sorted.last(),
        jitterMs = jitter
    )
}

/** Megabits per second for [bytes] transferred in [elapsedNanos], or null if not computable. */
fun throughputMbps(bytes: Long, elapsedNanos: Long): Double? {
    if (bytes <= 0L || elapsedNanos <= 0L) return null
    return bytes * 8.0 / (elapsedNanos / 1_000_000_000.0) / 1_000_000.0
}
