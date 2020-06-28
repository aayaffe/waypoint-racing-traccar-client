package avimarine.traccar.client.route

import android.location.Location

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

    constructor(wpt: Location){
        this.type = ProofAreaType.CIRCLE
        this.bearings = arrayListOf()
        this.wpts = arrayListOf()
    }


}
