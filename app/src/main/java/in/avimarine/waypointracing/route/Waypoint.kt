package `in`.avimarine.waypointracing.route

import `in`.avimarine.waypointracing.utils.Serializers
import android.location.Location
import android.os.Parcelable
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
//import kotlinx.android.parcel.Parcelize
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.json.JSONException

@Serializable
@Parcelize
class Waypoint(
    override val name: String,
    override val type: RouteElementType = RouteElementType.WAYPOINT,
    @Serializable(with = Serializers.Companion.LocationSerializer::class)
    override val stbdWpt: Location,
    @Serializable(with = Serializers.Companion.LocationSerializer::class)
    override val portWpt: Location,
    override val mandatory: Boolean,
    override val proofArea: ProofArea,
    override val id: Int,
) : RouteElement, Parcelable {
    constructor(
        name: String,
        location: Location,
        mandatory: Boolean,
        bearing1: Double,
        bearing2: Double,
        id: Int
    ) : this(
        name,
        RouteElementType.WAYPOINT,
        location,
        location,
        mandatory,
        ProofAreaFactory.createProofArea(location, bearing1, bearing2),
        id
    )

    constructor(name: String,
                location: Location,
                mandatory: Boolean,
                dist: Double,
    id: Int
    ) : this(name, RouteElementType.WAYPOINT, location, location, mandatory, ProofAreaFactory.createProofArea(dist), id)

    override fun isInProofArea(loc: Location): Boolean {
        return proofArea.isInProofArea(portWpt, loc)
    }

    override fun toString(): String {
        return name
    }

    companion object {
        fun fromGeoJson(f: Feature): Waypoint {
            val name = f.properties()?.get("name")?.asString
                ?: throw JSONException("Failed to get Waypoint name")
            val point: Point = f.geometry() as Point
            val loc = Location("")
            loc.latitude = point.latitude()
            loc.longitude = point.longitude()
            val man = f.properties()!!.get("mandatory").asBoolean
            val proofAreaType = try {
                ProofAreaType.valueOf((f.properties()?.get("proofAreaType") as JsonElement).asString)
            } catch (e: Exception){
                ProofAreaType.QUADRANT
            }
            val id = f.properties()!!.get("id").asInt
            return when (proofAreaType) {
                ProofAreaType.QUADRANT -> {
                    val b1 = (f.properties()?.get("proofAreaBearings") as JsonArray).get(0).asDouble
                    val b2 = (f.properties()?.get("proofAreaBearings") as JsonArray).get(1).asDouble
                    Waypoint(name, loc, man, b1, b2, id)
                }
                ProofAreaType.CIRCLE -> {
                    val d = (f.properties()?.get("proofAreaSize") as JsonElement).asDouble
                    Waypoint(name, loc, man, d, id)
                }
                else -> {
                    throw JsonParseException("Improper proof area for waypoint")
                }
            }

        }

    }
}