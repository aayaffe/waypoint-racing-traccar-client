package avimarine.traccar.client.route

import android.location.Location

class Waypoint : RouteElement  {
    override val name: String
    override val type = RouteElementType.WAYPOINT
    override val stbdWpt : Location
    override val portWpt: Location
    override val mandatory: Boolean

    constructor(name: String, location: Location, mandatory: Boolean){
        this.name = name
        stbdWpt = location
        portWpt = location
        this.mandatory = mandatory
    }
    override fun toString() : String{
        return name
    }
}