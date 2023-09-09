package `in`.avimarine.waypointracing.route

import `in`.avimarine.androidutils.Serializers
import android.location.Location
import android.os.Parcelable
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import `in`.avimarine.androidutils.geo.Distance
import `in`.avimarine.androidutils.units.DistanceUnits
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.json.JSONException

@Serializable
@Parcelize
class Finish(
    override val name: String,
    override val routeElementType: RouteElementType = RouteElementType.FINISH,
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
        stbdLocation: Location,
        portLocation: Location,
        bearing1: Double,
        bearing2: Double,
        dist: Distance,
        id: Int,
        points: Double = 0.0
    ) : this(
        name,
        RouteElementType.FINISH,
        stbdLocation,
        portLocation,
        true,
        ProofAreaFactory.createProofArea(stbdLocation, portLocation, bearing1, bearing2, dist),
        id,
        points
    )

    constructor(
        name: String,
        stbdLocation: Location,
        portLocation: Location,
        dist: Distance,
        id: Int,
        points: Double = 0.0
    ) :
            this(
                name,
                RouteElementType.FINISH,
                stbdLocation,
                portLocation,
                true,
                ProofAreaFactory.createProofArea(stbdLocation, portLocation, dist),
                id,
                points
            )

    override fun isInProofArea(loc: Location): Boolean {
        return proofArea.isInProofArea(portWpt, stbdWpt, loc)
    }

    override fun toString(): String {
        return name
    }

    override fun toGeoJson(): String {
        var f = Feature.fromGeometry(LineString.fromLngLats(arrayListOf(
            Point.fromLngLat(stbdWpt.longitude, stbdWpt.latitude),
            Point.fromLngLat(portWpt.longitude, portWpt.latitude)
        )))
        f.addStringProperty("name", name)
        f.addBooleanProperty("mandatory", mandatory)
        f.addNumberProperty("points", points)

        f.addStringProperty("proofAreaType", proofArea.type.name)


        f = addProofAreaGeoJson(f)
        f.addStringProperty("routeElementType", routeElementType.name)
        f.addNumberProperty("id", id)
        return f.toJson()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Finish

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
        fun fromGeoJson(f: Feature): Finish {
            val props = f.properties()?:throw JSONException("Failed to get properties")
            val name =
                props.get("name")?.asString ?: throw JSONException("Failed to get name")
            val line: LineString = f.geometry() as LineString
            val stbdloc = Location("")
            stbdloc.latitude = line.coordinates()[0].latitude()
            stbdloc.longitude = line.coordinates()[0].longitude()
            val portloc = Location("")
            portloc.latitude = line.coordinates()[1].latitude()
            portloc.longitude = line.coordinates()[1].longitude()
            val id = props.get("id").asInt
            val points = if (props.has("points")) props.get("points").asDouble else 0.0
            return if (props.has("proofAreaBearings")) {
                val b1 = (props.get("proofAreaBearings") as JsonArray).get(0).asDouble
                val b2 = (props.get("proofAreaBearings") as JsonArray).get(1).asDouble
                val pas = props.get("proofAreaSize")
                val dist = if (pas is JsonPrimitive) {
                    pas.asDouble
                } else {
                    (pas as JsonArray).get(0).asDouble
                }
                Finish(name, stbdloc, portloc, b1, b2, Distance(dist, DistanceUnits.NauticalMiles), id, points)
            } else {
                val pas = props.get("proofAreaSize")
                val dist = if (pas is JsonPrimitive) {
                    pas.asDouble
                } else {
                    (pas as JsonArray).get(0).asDouble
                }
                Finish(name, stbdloc, portloc, Distance(dist, DistanceUnits.NauticalMiles), id, points)
            }
        }
    }
}