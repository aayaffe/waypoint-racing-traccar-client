package `in`.avimarine.waypointracing.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceReadinessTest {
    @Test
    fun healthyDeviceHasNoReadinessIssues() {
        assertEquals(
            emptyList<DeviceReadinessIssue>(),
            DeviceReadiness.issues(
                notificationsEnabled = true,
                batteryOptimizationIgnored = true,
                backgroundRestricted = false,
                batterySaverEnabled = false,
            )
        )
    }

    @Test
    fun restrictionIsFirstAndWarningsRemainIndependent() {
        assertEquals(
            listOf(
                DeviceReadinessIssue.BACKGROUND_RESTRICTED,
                DeviceReadinessIssue.NOTIFICATIONS_DISABLED,
                DeviceReadinessIssue.BATTERY_OPTIMIZED,
                DeviceReadinessIssue.BATTERY_SAVER,
            ),
            DeviceReadiness.issues(
                notificationsEnabled = false,
                batteryOptimizationIgnored = false,
                backgroundRestricted = true,
                batterySaverEnabled = true,
            )
        )
    }
}
