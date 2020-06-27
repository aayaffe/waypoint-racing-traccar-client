package avimarine.traccar.client.route

import android.location.Location

class Gate : RouteElement  {
    override val name: String
    override val type = RouteElementType.GATE
    override val stbdWpt : Location
    override val portWpt: Location
    override val mandatory: Boolean

    constructor(name: String, stbdLocation: Location, portLocation: Location, mandatory: Boolean){
        this.name = name
        stbdWpt = stbdLocation
        portWpt = portLocation
        this.mandatory = mandatory
    }
    override fun toString() : String{
        return name
    }
}