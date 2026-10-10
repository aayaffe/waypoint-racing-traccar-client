package `in`.avimarine.waypointracing.ui

/** Immutable, UI-only summary of the Race Deck's header and selected target. */
data class RaceDeckUiState(
    val courseName: String = "",
    val boatName: String = "",
    val target: RaceDeckTargetState? = null,
    val health: RaceDeckHealthState = RaceDeckHealthState(
        gps = RaceDeckHealthTone.CRITICAL,
        tracking = RaceDeckHealthTone.CRITICAL,
        sync = RaceDeckHealthTone.WARNING,
    ),
    val mapExpanded: Boolean = false,
)
