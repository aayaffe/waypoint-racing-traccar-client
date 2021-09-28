package `in`.avimarine.waypointracing.route

import `in`.avimarine.waypointracing.Position
import `in`.avimarine.waypointracing.utils.Serializers
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class GatePassing(
    val id: Long = 0,
    val eventName: String,
    val routeId: String,
    val deviceId: String,
    val boatName: String = "",
    val gateName: String = "",
    @Serializable(with = Serializers.Companion.DateSerializer::class)
    val time: Date,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val speed: Double = 0.0,
    val course: Double = 0.0,
    val accuracy: Double = 0.0,
    val battery: Double = 0.0,
    val mock: Boolean = false,
) {



    constructor(eventName: String, routeId: String, deviceId: String, boatName: String, gateName: String, time: Date, position: Position) : this(
        eventName = eventName,
        routeId = routeId,
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