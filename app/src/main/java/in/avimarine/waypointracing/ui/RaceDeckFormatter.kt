package `in`.avimarine.waypointracing.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/**
 * Presentation-only formatting for the Release A Race Deck.
 *
 * Keeping these rules outside Activities and navigation calculations makes the
 * instrument display independently testable and prevents UI changes from
 * changing race semantics.
 */
object RaceDeckFormatter {
    private const val unavailable = "—"

    fun clock(timeMillis: Long, locale: Locale = Locale.getDefault(), timeZone: TimeZone = TimeZone.getDefault()): String =
        formatTime(timeMillis, locale, timeZone)

    fun eta(timeMillis: Long?, locale: Locale = Locale.getDefault(), timeZone: TimeZone = TimeZone.getDefault()): String =
        timeMillis?.let { formatTime(it, locale, timeZone) } ?: unavailable

    fun passDetails(timeMillis: Long, latitude: Double, longitude: Double, locale: Locale = Locale.getDefault(), timeZone: TimeZone = TimeZone.getDefault()): String =
        "${formatTime(timeMillis, locale, timeZone)}\n${coordinate(latitude, true, locale)} ${coordinate(longitude, false, locale)}"

    private fun formatTime(timeMillis: Long, locale: Locale, timeZone: TimeZone): String =
        SimpleDateFormat("HH:mm:ss", locale).apply { this.timeZone = timeZone }.format(Date(timeMillis))

    private fun coordinate(value: Double, latitude: Boolean, locale: Locale): String {
        if (!value.isFinite()) return unavailable
        val degrees = abs(value).toInt()
        val minutes = (abs(value) - degrees) * 60
        val hemisphere = if (latitude) {
            if (value < 0) "S" else "N"
        } else if (value < 0) "W" else "E"
        return String.format(locale, "%d°%06.3f'%s", degrees, minutes, hemisphere)
    }
}
