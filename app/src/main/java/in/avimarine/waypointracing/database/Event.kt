package `in`.avimarine.waypointracing.database

data class Event(
    val uid: String = "",
    val eventType: EventType,
    val time: Long,
    val extraData: String
)

