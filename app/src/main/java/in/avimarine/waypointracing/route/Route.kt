package `in`.avimarine.waypointracing.route

import `in`.avimarine.waypointracing.utils.Serializers
import android.os.Parcelable
import com.google.gson.JsonParseException
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import kotlinx.android.parcel.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONException
import org.json.JSONObject
import java.util.*

@Parcelize
@Serializable
class Route(
    val id: String,
    val eventName: String,
    @Serializable(with = Serializers.Companion.DateSerializer::class)
    val startTime: Date,
    val elements: ArrayList<RouteElement>,
    @Serializable(with = Serializers.Companion.DateSerializer::class)
    val lastUpdate: Date,
    val eventType: EventType
): Parcelable {
    fun isEmpty(): Boolean{
        return elements.size == 0
    }

    fun toGeoJson():String{
        return Json.encodeToString(this)
    }

    fun isValidWpt(nextWpt: Int): Boolean {
        return nextWpt<elements.size && nextWpt > -1
    }
    companion object {
        val TAG = "Route"
        fun fromGeoJson(geojson: String): Route {
            val json = JSONObject(convertStandardJSONString(geojson))
            val name = json.getJSONObject("routedata").getString("name")
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
            return Route(id, name, Date(0), el, date, et)
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
            return Route("", "", Date(), arrayListOf(), Date(), EventType.WPTRACING)
        }


    }
}