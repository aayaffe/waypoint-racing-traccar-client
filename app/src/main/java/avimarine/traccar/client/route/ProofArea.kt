package avimarine.traccar.client.route

import android.location.Location
import android.util.Log
import avimarine.traccar.client.TAG
import avimarine.traccar.client.isBetweenAngles
import avimarine.traccar.client.isPointInPolygon
import com.mapbox.geojson.Point
import com.mapbox.turf.TurfMeasurement.bearing

class ProofArea {
    val type : ProofAreaType
    //Bearings will be defined clockwise
    val bearings : ArrayList<Double>
    //Waypoints will be defined in order, as to create a valid polygon
    val wpts : ArrayList<Location>

    constructor(type: ProofAreaType, bearings: ArrayList<Double>, wpts: ArrayList<Location>) {
        this.type = type
        this.bearings = bearings
        this.wpts = wpts
    }

    constructor(bearings: ArrayList<Double>){
        this.type = ProofAreaType.QUADRANT
        this.bearings = bearings
        this.wpts = arrayListOf()
    }

    constructor(type: ProofAreaType, wpts: ArrayList<Location>){
        this.type = ProofAreaType.POLYGON
        this.bearings = arrayListOf()
        this.wpts = wpts
    }

    fun isInProofArea(portWpt: Location, stbdWpt: Location, loc:Location):Boolean{
        if (type==ProofAreaType.POLYGON){
            return isPointInPolygon(wpts,loc)
        } else if (type==ProofAreaType.QUADRANT){
            val b1 = bearing(Point.fromLngLat(portWpt.longitude,portWpt.latitude), Point.fromLngLat(loc.longitude,loc.latitude))
            val b2 = bearing(Point.fromLngLat(stbdWpt.longitude,stbdWpt.latitude), Point.fromLngLat(loc.longitude,loc.latitude))
            return (isBetweenAngles(bearings[0], bearings[1], b1) || isBetweenAngles(bearings[0], bearings[1], b2))
        }
        Log.e(TAG,"Unknown type of ProofArea for isInProofArea")
        return false
    }

    fun isInProofArea(wpt: Location, loc:Location):Boolean{
        if (type==ProofAreaType.POLYGON){
            return isPointInPolygon(wpts,loc)
        } else if (type==ProofAreaType.QUADRANT){
            val b1 = bearing(Point.fromLngLat(wpt.longitude,wpt.latitude), Point.fromLngLat(loc.longitude,loc.latitude))
            return (isBetweenAngles(bearings[0], bearings[1], b1))
        }
        Log.e(TAG,"Unknown type of ProofArea for isInProofArea")
        return false
    }

}
