package `in`.avimarine.waypointracing.event

import `in`.avimarine.waypointracing.Position
import java.util.UUID

data class EventSession(
    val eventId: String = "",
    val boatId: String = "",
    val routeId: String = "",
    val routeVersion: String = "",
    val sessionId: String = "",
) {
    val isAssigned: Boolean get() = eventId.isNotBlank() && boatId.isNotBlank() && routeId.isNotBlank()

    fun newSourceEventId(): String = UUID.randomUUID().toString()

    fun attach(position: Position): Position = if (isAssigned) position.copy(
        eventId = eventId,
        boatId = boatId,
        routeId = routeId,
        sessionId = sessionId,
        sourceEventId = newSourceEventId(),
    ) else position
}
