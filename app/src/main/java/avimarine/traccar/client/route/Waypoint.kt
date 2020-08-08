package avimarine.traccar.client.route

import android.location.Location
import com.google.gson.JsonArray
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList

class Waypoint : RouteElement  {
    override val name: String
    override val type = RouteElementType.WAYPOINT
    override val stbdWpt : Location
    override val portWpt: Location
    override val mandatory: Boolean
    override val proofArea: ProofArea
    override var firstTimeInProofArea: Long = -1

    constructor(name: String, location: Location, mandatory: Boolean, bearing1: Double, bearing2:Double){
        this.name = name
        stbdWpt = location
        portWpt = location
        this.mandatory = mandatory
        this.proofArea = ProofAreaFactory.createProofArea(stbdWpt,bearing1,bearing2)
    }
    override fun isInProofArea(loc: Location):Boolean{
        return proofArea.isInProofArea(portWpt,loc)
    }
    override fun toString() : String{
        return name
    }

    companion object {
        val TAG = "Waypoint"
        fun fromGeoJson(f: Feature) : Waypoint{
            val name = f.properties()?.get("name")?.asString?:throw JSONException("Failed to get Waypoint name")
            val point : Point = f.geometry() as Point
            val loc = Location("")
            loc.latitude = point.latitude()
            loc.longitude = point.longitude()
            val man =  f.properties()!!.get("mandatory").asBoolean
            val b1 =  (f.properties()?.get("proofAreaBearings") as JsonArray).get(0).asDouble
            val b2 =  (f.properties()?.get("proofAreaBearings") as JsonArray).get(1).asDouble
            return Waypoint(name, loc, man, b1, b2)
        }

    }
}