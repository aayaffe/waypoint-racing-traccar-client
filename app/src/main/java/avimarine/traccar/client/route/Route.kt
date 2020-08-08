package avimarine.traccar.client.route

import android.util.JsonReader
import android.util.Log
import com.google.gson.JsonParseException
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import org.json.JSONException
import org.json.JSONObject
import java.text.ParseException
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList

class Route (val eventName:String, val startTime: Date, val elements: ArrayList<RouteElement>, val lastUpdate: Date){
    companion object {
        val TAG = "Route"
        fun fromGeoJson(geojson: String) : Route{
            val json = JSONObject(geojson)
            val name = json.getJSONObject("routedata").getString("name")
            val features = FeatureCollection.fromJson(geojson)
                    ?: throw JSONException("Unable to parse Route")
            val el : ArrayList<RouteElement> = arrayListOf()
            for (f in features.features()!!){
                el.add(parseRouteElement(f))
            }
            return Route(name, Date(0), el, Date(0))
        }
        private fun parseRouteElement(f: Feature): RouteElement {
            return when {
                f.properties()?.get("routeElementType")?.asString == "GATE" -> {
                    Gate.fromGeoJson(f)
                }
                f.properties()?.get("routeElementType")?.asString == "FINISH" -> {
                    Finish.fromGeoJson(f)
                }
                f.properties()?.get("routeElementType")?.asString == "WAYPOINT" -> {
                    Waypoint.fromGeoJson(f)
                }
                else -> {
                    throw JsonParseException("Unknow object")
                }
            }
        }
    }
}