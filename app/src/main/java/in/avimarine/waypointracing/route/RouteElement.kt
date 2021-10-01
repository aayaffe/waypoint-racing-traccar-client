package `in`.avimarine.waypointracing.route

import `in`.avimarine.waypointracing.Position
import android.location.Location
import android.os.Parcelable
import kotlinx.serialization.Serializable

sealed interface RouteElement :Parcelable {
    val name : String
    val type : RouteElementType
    val stbdWpt : Location
    val portWpt : Location
    val mandatory : Boolean
    val proofArea : ProofArea

    fun isInProofArea(loc: Location): Boolean
}