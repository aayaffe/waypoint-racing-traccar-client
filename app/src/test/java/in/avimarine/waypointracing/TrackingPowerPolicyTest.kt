package `in`.avimarine.waypointracing

import android.os.PowerManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingPowerPolicyTest {
    @Test
    fun detectsScreenOffGpsBlockingModes() {
        assertTrue(TrackingPowerPolicy.blocksScreenOffGps(
            35, true, PowerManager.LOCATION_MODE_GPS_DISABLED_WHEN_SCREEN_OFF))
        assertTrue(TrackingPowerPolicy.blocksScreenOffGps(
            35, true, PowerManager.LOCATION_MODE_ALL_DISABLED_WHEN_SCREEN_OFF))
    }

    @Test
    fun doesNotWarnWhenGpsRemainsAvailableOrPolicyIsUnknown() {
        assertFalse(TrackingPowerPolicy.blocksScreenOffGps(
            35, false, PowerManager.LOCATION_MODE_GPS_DISABLED_WHEN_SCREEN_OFF))
        assertFalse(TrackingPowerPolicy.blocksScreenOffGps(
            35, true, PowerManager.LOCATION_MODE_FOREGROUND_ONLY))
        assertFalse(TrackingPowerPolicy.blocksScreenOffGps(
            23, true, PowerManager.LOCATION_MODE_GPS_DISABLED_WHEN_SCREEN_OFF))
    }
}
