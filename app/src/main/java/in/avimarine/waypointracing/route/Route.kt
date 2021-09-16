package `in`.avimarine.waypointracing.route

import `in`.avimarine.waypointracing.route.Finish
import `in`.avimarine.waypointracing.route.Gate
import android.os.Parcelable
import com.google.gson.JsonParseException
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import kotlinx.android.parcel.Parcelize
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList
@Parcelize
class Route(
    val eventName: String,
    val startTime: Date,
    val elements: ArrayList<RouteElement>,
    val lastUpdate: Date
): Parcelable {
    companion object {
        val TAG = "Route"
        fun fromGeoJson(geojson: String): Route {

            val json = JSONObject(convertStandardJSONString(geojson))
            val name = json.getJSONObject("routedata").getString("name")
            val features = FeatureCollection.fromJson(geojson)
                ?: throw JSONException("Unable to parse Route")
            val el: ArrayList<RouteElement> = arrayListOf()
            for (f in features.features()!!) {
                el.add(parseRouteElement(f))
            }
            return Route(name, Date(0), el, Date(0))
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
    }
}