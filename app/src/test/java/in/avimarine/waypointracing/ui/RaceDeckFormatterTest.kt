package `in`.avimarine.waypointracing.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

class RaceDeckFormatterTest {
    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun clockAndEtaAlwaysIncludeSeconds() {
        val instant = 1_700_000_045_000L

        assertEquals("22:14:05", RaceDeckFormatter.clock(instant, Locale.US, utc))
        assertEquals("22:14:05", RaceDeckFormatter.eta(instant, Locale.US, utc))
        assertEquals("—", RaceDeckFormatter.eta(null, Locale.US, utc))
    }

    @Test
    fun passDetailsUsesEventTimeAndEventPosition() {
        assertEquals(
            "22:14:05\n32°49.380'N 34°58.140'E",
            RaceDeckFormatter.passDetails(1_700_000_045_000L, 32.823, 34.969, Locale.US, utc)
        )
    }
}
