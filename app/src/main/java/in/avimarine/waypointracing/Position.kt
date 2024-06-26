package `in`.avimarine.waypointracing

import android.location.Location
import android.location.LocationManager
import android.os.Build
import `in`.avimarine.androidutils.BatteryStatus
import java.util.*

data class Position(
    val id: Long = 0,
    val deviceId: String,
    val userId: String = "",
    val time: Date,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val speed: Double = 0.0, //In Knots
    val course: Double = 0.0,
    val accuracy: Double = 0.0,
    val battery: Double = 0.0,
    val boatName: String = "",
    val charging: Boolean = false,
    val mock: Boolean = false,
) {
    constructor(deviceId: String, userId: String, boatName: String, location: Location, battery: BatteryStatus) : this(
        deviceId = deviceId,
        userId = userId,
        time = Date(location.time.correctRollover()),
        latitude = location.latitude,
        longitude = location.longitude,
        altitude = location.altitude,
        speed = location.speed * 1.943844, // convert m/sec to knots
        course = location.bearing.toDouble(),
        accuracy = if (location.provider != null && location.provider != LocationManager.GPS_PROVIDER) {
            location.accuracy.toDouble()
        } else {
            0.0
        },
        boatName = boatName,
        mock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        location.isMock
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            @Suppress("DEPRECATION")
            location.isFromMockProvider
        } else {
            false
        },
        battery = battery.level,
        charging = battery.charging,

    )
}

private const val rolloverDate = 1554508800000L // April 6, 2019
private const val rolloverOffset = 619315200000L // 1024 weeks

private fun Long.correctRollover(): Long {
    return if (this < rolloverDate) this + rolloverOffset else this
}