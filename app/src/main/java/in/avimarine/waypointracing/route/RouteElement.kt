package `in`.avimarine.waypointracing.route

import android.location.Location
import android.os.Parcelable
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import com.mapbox.geojson.Feature
import `in`.avimarine.androidutils.units.DistanceUnits
import kotlinx.serialization.Serializable

@Serializable
sealed interface RouteElement :Parcelable {
    val name : String
    val routeElementType : RouteElementType
    val stbdWpt : Location
    val portWpt : Location
    val mandatory : Boolean
    val proofArea : ProofArea
    val id : Int
    val points: Double

    fun isInProofArea(loc: Location): Boolean
    fun toGeoJson(): String

    fun addProofAreaGeoJson(f: Feature): Feature {
        when (proofArea.type) {
            ProofAreaType.QUADRANT -> {
                val arr = JsonArray()
                if (proofArea.bearings.size == 2) {
                    proofArea.bearings.forEach {
                        arr.add(JsonPrimitive(it))
                    }
                    f.addProperty("proofAreaBearings", arr)
                }
            }

            ProofAreaType.CIRCLE -> {
                f.addNumberProperty(
                    "proofAreaSize",
                    proofArea.distance.getValue(DistanceUnits.NauticalMiles)
                )
            }

            ProofAreaType.POLYGON -> {
                f.addNumberProperty(
                    "proofAreaSize",
                    proofArea.distance.getValue(DistanceUnits.NauticalMiles)
                )
                val arr = JsonArray()
                if (proofArea.bearings.size == 2) {
                    proofArea.bearings.forEach {
                        arr.add(JsonPrimitive(it))
                    }
                    f.addProperty("proofAreaBearings", arr)
                }
            }
        }
        return f
    }
}