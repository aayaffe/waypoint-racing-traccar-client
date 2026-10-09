package `in`.avimarine.waypointracing.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class RaceDeckHealthMapperTest {
    @Test
    fun representsOperationalStatesIndependently() {
        val state = RaceDeckHealthMapper.map(
            gpsAvailable = true,
            trackingActive = false,
            syncRecent = false,
        )

        assertEquals(RaceDeckHealthTone.HEALTHY, state.gps)
        assertEquals(RaceDeckHealthTone.CRITICAL, state.tracking)
        assertEquals(RaceDeckHealthTone.WARNING, state.sync)
    }
}
