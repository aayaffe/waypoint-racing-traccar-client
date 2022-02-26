package `in`.avimarine.waypointracing.route

import android.location.Location
import android.os.Parcelable

sealed interface RouteElement :Parcelable {
    val name : String
    val type : RouteElementType
    val stbdWpt : Location
    val portWpt : Location
    val mandatory : Boolean
    val proofArea : ProofArea
    val id : Int

    fun isInProofArea(loc: Location): Boolean
}