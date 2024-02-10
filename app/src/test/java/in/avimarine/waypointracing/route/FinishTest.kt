package `in`.avimarine.waypointracing.route

import android.os.Build
import com.mapbox.geojson.Feature
import `in`.avimarine.androidutils.Utils.Companion.convertStandardJSONString
import `in`.avimarine.androidutils.createLocation
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
class FinishTest {

    @Test
    fun testParseGate() {
        val jsonText = this::class.java.classLoader?.getResource("test_wptracing.json")?.readText()
        val r = Route.fromGeoJson(jsonText!!)
        val waypoint1 = r.elements[5]
        Assert.assertEquals(ProofAreaType.POLYGON, waypoint1.proofArea.type)
        Assert.assertTrue(waypoint1.isInProofArea(createLocation(31.685, 34.551)))
    }

    @Test
    fun testToGeoJson(){
        val jsonText = this::class.java.classLoader?.getResource("test_wptracing.json")?.readText()
        val r = Route.fromGeoJson(jsonText!!)
        val waypoint1 = r.elements[5]
        val geoJson = waypoint1.toGeoJson()
        val feature = Feature.fromJson(convertStandardJSONString(geoJson))
        val reparsedWaypoint = Finish.fromGeoJson(feature)
        Assert.assertEquals(waypoint1, reparsedWaypoint)
    }

}
