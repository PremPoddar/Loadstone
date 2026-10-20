package com.prempoddar.loadstone.core.geo

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max

/** One point of a recorded path. */
data class TrailPoint(val latitude: Double, val longitude: Double)

/**
 * A path walked away from a pin, kept so it can be walked back exactly, around the building,
 * along the road, back down the right trail, rather than in the straight line an arrow points.
 * Points run from the pin outward.
 */
object Trail {
    /** Compact text for the database: "lat,lon;lat,lon", to about 10 cm. */
    fun encode(points: List<TrailPoint>): String =
        points.joinToString(";") { "%.6f,%.6f".format(java.util.Locale.ROOT, it.latitude, it.longitude) }

    fun decode(text: String): List<TrailPoint> =
        if (text.isBlank()) emptyList() else text.split(';').mapNotNull { p ->
            val (a, b) = p.split(',').takeIf { it.size == 2 } ?: return@mapNotNull null
            val lat = a.toDoubleOrNull() ?: return@mapNotNull null
            val lon = b.toDoubleOrNull() ?: return@mapNotNull null
            TrailPoint(lat, lon)
        }

    /** Walking length of the path in metres. */
    fun length(points: List<TrailPoint>): Double =
        points.zipWithNext().sumOf { (a, b) -> GeoMath.distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude) }

    /**
     * Drops points that add nothing to the shape (Douglas–Peucker): a straight stretch keeps its
     * two ends, every turn keeps its corner. Nothing moves by more than [toleranceMeters].
     */
    fun simplify(points: List<TrailPoint>, toleranceMeters: Double = 3.0): List<TrailPoint> {
        if (points.size < 3) return points
        val plane = Plane(points[0])
        val xy = points.map { plane.of(it) }
        val keep = BooleanArray(points.size).also { it[0] = true; it[it.lastIndex] = true }
        val stack = ArrayDeque<Pair<Int, Int>>().apply { add(0 to points.lastIndex) }
        while (stack.isNotEmpty()) {
            val (from, to) = stack.removeLast()
            var worst = -1; var worstD = toleranceMeters
            for (i in from + 1 until to) {
                val d = distanceToSegment(xy[i], xy[from], xy[to])
                if (d > worstD) { worst = i; worstD = d }
            }
            if (worst >= 0) { keep[worst] = true; stack.add(from to worst); stack.add(worst to to) }
        }
        return points.filterIndexed { i, _ -> keep[i] }
    }

    /** Where to head next to retrace [trail] back to its start (the pin). */
    data class Guide(
        val target: TrailPoint,
        /** Distance left along the path to the pin. */
        val remainingMeters: Double,
        /** How far from the path you have strayed. */
        val offTrailMeters: Double,
    )

    /**
     * The point [lookaheadMeters] further along the path toward the pin from wherever you are on
     * it. Aiming a little ahead rather than at the nearest point keeps the arrow pointing along
     * the path instead of sideways at it; once you have strayed far, it points back to the path.
     */
    fun guide(trail: List<TrailPoint>, latitude: Double, longitude: Double, lookaheadMeters: Double = 15.0): Guide? {
        if (trail.size < 2) return null
        val plane = Plane(trail[0])
        val xy = trail.map { plane.of(it) }
        val here = plane.of(TrailPoint(latitude, longitude))
        // The nearest point on the path, as segment index and fraction along it.
        var bestSeg = 0; var bestT = 0.0; var bestD = Double.MAX_VALUE
        for (i in 0 until xy.lastIndex) {
            val (t, d) = project(here, xy[i], xy[i + 1])
            if (d < bestD) { bestD = d; bestSeg = i; bestT = t }
        }
        val seg = segLengths(xy)
        // Path distance from the pin (index 0) to the nearest point.
        val along = seg.take(bestSeg).sum() + bestT * seg[bestSeg]
        if (bestD > STRAYED_METERS) {
            val back = lerp(xy[bestSeg], xy[bestSeg + 1], bestT)
            return Guide(plane.back(back), along + bestD, bestD)
        }
        // Walk back toward the pin by the lookahead.
        val goal = max(0.0, along - lookaheadMeters)
        var acc = 0.0
        for (i in 0 until xy.lastIndex) {
            if (acc + seg[i] >= goal) {
                val t = if (seg[i] == 0.0) 0.0 else (goal - acc) / seg[i]
                return Guide(plane.back(lerp(xy[i], xy[i + 1], t)), along, bestD)
            }
            acc += seg[i]
        }
        return Guide(trail[0], along, bestD)
    }

    /** Beyond this far from the path, head back to it first. */
    const val STRAYED_METERS = 35.0

    private fun segLengths(xy: List<DoubleArray>) = xy.zipWithNext { a, b -> hypot(b[0] - a[0], b[1] - a[1]) }

    private fun lerp(a: DoubleArray, b: DoubleArray, t: Double) = doubleArrayOf(a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t)

    /** Fraction along a→b of the nearest point to p, and the distance to it. */
    private fun project(p: DoubleArray, a: DoubleArray, b: DoubleArray): Pair<Double, Double> {
        val dx = b[0] - a[0]; val dy = b[1] - a[1]
        val len2 = dx * dx + dy * dy
        val t = if (len2 == 0.0) 0.0 else (((p[0] - a[0]) * dx + (p[1] - a[1]) * dy) / len2).coerceIn(0.0, 1.0)
        return t to hypot(p[0] - (a[0] + t * dx), p[1] - (a[1] + t * dy))
    }

    private fun distanceToSegment(p: DoubleArray, a: DoubleArray, b: DoubleArray) = project(p, a, b).second

    /** A flat east/north metre grid around [origin]; plenty accurate over a walk. */
    private class Plane(private val origin: TrailPoint) {
        private val mPerLat = 111_320.0
        private val mPerLon = 111_320.0 * cos(origin.latitude * PI / 180)
        fun of(p: TrailPoint) = doubleArrayOf((p.longitude - origin.longitude) * mPerLon, (p.latitude - origin.latitude) * mPerLat)
        fun back(xy: DoubleArray) = TrailPoint(origin.latitude + xy[1] / mPerLat, origin.longitude + xy[0] / mPerLon)
    }
}

/** Builds a path from a stream of estimates: a point every few metres, rough fixes left out. */
class TrailRecorder(initial: List<TrailPoint> = emptyList()) {
    private val points = initial.toMutableList()
    val trail: List<TrailPoint> get() = points

    /** True if [fix] added a point. */
    fun add(fix: PinFix): Boolean {
        if (fix.estAccuracyMeters > MAX_ACCURACY) return false
        val p = TrailPoint(fix.latitude, fix.longitude)
        val last = points.lastOrNull()
        if (last != null && GeoMath.distanceMeters(last.latitude, last.longitude, p.latitude, p.longitude) < STEP_METERS) return false
        points += p
        return true
    }

    companion object {
        const val STEP_METERS = 5.0
        const val MAX_ACCURACY = 20f
    }
}
