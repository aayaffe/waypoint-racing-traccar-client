package `in`.avimarine.waypointracing.route

import `in`.avimarine.androidutils.Position
import `in`.avimarine.androidutils.Serializers
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class GatePassing(
    val id: Int = 0,
    val eventName: String,
    val routeId: String,
    @Serializable(with = Serializers.Companion.DateSerializer::class)
    val routeLastUpdate: Date,
    val deviceId: String,
    val boatName: String = "",
    val gateId: Int,
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



    constructor(eventName: String, routeId: String, routeLastUpdate: Date, deviceId: String, boatName: String, gateId: Int, gateName: String, time: Date, position: Position) : this(
        eventName = eventName,
        routeId = routeId,
        routeLastUpdate = routeLastUpdate,
        deviceId = deviceId,
        boatName = boatName,
        gateId = gateId,
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