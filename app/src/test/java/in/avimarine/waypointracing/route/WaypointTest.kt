package `in`.avimarine.waypointracing.route

import android.os.Build
import `in`.avimarine.androidutils.createLocation
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
class WaypointTest {

    @Test
    fun testParseCircleWaypoint() {
        val jsonText =this::class.java.classLoader.getResource("test_treasure.json").readText()
        val r = Route.fromGeoJson(jsonText)
        val waypoint1 = r.elements[0]
        Assert.assertEquals(ProofAreaType.CIRCLE, waypoint1.proofArea.type)
        Assert.assertTrue(waypoint1.isInProofArea(createLocation(32.835, 35.02)))
    }

}
