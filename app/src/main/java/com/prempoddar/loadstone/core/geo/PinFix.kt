package com.prempoddar.loadstone.core.geo

/** A converged position estimate produced by [PositionEstimator]. */
data class PinFix(
    val latitude: Double,
    val longitude: Double,
    /** One-sigma (≈68% confidence) horizontal radius of the estimate, in meters. */
    val estAccuracyMeters: Float,
    val sampleCount: Int,
    val altitudeMeters: Double? = null,
    /** True once the estimate has stopped improving: more waiting will not help. */
    val settled: Boolean = false,
    /** True while the fixes say the user is walking, so the estimate is tracking rather than converging. */
    val moving: Boolean = false,
)

/**
 * A single raw fix fed into [PositionEstimator].
 *
 * @param accuracyMeters the provider's one-sigma horizontal radius.
 * @param elapsedRealtimeMs a monotonic timestamp. Wall-clock time is not usable here: it jumps when
 *   the network corrects the clock, which would make the filter think no time (or hours) had passed.
 * @param speedMps ground speed from the provider when it has one, used to tell walking from noise.
 */
data class RawFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val elapsedRealtimeMs: Long,
    val altitudeMeters: Double? = null,
    val altitudeAccuracyMeters: Float? = null,
    val speedMps: Float? = null,
)
