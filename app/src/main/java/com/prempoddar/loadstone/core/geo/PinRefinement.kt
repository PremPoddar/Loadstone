package com.prempoddar.loadstone.core.geo

import kotlin.math.max
import kotlin.math.sqrt

/**
 * Combines a stored pin with a fresh observation of the same spot.
 *
 * Nothing you can do while saving a pin removes the error that every fix in that minute shares —
 * the satellite geometry overhead, the wall the signal bounced off. That is why a pin saved
 * carefully can still be ten meters out when you walk back to it, and why waiting longer at the
 * time would not have helped.
 *
 * Coming back later does help, and this is the only thing that does. Hours or weeks on, the
 * constellation has moved and the atmosphere has changed, so a second measurement is wrong in a
 * genuinely different direction. Two such measurements, weighted by how good each one is, land
 * closer to the truth than either — and unlike fixes seconds apart, their errors really are
 * independent, so the combined uncertainty may be tighter than both.
 */
object PinRefinement {
    /** A position with a one-sigma horizontal uncertainty in meters. */
    data class Observation(val latitude: Double, val longitude: Double, val sigmaMeters: Double)

    /**
     * Inverse-variance weighted combination of [stored] and [observed]: each is pulled toward the
     * other in proportion to how much tighter the other is.
     */
    fun merge(stored: Observation, observed: Observation): Observation {
        val sa = max(stored.sigmaMeters, PositionEstimator.ABSOLUTE_FLOOR_M)
        val sb = max(observed.sigmaMeters, PositionEstimator.ABSOLUTE_FLOOR_M)
        val wa = 1.0 / (sa * sa)
        val wb = 1.0 / (sb * sb)
        val total = wa + wb

        // Average in a local frame so that a degree of longitude is not mistaken for a degree of
        // latitude, which away from the equator it is not.
        val frame = LocalFrame(stored.latitude, stored.longitude)
        val east = (wa * frame.east(stored.longitude) + wb * frame.east(observed.longitude)) / total
        val north = (wa * frame.north(stored.latitude) + wb * frame.north(observed.latitude)) / total

        return Observation(
            latitude = frame.latitudeOf(north),
            longitude = frame.longitudeOf(east),
            sigmaMeters = max(sqrt(1.0 / total), PositionEstimator.ABSOLUTE_FLOOR_M),
        )
    }

    /**
     * Whether a second visit is worth folding in. Merging an observation that is far looser than
     * the pin only adds noise, and merging one that disagrees wildly means one of the two is not
     * describing this spot at all — most likely the user is standing somewhere else.
     */
    fun worthMerging(stored: Observation, observed: Observation): Boolean {
        if (observed.sigmaMeters > stored.sigmaMeters * MAX_SIGMA_RATIO) return false
        val separation = GeoMath.distanceMeters(
            stored.latitude,
            stored.longitude,
            observed.latitude,
            observed.longitude,
        )
        val combined = sqrt(
            stored.sigmaMeters * stored.sigmaMeters + observed.sigmaMeters * observed.sigmaMeters,
        )
        return separation <= combined * MAX_SEPARATION_SIGMAS
    }

    /** An observation this much looser than the pin has nothing to add. */
    private const val MAX_SIGMA_RATIO = 1.5

    /** Beyond this disagreement the two are not measurements of one place. */
    private const val MAX_SEPARATION_SIGMAS = 3.0
}
