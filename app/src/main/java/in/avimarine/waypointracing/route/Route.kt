package `in`.avimarine.waypointracing.route

import `in`.avimarine.androidutils.Serializers
import android.os.Parcelable
import com.google.gson.JsonParseException
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
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
    companion object {
        val TAG = "Route"

        fun fromString(s: String): Route{
            return Json.decodeFromString(s)
        }

        fun fromGeoJson(geojson: String): Route {
            val json = JSONObject(convertStandardJSONString(geojson))
            val name = json.getJSONObject("routedata").getString("name")
            val organizing = json.getJSONObject("routedata").getString("organizing")
            val id = json.getJSONObject("routedata").getString("id")
            val date = Date(json.getJSONObject("routedata").getLong("lastUpdate"))
            val features = FeatureCollection.fromJson(geojson)
                ?: throw JSONException("Unable to parse Route")
            val el: ArrayList<RouteElement> = arrayListOf()
            for (f in features.features()!!) {
                el.add(parseRouteElement(f))
            }
            var et: EventType =
            try{
                EventType.valueOf(json.getJSONObject("routedata").getString("eventType"))
            } catch (e: Exception){
                EventType.WPTRACING
            }
            return Route(id, name, organizing, Date(0), el, date, et)
        }


        private fun convertStandardJSONString(data_json: String): String {
            var ret = data_json.replace("\\\\r\\\\n", "");
            ret = ret.replace("\"{", "{");
            ret = ret.replace("}\",", "},");
            ret = ret.replace("}\"", "}");
            return ret;
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

        fun emptyRoute(): Route {
            return Route("", "", "", Date(), arrayListOf(), Date(), EventType.WPTRACING)
        }


    }
}