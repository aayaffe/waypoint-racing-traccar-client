@file:UseSerializers(Serializers.Companion.LocationSerializer::class)
package `in`.avimarine.waypointracing.route

import `in`.avimarine.waypointracing.TAG
import `in`.avimarine.waypointracing.utils.*
import android.location.Location
import android.util.Log
import android.os.Parcelable
import com.mapbox.geojson.Point
import com.mapbox.turf.TurfMeasurement.bearing
import kotlinx.android.parcel.Parcelize
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
    var distance : Double
) : Parcelable{

    constructor(dist: Double) : this(ProofAreaType.CIRCLE, arrayListOf(), arrayListOf(), dist)

    constructor(bearings: ArrayList<Double>) : this(ProofAreaType.QUADRANT, bearings, arrayListOf(),0.0)

    constructor(type: ProofAreaType, wpts: ArrayList<Location>):this(type, arrayListOf(), wpts, 0.0)

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
                return getDistance(wpt, loc) < toMeters(distance)
            }
            else -> {
                Log.e(TAG, "Unknown type of ProofArea for isInProofArea")
                return false
            }
        }
    }

}