@file:UseSerializers(Serializers.Companion.LocationSerializer::class)
package `in`.avimarine.waypointracing.route

import android.location.Location
import android.os.Parcelable
import android.util.Log
import com.mapbox.geojson.Point
import com.mapbox.turf.TurfMeasurement.bearing
import `in`.avimarine.androidutils.*
import `in`.avimarine.androidutils.geo.Distance
import `in`.avimarine.androidutils.units.DistanceUnits
import `in`.avimarine.waypointracing.utils.*
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Parcelize
@Serializable
class ProofArea  (
    var type : ProofAreaType,
    //Bearings will be defined clockwise
    var bearings : ArrayList<Double>,
    //Waypoints will be defined in order, as to create a valid polygon
//    @Serializable(with = Serializers.Companion.LocationSerializer::class)
    var wpts : ArrayList<Location>,
    var distance : Distance
) : Parcelable{

    constructor(dist: Distance) : this(ProofAreaType.CIRCLE, arrayListOf(), arrayListOf(), dist)

    constructor(bearings: ArrayList<Double>) : this(ProofAreaType.QUADRANT, bearings, arrayListOf(),Distance(0.0, DistanceUnits.NauticalMiles))

    constructor(type: ProofAreaType, wpts: ArrayList<Location>, dist: Distance):this(type, arrayListOf(), wpts, dist)

    fun isInProofArea(portWpt: Location, stbdWpt: Location, loc:Location):Boolean{
        if (type== ProofAreaType.POLYGON){
            return isPointInPolygon(wpts,loc)
        } else if (type== ProofAreaType.QUADRANT){
            val b1 = bearing(Point.fromLngLat(portWpt.longitude,portWpt.latitude), Point.fromLngLat(loc.longitude,loc.latitude))
            val b2 = bearing(Point.fromLngLat(stbdWpt.longitude,stbdWpt.latitude), Point.fromLngLat(loc.longitude,loc.latitude))
            return (isBetweenAngles(bearings[0], bearings[1], b1) || isBetweenAngles(bearings[0], bearings[1], b2))
        }
        Log.e(TAG,"Unknown type of ProofArea for isInProofArea")
        return false
    }

    fun isInProofArea(wpt: Location, loc:Location):Boolean{
        when (type) {
            ProofAreaType.POLYGON -> {
                return isPointInPolygon(wpts,loc)
            }
            ProofAreaType.QUADRANT -> {
                val b1 = bearing(Point.fromLngLat(wpt.longitude,wpt.latitude), Point.fromLngLat(loc.longitude,loc.latitude))
                return (isBetweenAngles(bearings[0], bearings[1], b1))
            }
            ProofAreaType.CIRCLE -> {
                return getDistance(wpt, loc).getValue(DistanceUnits.NauticalMiles) < distance.getValue(DistanceUnits.NauticalMiles)
            }
            else -> {
                Log.e(TAG, "Unknown type of ProofArea for isInProofArea")
                return false
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProofArea

        if (type != other.type) return false
        val allwpts = wpts.zip(other.wpts)
        for (wpt in allwpts){
            if (wpt.first.latitude != wpt.second.latitude) return false
            if (wpt.first.longitude != wpt.second.longitude) return false
        }
        if (bearings != other.bearings) return false
        if (distance != other.distance) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + bearings.hashCode()
        result = 31 * result + wpts.hashCode()
        result = 31 * result + distance.hashCode()
        return result
    }


}
