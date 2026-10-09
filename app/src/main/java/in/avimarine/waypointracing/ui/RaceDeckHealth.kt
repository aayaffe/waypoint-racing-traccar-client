package `in`.avimarine.waypointracing.ui

/** Domain-neutral health presentation decisions for the Release A Race Deck. */
enum class RaceDeckHealthTone {
    HEALTHY,
    WARNING,
    CRITICAL,
}

data class RaceDeckHealthState(
    val gps: RaceDeckHealthTone,
    val tracking: RaceDeckHealthTone,
    val sync: RaceDeckHealthTone,
)

object RaceDeckHealthMapper {
    fun map(
        gpsAvailable: Boolean,
        trackingActive: Boolean,
        syncRecent: Boolean,
    ): RaceDeckHealthState = RaceDeckHealthState(
        gps = if (gpsAvailable) RaceDeckHealthTone.HEALTHY else RaceDeckHealthTone.CRITICAL,
        tracking = if (trackingActive) RaceDeckHealthTone.HEALTHY else RaceDeckHealthTone.CRITICAL,
        sync = if (syncRecent) RaceDeckHealthTone.HEALTHY else RaceDeckHealthTone.WARNING,
    )
}
