package `in`.avimarine.waypointracing.ui

/** Behavioral policy for a prolonged GPS outage; not a view-timing rule. */
object GpsAlertPolicy {
    private const val MIN_OUTAGE_ALERT_MS = 120_000L

    fun alertDelayMillis(locationTimeoutMillis: Long): Long =
        maxOf(MIN_OUTAGE_ALERT_MS, locationTimeoutMillis)
}
