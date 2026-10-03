package `in`.avimarine.waypointracing.event

import `in`.avimarine.waypointracing.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class EventSessionTest {
    @Test
    fun attachesStableSessionContextAndUniqueSourceIds() {
        val session = EventSession("event-1", "boat-1", "route-1", "version-1", "session-1")
        val position = Position(deviceId = "device", userId = "user", time = Date(0))

        val first = session.attach(position)
        val second = session.attach(position)

        assertTrue(session.isAssigned)
        assertEquals("event-1", first.eventId)
        assertEquals("boat-1", first.boatId)
        assertEquals("route-1", first.routeId)
        assertEquals("session-1", first.sessionId)
        assertNotEquals(first.sourceEventId, second.sourceEventId)
    }

    @Test
    fun preservesLegacyPositionWhenNoEventIsAssigned() {
        val position = Position(deviceId = "device", userId = "user", time = Date(0))
        val unassigned = EventSession().attach(position)

        assertFalse(EventSession().isAssigned)
        assertEquals(position, unassigned)
    }
}
