package `in`.avimarine.waypointracing.ui

enum class DeviceReadinessIssue(val severity: RaceDeckHealthTone) {
    BACKGROUND_RESTRICTED(RaceDeckHealthTone.CRITICAL),
    NOTIFICATIONS_DISABLED(RaceDeckHealthTone.WARNING),
    BATTERY_OPTIMIZED(RaceDeckHealthTone.WARNING),
    BATTERY_SAVER(RaceDeckHealthTone.WARNING),
}

object DeviceReadiness {
    fun issues(
        notificationsEnabled: Boolean,
        batteryOptimizationIgnored: Boolean,
        backgroundRestricted: Boolean,
        batterySaverEnabled: Boolean,
    ): List<DeviceReadinessIssue> = buildList {
        if (backgroundRestricted) add(DeviceReadinessIssue.BACKGROUND_RESTRICTED)
        if (!notificationsEnabled) add(DeviceReadinessIssue.NOTIFICATIONS_DISABLED)
        if (!batteryOptimizationIgnored) add(DeviceReadinessIssue.BATTERY_OPTIMIZED)
        if (batterySaverEnabled) add(DeviceReadinessIssue.BATTERY_SAVER)
    }
}
