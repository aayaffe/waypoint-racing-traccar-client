package `in`.avimarine.waypointracing.event

import `in`.avimarine.waypointracing.route.Route
import `in`.avimarine.waypointracing.utils.Preferences
import com.google.firebase.functions.FirebaseFunctions
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicLong

object EventAssignmentResolver {
    private val lookupGeneration = AtomicLong(0)
    private val timestampPatterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
    )

    /**
     * Uses java.text rather than java.time because this app supports API 23.
     * The backend returns canonical UTC ISO-8601 timestamp strings.
     */
    internal fun parseServerTimestamp(value: Any?): Long? {
        val text = value as? String ?: return null
        return timestampPatterns.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(text)?.time
            }.getOrNull()
        }
    }

    fun refreshForRoute(route: Route, preferences: Preferences, onComplete: (EventSession?) -> Unit = {}) {
        // A route change must never continue publishing under a previous route's
        // event assignment while the asynchronous lookup is pending or fails.
        val requestGeneration = lookupGeneration.incrementAndGet()
        preferences.beginEventAssignmentLookup()
        fun isCurrentRequest() = lookupGeneration.get() == requestGeneration
        fun noAssignment() {
            if (!isCurrentRequest()) return
            preferences.clearEventSession()
            onComplete(null)
        }
        fun lookupFailed() {
            if (!isCurrentRequest()) return
            // Do not silently fall back to unscoped uploads for an event-enabled
            // route when assignment lookup cannot establish consent scope.
            preferences.failEventAssignmentLookup()
            onComplete(null)
        }

        FirebaseFunctions.getInstance().getHttpsCallable("getMyEventsCallable").call()
            .addOnSuccessListener { result ->
                if (!isCurrentRequest()) return@addOnSuccessListener
                val response = result.data as? Map<*, *> ?: return@addOnSuccessListener noAssignment()
                val events = response["events"] as? List<*> ?: return@addOnSuccessListener noAssignment()
                val now = Date().time
                val match = events.asSequence().mapNotNull { it as? Map<*, *> }.firstNotNullOfOrNull { event ->
                    if (event["routeId"] != route.id) return@firstNotNullOfOrNull null
                    val membership = event["membership"] as? Map<*, *> ?: return@firstNotNullOfOrNull null
                    val assignments = membership["assignments"] as? List<*> ?: return@firstNotNullOfOrNull null
                    val assignment = assignments.asSequence().mapNotNull { it as? Map<*, *> }.firstOrNull { candidate ->
                        val from = parseServerTimestamp(candidate["from"]) ?: Long.MAX_VALUE
                        val to = parseServerTimestamp(candidate["to"]) ?: Long.MIN_VALUE
                        now in from..to
                    } ?: return@firstNotNullOfOrNull null
                    val eventId = event["eventId"]?.toString() ?: return@firstNotNullOfOrNull null
                    val boatId = assignment["boatId"]?.toString() ?: return@firstNotNullOfOrNull null
                    EventSession(eventId, boatId, route.id, route.lastUpdate.time.toString(), java.util.UUID.randomUUID().toString())
                }
                if (!isCurrentRequest()) return@addOnSuccessListener
                if (match == null) noAssignment() else {
                    preferences.eventSession = match
                    preferences.eventAssignmentLookupPending = false
                    onComplete(match)
                }
            }
            .addOnFailureListener { lookupFailed() }
    }
}
