package `in`.avimarine.waypointracing.route

import android.location.Location
import `in`.avimarine.waypointracing.route.RouteElement
import `in`.avimarine.waypointracing.route.RouteElementType
import com.google.gson.JsonArray
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import org.json.JSONException

class Gate : RouteElement {
    override val name: String
    override val type = RouteElementType.GATE
    override val stbdWpt : Location
    override val portWpt: Location
    override val mandatory: Boolean
    override val proofArea: ProofArea
    override var firstTimeInProofArea: Long = -1


    constructor(name: String, stbdLocation: Location, portLocation: Location, mandatory: Boolean, bearing1: Double, bearing2: Double){
        this.name = name
        stbdWpt = stbdLocation
        portWpt = portLocation
        this.mandatory = mandatory
        this.proofArea = ProofAreaFactory.createProofArea(stbdWpt, portWpt, bearing1, bearing2)
    }
    constructor(name: String, stbdLocation: Location, portLocation: Location, mandatory: Boolean, bearing1: Double, bearing2: Double, dist: Double){
        this.name = name
        stbdWpt = stbdLocation
        portWpt = portLocation
        this.mandatory = mandatory
        this.proofArea = ProofAreaFactory.createProofArea(stbdWpt, portWpt, bearing1, bearing2, dist)
    }
    constructor(name: String, stbdLocation: Location, portLocation: Location, mandatory: Boolean, dist: Double){
        this.name = name
        stbdWpt = stbdLocation
        portWpt = portLocation
        this.mandatory = mandatory
        this.proofArea = ProofAreaFactory.createProofArea(stbdWpt, portWpt, dist)
    }
    override fun isInProofArea(loc: Location):Boolean{
        return proofArea.isInProofArea(portWpt,stbdWpt,loc)
    }
    override fun toString() : String{
        return name
    }

    companion object {
        val TAG = "Gate"
        fun fromGeoJson(f: Feature) : Gate {
            val name = f.properties()?.get("name")?.asString?:throw JSONException("Failed to get name")
            val line : LineString = f.geometry() as LineString
            val stbdloc = Location("")
            stbdloc.latitude = line.coordinates().get(0).latitude()
            stbdloc.longitude = line.coordinates().get(0).longitude()
            val portloc = Location("")
            portloc.latitude = line.coordinates().get(1).latitude()
            portloc.longitude = line.coordinates().get(1).longitude()
            val man =  f.properties()!!.get("mandatory").asBoolean
            if (f.properties()!!.has("proofAreaBearings")!!) {
                val b1 =  (f.properties()?.get("proofAreaBearings") as JsonArray).get(0).asDouble
                val b2 =  (f.properties()?.get("proofAreaBearings") as JsonArray).get(1).asDouble
                val dist =  (f.properties()?.get("proofAreaSize") as JsonArray).get(0).asDouble
                return Gate(name, stbdloc, portloc, man, b1, b2, dist)
            } else {
                val dist = (f.properties()?.get("proofAreaSize") as JsonArray).get(0).asDouble
                return Gate(name, stbdloc, portloc, man, dist)
            }
        }
    }
}