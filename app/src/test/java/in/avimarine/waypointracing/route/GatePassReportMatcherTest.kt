package `in`.avimarine.waypointracing.route

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class GatePassReportMatcherTest {
    private fun pass(sourceEventId: String = "pass-2") = GatePassing(
        eventName = "Race",
        routeId = "route",
        routeLastUpdate = Date(0),
        deviceId = "device",
        gateId = 1,
        time = Date(2000),
        sourceEventId = sourceEventId,
    )

    @Test
    fun olderReportForSameGateDoesNotConfirmCurrentPass() {
        assertFalse(GatePassReportMatcher.matches(pass(), "pass-1", 1000, "device"))
        assertTrue(GatePassReportMatcher.matches(pass(), "pass-2", 2000, "device"))
    }

    @Test
    fun legacyPassRequiresMatchingTimeAndDevice() {
        assertFalse(GatePassReportMatcher.matches(pass(""), null, 1000, "device"))
        assertFalse(GatePassReportMatcher.matches(pass(""), null, 2000, "other-device"))
        assertTrue(GatePassReportMatcher.matches(pass(""), null, 2000, "device"))
    }
}
