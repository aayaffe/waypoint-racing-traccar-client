package `in`.avimarine.waypointracing.ui

import android.graphics.drawable.Drawable

/** Immutable, UI-only presentation model for the Race Deck. */
data class RaceDeckUiState(
    val courseName: String = "",
    val boatName: String = "",
    val target: RaceDeckTargetState? = null,
    val navigation: RaceDeckNavigationState = RaceDeckNavigationState(),
    val health: RaceDeckHealthState = RaceDeckHealthState(
        gps = RaceDeckHealthTone.CRITICAL,
        tracking = RaceDeckHealthTone.CRITICAL,
        sync = RaceDeckHealthTone.WARNING,
    ),
    val healthLabels: RaceDeckHealthLabels = RaceDeckHealthLabels(),
    val deviceWarning: String? = null,
    val deviceWarningCritical: Boolean = false,
    val trackingWarningVisible: Boolean = false,
    val gpsWarning: String? = null,
    val passBanner: RaceDeckPassBannerState? = null,
    val mapExpanded: Boolean = false,
)

data class RaceDeckHealthLabels(
    val gps: String = "",
    val tracking: String = "",
    val sync: String = "",
    val device: String = "",
)

/** Values already calculated by the legacy navigation layer, ready for display. */
data class RaceDeckNavigationState(
    val bearing: String = "—",
    val distance: String = "—",
    val portEndpoint: String = "—",
    val starboardEndpoint: String = "—",
    val cog: String = "—",
    val sog: String = "—",
    val vmg: String = "—",
    val ttg: String = "—",
    val eta: String = "—",
    val time: String = "—",
    val position: String = "—",
)

/** Presentation only; pass detection and persistence remain in the existing event path. */
data class RaceDeckPassBannerState(
    val name: String,
    val eventDetails: String,
    val nextTarget: String?,
    val nextTargetLabel: String? = null,
    val isFinish: Boolean,
    val titleText: String = "",
    val icon: Drawable? = null,
    val uploadStatusText: String = "",
) {
    companion object {
        const val MAX_DISPLAY_DURATION_MILLIS = 10_000L
    }
}
