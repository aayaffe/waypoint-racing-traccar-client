package `in`.avimarine.waypointracing.route

import `in`.avimarine.androidutils.Serializers
import android.os.Parcelable
import com.google.gson.JsonParseException
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import `in`.avimarine.androidutils.Utils.Companion.convertStandardJSONString
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONException
import org.json.JSONObject
import java.util.*

@Parcelize
@Serializable
class Route(
    val id: String = "",
    val eventName: String = "",
    val organizing: String = "",
    @Serializable(with = Serializers.Companion.DateSerializer::class)
    val startTime: Date = Date(0),
    val elements: ArrayList<RouteElement> = arrayListOf(),
    @Serializable(with = Serializers.Companion.DateSerializer::class)
    val lastUpdate: Date = Date(0),
    val eventType: EventType = EventType.WPTRACING
): Parcelable {

    fun isEmpty(): Boolean{
        return elements.size == 0
    }

    override fun toString():String{
        return Json.encodeToString(this)
    }

    fun isValidWpt(nextWpt: Int): Boolean {
        return nextWpt<elements.size && nextWpt > -1
    }

    /**
     * Returns the next non optional waypoint.
     * If the current waypoint is the last one, it returns the last one.
     * If the input is -1 the the first non optional waypoint in the route is returned
     * If no non optional waypoint is found, the last waypoint is returned
     */
    fun getNextNonOptionalWpt(currentWpt: Int): Int {
        var i = currentWpt + 1
        while (i < elements.size - 1 && !elements[i].mandatory) {
            i++
        }
        if (i > elements.size - 1) {
            i = elements.size - 1
        }
        return i
    }

    companion object {

        fun fromString(s: String): Route{
            return Json.decodeFromString(s)
        }

        fun fromGeoJson(geoJson: String): Route {
            val json = JSONObject(convertStandardJSONString(geoJson))
            val name = json.getJSONObject("routedata").getString("name")
            val organizing = json.getJSONObject("routedata").getString("organizing")
            val id = json.getJSONObject("routedata").getString("id")
            val date = Date(json.getJSONObject("routedata").getLong("lastUpdate"))
            val features = FeatureCollection.fromJson(geoJson)
                ?: throw JSONException("Unable to parse Route")
            val el: ArrayList<RouteElement> = arrayListOf()
            for (f in features.features()!!) {
                el.add(parseRouteElement(f))
            }
            val et: EventType =
            try{
                EventType.valueOf(json.getJSONObject("routedata").getString("eventType"))
            } catch (e: Exception){
                EventType.WPTRACING
            }
            return Route(id, name, organizing, Date(0), el, date, et)
        }




        private fun parseRouteElement(f: Feature): RouteElement {
            return when (f.properties()?.get("routeElementType")?.asString) {
                "GATE" -> {
                    Gate.fromGeoJson(f)
                }
                "FINISH" -> {
                    Finish.fromGeoJson(f)
                }
                "WAYPOINT" -> {
                    Waypoint.fromGeoJson(f)
                }
                else -> {
                    throw JsonParseException("Unknow object")
                }
            }
        }
        fun emptyRoute(): Route {
            return Route("", "", "", Date(), arrayListOf(), Date(), EventType.WPTRACING)
        }
    }
}