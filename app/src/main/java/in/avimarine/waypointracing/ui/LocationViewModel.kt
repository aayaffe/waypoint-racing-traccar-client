package `in`.avimarine.waypointracing.ui

import `in`.avimarine.waypointracing.Position
import `in`.avimarine.waypointracing.activities.SettingsFragment
import `in`.avimarine.waypointracing.route.ProofAreaType
import `in`.avimarine.waypointracing.route.RouteElement
import `in`.avimarine.waypointracing.route.RouteElementType
import `in`.avimarine.waypointracing.utils.*
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import kotlin.math.cos

class LocationViewModel(
    val position: Position,
    val wpt: RouteElement?,
    val sharedPreferences: SharedPreferences
) : ViewModel() {
    fun getCOGData(): String{
        val magnetic = sharedPreferences.getBoolean(SettingsFragment.KEY_MAGNETIC, false)
        return getDirString(position.course, magnetic, false, position, position.time.time)
    }
    fun getSOGData(): String{
        return getSpeedString(position.speed)
    }
    fun getLocationData(): String {
        return getLatString(position.latitude) + "\n" + getLonString(position.longitude)
    }
    fun getTimeData():String {
        return timeStamptoDateString(position.time.time)
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
        if (wpt.type == RouteElementType.WAYPOINT) {
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
        val s = position.speed;
        val brg = pointToLineDir(position.toLocation(), wpt.portWpt, wpt.stbdWpt)
        val dif = if ((brg - position.course)<0) brg - position.course + 360 else brg - position.course
        return getSpeedString(s * cos(Math.toRadians(dif)))
    }
}