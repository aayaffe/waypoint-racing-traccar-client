package `in`.avimarine.waypointracing.route

import android.os.Build
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import `in`.avimarine.androidutils.Utils.Companion.convertStandardJSONString
import `in`.avimarine.androidutils.createLocation
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
class GateTest {

    @Test
    fun testParseGate() {
        val jsonText =this::class.java.classLoader.getResource("test_wptracing.json").readText()
        val r = Route.fromGeoJson(jsonText)
        val waypoint1 = r.elements[0]
        Assert.assertEquals(ProofAreaType.POLYGON, waypoint1.proofArea.type)
        Assert.assertTrue(waypoint1.isInProofArea(createLocation(32.683, 34.84)))
    }

    @Test
    fun testToGeoJson(){
        val jsonText =this::class.java.classLoader.getResource("test_wptracing.json").readText()
        val r = Route.fromGeoJson(jsonText)
        val waypoint1 = r.elements[0]
        val geoJson = waypoint1.toGeoJson()
        val feature = Feature.fromJson(convertStandardJSONString(geoJson))
        val reparsedWaypoint = Gate.fromGeoJson(feature)
        Assert.assertEquals(waypoint1, reparsedWaypoint)
    }

}
