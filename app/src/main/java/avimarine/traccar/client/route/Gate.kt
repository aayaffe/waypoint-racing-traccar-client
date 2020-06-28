package avimarine.traccar.client.route

import android.location.Location

class Gate : RouteElement  {
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
        this.proofArea = ProofAreaFactory.createProofArea(stbdWpt,portWpt,bearing1,bearing2)
    }
    constructor(name: String, stbdLocation: Location, portLocation: Location, mandatory: Boolean, bearing1: Double, bearing2: Double, dist: Double){
        this.name = name
        stbdWpt = stbdLocation
        portWpt = portLocation
        this.mandatory = mandatory
        this.proofArea = ProofAreaFactory.createProofArea(stbdWpt,portWpt,bearing1,bearing2,dist)
    }
    constructor(name: String, stbdLocation: Location, portLocation: Location, mandatory: Boolean, dist: Double){
        this.name = name
        stbdWpt = stbdLocation
        portWpt = portLocation
        this.mandatory = mandatory
        this.proofArea = ProofAreaFactory.createProofArea(stbdWpt,portWpt,dist)
    }
    override fun isInProofArea(loc: Location):Boolean{
        return proofArea.isInProofArea(portWpt,stbdWpt,loc)
    }
    override fun toString() : String{
        return name
    }
}