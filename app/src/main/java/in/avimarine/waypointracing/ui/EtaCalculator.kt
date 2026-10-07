package `in`.avimarine.waypointracing.ui

import kotlin.math.roundToLong

/** Returns an arrival timestamp only when progress toward the selected target is measurable. */
internal object EtaCalculator {
    fun arrivalTimeMillis(distanceNauticalMiles: Double, vmgKnots: Double, fixTimeMillis: Long): Long? {
        if (!distanceNauticalMiles.isFinite() || distanceNauticalMiles < 0.0 ||
            !vmgKnots.isFinite() || vmgKnots <= 0.0 || fixTimeMillis <= 0L) {
            return null
        }

        val travelMillis = distanceNauticalMiles / vmgKnots * 3_600_000.0
        if (!travelMillis.isFinite() || travelMillis > Long.MAX_VALUE.toDouble() - fixTimeMillis) {
            return null
        }
        return fixTimeMillis + travelMillis.roundToLong()
    }
}
