package `in`.avimarine.waypointracing.ui

import `in`.avimarine.waypointracing.activities.SettingsFragment
import `in`.avimarine.waypointracing.route.ProofAreaType
import `in`.avimarine.waypointracing.route.RouteElement
import `in`.avimarine.waypointracing.route.RouteElementType
import `in`.avimarine.waypointracing.utils.*
import android.content.SharedPreferences
import android.graphics.Color
import androidx.lifecycle.ViewModel
import `in`.avimarine.androidutils.*
import `in`.avimarine.androidutils.geo.Speed
import `in`.avimarine.androidutils.units.SpeedUnits

class LocationViewModel(
    val position: Position,
    val wpt: RouteElement?,
    val sharedPreferences: SharedPreferences
) : ViewModel() {
    fun getCOGData(): String{
        val magnetic = sharedPreferences.getBoolean(SettingsFragment.KEY_MAGNETIC, false)
        return getDirString(position.course, magnetic, false, position, position.time.time)
    }
    fun getCOGColor(): Int{
        if (wpt== null)
            return -65536
        return when (wpt.routeElementType) {
            RouteElementType.WAYPOINT -> Color.BLACK
            else -> {
                val portBearing = getDirection(position, wpt.portWpt)
                val stbdBearing = getDirection(position, wpt.stbdWpt)
                if (isBetweenAngles(portBearing, stbdBearing, position.course) && getVMG(position, wpt.portWpt, wpt.stbdWpt)>0) {
                    Color.GREEN
                } else {
                    Color.BLACK
                }

            }
        }
    }
    fun getSOGData(): String{
        return getSpeedString(Speed(position.speed, SpeedUnits.Knots), SpeedUnits.Knots)
    }
    fun getLocationData(): String {
        return getLatString(position.latitude) + "\n" + getLonString(position.longitude)
    }

    fun getAccuracyData():String {
        return if (position.accuracy>0) {position.accuracy.toString()} else ""
    }

    fun getTimeData():String {
        return timeStampToDateString(position.time.time)
    }
    fun getPortData(): String {
        if (wpt == null){
            return "-----"
        }
        val magnetic = sharedPreferences.getBoolean(SettingsFragment.KEY_MAGNETIC, false)
        return getDirString(
            getDirection(position, wpt.portWpt),
            magnetic,
            false,
            position,
            position.time.time
        ) + "/" + getDistString(getDistance(position, wpt.portWpt))
    }

    fun getStbdData(): String {
        if (wpt == null){
            return "-----"
        }
        if (wpt.routeElementType == RouteElementType.WAYPOINT) {
            if (wpt.proofArea.type == ProofAreaType.QUADRANT) {
                return getPointOfCompass(
                    wpt.proofArea.bearings[0],
                    wpt.proofArea.bearings[1]
                )
            } else {
                return "-----"
            }
        }
        val magnetic = sharedPreferences.getBoolean(SettingsFragment.KEY_MAGNETIC, false)
        return getDirString(
            getDirection(position, wpt.stbdWpt),
            magnetic,
            false,
            position,
            position.time.time
        ) + "/" + getDistString(getDistance(position, wpt.stbdWpt))
    }

    fun getShortestDistanceToGateData(): String {
        if (wpt==null){
            return "-----"
        }
        return getDistString(pointToLineDist(position.toLocation(), wpt.portWpt, wpt.stbdWpt))
    }

    fun getVMGGateData(): String{
        if (wpt==null){
            return "-----"
        }
        val vmg = getVMG(position, wpt.portWpt, wpt.stbdWpt)
        return getSpeedString(Speed(vmg,SpeedUnits.Knots),SpeedUnits.Knots)
    }
}