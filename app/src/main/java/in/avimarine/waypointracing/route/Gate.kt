package `in`.avimarine.waypointracing.route

import `in`.avimarine.waypointracing.utils.Serializers
import android.location.Location
import android.os.Parcelable
import com.google.gson.JsonArray
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import kotlinx.android.parcel.Parcelize
import kotlinx.serialization.Serializable
import org.json.JSONException

@Parcelize
//@Serializable
class Gate(
    override val name: String,
    override val type: RouteElementType = RouteElementType.GATE,
    @Serializable(with = Serializers.Companion.LocationSerializer::class)
    override val stbdWpt: Location,
    @Serializable(with = Serializers.Companion.LocationSerializer::class)
    override val portWpt: Location,
    override val mandatory: Boolean,
    override val proofArea: ProofArea,
    override var firstTimeInProofArea: Long = -1
) : RouteElement, Parcelable {

    constructor(
        name: String,
        stbdLocation: Location,
        portLocation: Location,
        mandatory: Boolean,
        bearing1: Double,
        bearing2: Double
    ) : this(
        name,
        RouteElementType.GATE,
        stbdLocation,
        portLocation,
        mandatory,
        ProofAreaFactory.createProofArea(stbdLocation, portLocation, bearing1, bearing2)
    )

    constructor(
        name: String,
        stbdLocation: Location,
        portLocation: Location,
        mandatory: Boolean,
        bearing1: Double,
        bearing2: Double,
        dist: Double
    ) : this(
        name,
        RouteElementType.GATE,
        stbdLocation,
        portLocation,
        mandatory,
        ProofAreaFactory.createProofArea(stbdLocation, portLocation, bearing1, bearing2, dist)
    )

    constructor(
        name: String,
        stbdLocation: Location,
        portLocation: Location,
        mandatory: Boolean,
        dist: Double
    ) : this(
        name,
        RouteElementType.GATE,
        stbdLocation,
        portLocation,
        mandatory,
        ProofAreaFactory.createProofArea(stbdLocation, portLocation, dist)
    )

    override fun isInProofArea(loc: Location): Boolean {
        return proofArea.isInProofArea(portWpt, stbdWpt, loc)
    }

    override fun toString(): String {
        return name
    }

    companion object {
        val TAG = "Gate"
        fun fromGeoJson(f: Feature): Gate {
            val name =
                f.properties()?.get("name")?.asString ?: throw JSONException("Failed to get name")
            val line: LineString = f.geometry() as LineString
            val stbdloc = Location("")
            stbdloc.latitude = line.coordinates().get(0).latitude()
            stbdloc.longitude = line.coordinates().get(0).longitude()
            val portloc = Location("")
            portloc.latitude = line.coordinates().get(1).latitude()
            portloc.longitude = line.coordinates().get(1).longitude()
            val man = f.properties()!!.get("mandatory").asBoolean
            if (f.properties()!!.has("proofAreaBearings")!!) {
                val b1 = (f.properties()?.get("proofAreaBearings") as JsonArray).get(0).asDouble
                val b2 = (f.properties()?.get("proofAreaBearings") as JsonArray).get(1).asDouble
                val dist = (f.properties()?.get("proofAreaSize") as JsonArray).get(0).asDouble
                return Gate(name, stbdloc, portloc, man, b1, b2, dist)
            } else {
                val dist = (f.properties()?.get("proofAreaSize") as JsonArray).get(0).asDouble
                return Gate(name, stbdloc, portloc, man, dist)
            }
        }
    }
}