package `in`.avimarine.waypointracing.route

import `in`.avimarine.waypointracing.route.ProofArea
import android.location.Location
import `in`.avimarine.waypointracing.Position
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


interface RouteElement :Parcelable {
    val name : String
    val type : RouteElementType
    val stbdWpt : Location
    val portWpt : Location
    val mandatory : Boolean
    val proofArea : ProofArea
    var firstTimeInProofArea : Long


    fun isInProofArea(loc: Location): Boolean
    fun passedGate(position: Position, forceUpdate:Boolean = false) : Boolean{
        if (forceUpdate || firstTimeInProofArea==-1L){
            firstTimeInProofArea = position.time.time
            return true
        } else if (firstTimeInProofArea>position.time.time){
            firstTimeInProofArea = position.time.time
            return true
        }
        return false
    }
}