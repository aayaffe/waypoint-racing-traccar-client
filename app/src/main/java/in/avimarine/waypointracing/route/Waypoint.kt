package `in`.avimarine.waypointracing.route

import `in`.avimarine.androidutils.Serializers
import android.location.Location
import android.os.Parcelable
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import `in`.avimarine.androidutils.geo.Distance
import `in`.avimarine.androidutils.units.DistanceUnits
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
        dist: Distance,
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

    override fun toGeoJson(): String {
        var f = Feature.fromGeometry(Point.fromLngLat(portWpt.longitude, portWpt.latitude))
        f.addStringProperty("name", name)
        f.addBooleanProperty("mandatory", mandatory)
        f.addNumberProperty("points", points)

        f.addStringProperty("proofAreaType", proofArea.type.name)


        f = addProofAreaGeoJson(f)
        f.addStringProperty("routeElementType", routeElementType.name)
        f.addNumberProperty("id", id)
        return f.toJson()
    }

    override fun toString(): String {
        return name
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Waypoint

        if (name != other.name) return false
        if (routeElementType != other.routeElementType) return false
        if (stbdWpt.longitude != other.stbdWpt.longitude) return false
        if (stbdWpt.latitude != other.stbdWpt.latitude) return false
        if (portWpt.longitude != other.portWpt.longitude) return false
        if (portWpt.latitude != other.portWpt.latitude) return false
        if (mandatory != other.mandatory) return false
        if (proofArea != other.proofArea) return false
        if (id != other.id) return false
        if (points != other.points) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + routeElementType.hashCode()
        result = 31 * result + stbdWpt.hashCode()
        result = 31 * result + portWpt.hashCode()
        result = 31 * result + mandatory.hashCode()
        result = 31 * result + proofArea.hashCode()
        result = 31 * result + id
        result = 31 * result + points.hashCode()
        return result
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
                    Waypoint(name, loc, man, Distance(dist, DistanceUnits.NauticalMiles), id, points)
                }
                else -> {
                    throw JsonParseException("Improper proof area for waypoint")
                }
            }

        }

    }
}