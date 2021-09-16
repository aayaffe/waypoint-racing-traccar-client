package `in`.avimarine.waypointracing.route

import `in`.avimarine.waypointracing.route.ProofArea
import `in`.avimarine.waypointracing.route.ProofAreaFactory
import android.location.Location
import android.os.Parcelable
import com.google.gson.JsonArray
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import kotlinx.android.parcel.Parcelize
import org.json.JSONException

@Parcelize
class Waypoint(
    override val name: String,
    override val type: RouteElementType = RouteElementType.WAYPOINT,
    override val stbdWpt: Location,
    override val portWpt: Location,
    override val mandatory: Boolean,
    override val proofArea: ProofArea,
    override var firstTimeInProofArea: Long = -1
) : RouteElement, Parcelable {
    constructor(
        name: String,
        location: Location,
        mandatory: Boolean,
        bearing1: Double,
        bearing2: Double
    ) : this(
        name,
        RouteElementType.WAYPOINT,
        location,
        location,
        mandatory,
        ProofAreaFactory.createProofArea(location, bearing1, bearing2)
    )

    override fun isInProofArea(loc: Location): Boolean {
        return proofArea.isInProofArea(portWpt, loc)
    }

    override fun toString(): String {
        return name
    }

    companion object {
        val TAG = "Waypoint"
        fun fromGeoJson(f: Feature): Waypoint {
            val name = f.properties()?.get("name")?.asString
                ?: throw JSONException("Failed to get Waypoint name")
            val point: Point = f.geometry() as Point
            val loc = Location("")
            loc.latitude = point.latitude()
            loc.longitude = point.longitude()
            val man = f.properties()!!.get("mandatory").asBoolean
            val b1 = (f.properties()?.get("proofAreaBearings") as JsonArray).get(0).asDouble
            val b2 = (f.properties()?.get("proofAreaBearings") as JsonArray).get(1).asDouble
            return Waypoint(name, loc, man, b1, b2)
        }

    }
}