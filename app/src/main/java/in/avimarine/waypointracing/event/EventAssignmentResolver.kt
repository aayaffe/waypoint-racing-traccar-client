package `in`.avimarine.waypointracing.event

import `in`.avimarine.waypointracing.route.Route
import `in`.avimarine.waypointracing.utils.Preferences
import com.google.firebase.functions.FirebaseFunctions
import java.util.Date

object EventAssignmentResolver {
    fun refreshForRoute(route: Route, preferences: Preferences, onComplete: (EventSession?) -> Unit = {}) {
        FirebaseFunctions.getInstance().getHttpsCallable("getMyEventsCallable").call()
            .addOnSuccessListener { result ->
                val response = result.data as? Map<*, *> ?: return@addOnSuccessListener onComplete(null)
                val events = response["events"] as? List<*> ?: return@addOnSuccessListener onComplete(null)
                val now = Date().time
                val match = events.asSequence().mapNotNull { it as? Map<*, *> }.firstNotNullOfOrNull { event ->
                    if (event["routeId"] != route.id) return@firstNotNullOfOrNull null
                    val membership = event["membership"] as? Map<*, *> ?: return@firstNotNullOfOrNull null
                    val assignments = membership["assignments"] as? List<*> ?: return@firstNotNullOfOrNull null
                    val assignment = assignments.asSequence().mapNotNull { it as? Map<*, *> }.firstOrNull { candidate ->
                        val from = candidate["from"]?.toString()?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() } ?: Long.MAX_VALUE
                        val to = candidate["to"]?.toString()?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() } ?: Long.MIN_VALUE
                        now in from..to
                    } ?: return@firstNotNullOfOrNull null
                    val eventId = event["eventId"]?.toString() ?: return@firstNotNullOfOrNull null
                    val boatId = assignment["boatId"]?.toString() ?: return@firstNotNullOfOrNull null
                    EventSession(eventId, boatId, route.id, route.lastUpdate.time.toString(), java.util.UUID.randomUUID().toString())
                }
                if (match == null) preferences.clearEventSession() else preferences.eventSession = match
                onComplete(match)
            }
            .addOnFailureListener { onComplete(null) }
    }
}
