package `in`.avimarine.waypointracing.route

import `in`.avimarine.waypointracing.utils.Serializers
import android.location.Location
import android.os.Parcelable
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
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
    override val routeElementType: RouteElementType = RouteElementType.WAYPOINT,
    @Serializable(with = Serializers.Companion.LocationSerializer::class)
    override val stbdWpt: Location,
    @Serializable(with = Serializers.Companion.LocationSerializer::class)
    override val portWpt: Location,
    override val mandatory: Boolean,
    override val proofArea: ProofArea,
    override val id: Int,
    override val points: Double,
) : RouteElement, Parcelable {
    constructor(
        name: String,
        location: Location,
        mandatory: Boolean,
        bearing1: Double,
        bearing2: Double,
        id: Int,
        points: Double
    ) : this(
        name,
        RouteElementType.WAYPOINT,
        location,
        location,
        mandatory,
        ProofAreaFactory.createProofArea(location, bearing1, bearing2),
        id,
        points
    )

    constructor(
        name: String,
        location: Location,
        mandatory: Boolean,
        dist: Double,
        id: Int,
        points: Double
    ) : this(
        name,
        RouteElementType.WAYPOINT,
        location,
        location,
        mandatory,
        ProofAreaFactory.createProofArea(dist),
        id,
        points
    )

    override fun isInProofArea(loc: Location): Boolean {
        return proofArea.isInProofArea(portWpt, loc)
    }

    override fun toString(): String {
        return name
    }

    companion object {
        fun fromGeoJson(f: Feature): Waypoint {
            val props = f.properties()?:throw JSONException("Failed to get properties")
            val name = props.get("name")?.asString
                ?: throw JSONException("Failed to get Waypoint name")
            val point: Point = f.geometry() as Point
            val loc = Location("")
            loc.latitude = point.latitude()
            loc.longitude = point.longitude()
            val man = props.get("mandatory").asBoolean
            val proofAreaType = try {
                ProofAreaType.valueOf(
                    (props.get("proofAreaType") as JsonElement).asString
                )
            } catch (e: Exception) {
                ProofAreaType.QUADRANT
            }
            val id = props.get("id").asInt
            val points = if (props.has("points")) props.get("points").asDouble else 0.0

            return when (proofAreaType) {
                ProofAreaType.QUADRANT -> {
                    val b1 = (props.get("proofAreaBearings") as JsonArray).get(0).asDouble
                    val b2 = (props.get("proofAreaBearings") as JsonArray).get(1).asDouble
                    Waypoint(name, loc, man, b1, b2, id, points)
                }
                ProofAreaType.CIRCLE -> {
                    val pas = props.get("proofAreaSize")
                    val dist = if (pas is JsonPrimitive) {
                        pas.asDouble
                    } else {
                        (pas as JsonArray).get(0).asDouble
                    }
                    Waypoint(name, loc, man, dist, id, points)
                }
                else -> {
                    throw JsonParseException("Improper proof area for waypoint")
                }
            }

        }

    }
}