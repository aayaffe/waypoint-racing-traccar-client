package `in`.avimarine.waypointracing.route

import android.location.Location
import android.os.Parcelable
import kotlinx.serialization.Serializable

@Serializable
sealed interface RouteElement :Parcelable {
    val name : String
    val routeElementType : RouteElementType
    val stbdWpt : Location
    val portWpt : Location
    val mandatory : Boolean
    val proofArea : ProofArea
    val id : Int
    val points: Double

    fun isInProofArea(loc: Location): Boolean
}