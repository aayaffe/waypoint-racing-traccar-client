package `in`.avimarine.waypointracing.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class GpsAlertPolicyTest {
    @Test
    fun prolongedAlertWaitsForAtLeastTwoMinutes() {
        assertEquals(120_000L, GpsAlertPolicy.alertDelayMillis(4_000L))
        assertEquals(240_000L, GpsAlertPolicy.alertDelayMillis(240_000L))
    }
}
