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
class WaypointTest {

    @Test
    fun testParseCircleWaypoint() {
        val jsonText =this::class.java.classLoader.getResource("test_treasure.json").readText()
        val r = Route.fromGeoJson(jsonText)
        val waypoint1 = r.elements[0]
        Assert.assertEquals(ProofAreaType.CIRCLE, waypoint1.proofArea.type)
        Assert.assertTrue(waypoint1.isInProofArea(createLocation(32.835, 35.02)))
    }

    @Test
    fun testToGeoJson(){
        val jsonText =this::class.java.classLoader.getResource("test_treasure.json").readText()
        val r = Route.fromGeoJson(jsonText)
        val waypoint1 = r.elements[0]
        val geoJson = waypoint1.toGeoJson()
        Assert.assertEquals(geoJson, "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[35.019288,32.834881]},\"properties\":{\"name\":\"Treasure 1\",\"mandatory\":false,\"points\":0.0,\"proofAreaType\":\"CIRCLE\",\"proofAreaSize\":0.06,\"routeElementType\":\"WAYPOINT\",\"id\":0}}")
        val feature = Feature.fromJson(convertStandardJSONString(geoJson))
        val reparsedWaypoint = Waypoint.fromGeoJson(feature)
        Assert.assertEquals(waypoint1, reparsedWaypoint)
    }

}
