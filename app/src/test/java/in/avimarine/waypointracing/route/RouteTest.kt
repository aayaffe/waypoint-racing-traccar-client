package `in`.avimarine.waypointracing.route

import android.os.Build
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
class RouteTest {

    @Test
    fun testParseWptRacingRoute() {
        val jsonText =this::class.java.classLoader.getResource("test_wptracing.json").readText()
        val r = Route.fromGeoJson(jsonText)
        Assert.assertEquals("Test Route 1", r.eventName)
        Assert.assertEquals(EventType.WPTRACING, r.eventType)
    }
    @Test
    fun testParseTresureRoute() {
        val jsonText =this::class.java.classLoader.getResource("test_treasure.json").readText()
        val r = Route.fromGeoJson(jsonText)
        Assert.assertEquals("Test Route 1", r.eventName)
        Assert.assertEquals(EventType.TREASUREHUNT, r.eventType)
    }
    @Test
    fun testParseCircleWaypoint() {
        val jsonText =this::class.java.classLoader.getResource("test_treasure.json").readText()
        val r = Route.fromGeoJson(jsonText)
        Assert.assertEquals("Test Route 1", r.eventName)
        Assert.assertEquals(EventType.TREASUREHUNT, r.eventType)
        val waypoint1 = r.elements[0]
        Assert.assertEquals(ProofAreaType.CIRCLE, waypoint1.proofArea.type)
        Assert.assertEquals(0.06, waypoint1.proofArea.distance, 0.01)
    }

}
