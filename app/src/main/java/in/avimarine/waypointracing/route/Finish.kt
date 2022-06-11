package `in`.avimarine.waypointracing.route

import `in`.avimarine.waypointracing.utils.Serializers
import android.location.Location
import android.os.Parcelable
import com.google.gson.JsonArray
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
//import kotlinx.android.parcel.Parcelize
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
        dist: Double,
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
        dist: Double,
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
                val dist = (props.get("proofAreaSize") as JsonArray).get(0).asDouble
                Finish(name, stbdloc, portloc, b1, b2, dist, id, points)
            } else {
                val dist = (props.get("proofAreaSize") as JsonArray).get(0).asDouble
                Finish(name, stbdloc, portloc, dist, id, points)
            }
        }
    }
}