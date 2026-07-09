package com.prempoddar.loadstone.core.geo

/** A converged position estimate produced by [LocationAveraging]. */
data class PinFix(
    val latitude: Double,
    val longitude: Double,
    val estAccuracyMeters: Float,
    val sampleCount: Int,
    val altitudeMeters: Double? = null,
)

/** A single raw GPS fix fed into [LocationAveraging]. */
data class RawFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timestampMs: Long,
    val altitudeMeters: Double? = null,
)
