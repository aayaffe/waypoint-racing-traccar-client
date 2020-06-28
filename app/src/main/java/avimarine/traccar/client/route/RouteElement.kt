package avimarine.traccar.client.route

import android.location.Location

interface RouteElement {
    val name : String
    val type : RouteElementType
    val stbdWpt : Location
    val portWpt : Location
    val mandatory : Boolean
//    val proofArea : ProofArea


}