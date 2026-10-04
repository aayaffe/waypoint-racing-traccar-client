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
    fun parsesBackendUtcTimestampsWithoutJavaTime() {
        assertEquals(1767441600000L, EventAssignmentResolver.parseServerTimestamp("2026-01-03T12:00:00.000Z"))
        assertEquals(1767441600000L, EventAssignmentResolver.parseServerTimestamp("2026-01-03T12:00:00Z"))
        assertEquals(null, EventAssignmentResolver.parseServerTimestamp("not-a-timestamp"))
    }

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

    @Test
    fun legacyUploadsDoNotDependOnEventSetup() {
        val assigned = EventSession("event-1", "boat-1", "route-1", "version-1", "session-1")

        assertTrue(EventSession().shouldUploadPosition(true, false, false, false))
        assertFalse(assigned.shouldUploadPosition(true, true, false, false))
        assertTrue(assigned.shouldUploadPosition(true, true, true, false))
        assertFalse(EventSession().shouldUploadPosition(false, true, true, false))
        assertFalse(EventSession().shouldUploadPosition(true, true, true, true))
    }
}
