package `in`.avimarine.waypointracing

import android.content.Context
import android.os.Build
import android.os.PowerManager

internal object TrackingPowerPolicy {
    fun blocksScreenOffGps(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) powerManager.locationPowerSaveMode else 0
        return blocksScreenOffGps(Build.VERSION.SDK_INT, powerManager.isPowerSaveMode, mode)
    }

    fun blocksScreenOffGps(sdk: Int, batterySaverOn: Boolean, locationMode: Int): Boolean =
        batterySaverOn && sdk >= Build.VERSION_CODES.P &&
            (locationMode == PowerManager.LOCATION_MODE_GPS_DISABLED_WHEN_SCREEN_OFF ||
                locationMode == PowerManager.LOCATION_MODE_ALL_DISABLED_WHEN_SCREEN_OFF)
}
