package `in`.avimarine.waypointracing.route

import `in`.avimarine.waypointracing.Position
import android.location.Location
import android.location.LocationManager
import android.os.Build
import java.util.*

data class GatePassing(
    val id: Long = 0,
    val eventName: String,
    val deviceId: String,
    val boatName: String = "",
    val gateName: String = "",
    val time: Date,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val speed: Double = 0.0,
    val course: Double = 0.0,
    val accuracy: Double = 0.0,
    val battery: Double = 0.0,
    val mock: Boolean = false,
) {

    constructor(eventName: String, deviceId: String, boatName: String, gateName: String, time: Date, position: Position) : this(
        eventName = eventName,
        deviceId = deviceId,
        boatName = boatName,
        gateName = gateName,
        time = time,
        latitude = position.latitude,
        longitude = position.longitude,
        speed = position.speed, // * 1.943844, // speed in knots
        course = position.course,
        accuracy = position.accuracy,
        battery = position.battery,
        mock = position.mock
    )
}