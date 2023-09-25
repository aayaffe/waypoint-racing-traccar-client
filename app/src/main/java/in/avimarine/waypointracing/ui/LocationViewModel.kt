package `in`.avimarine.waypointracing.ui

import `in`.avimarine.waypointracing.activities.SettingsFragment
import `in`.avimarine.waypointracing.route.ProofAreaType
import `in`.avimarine.waypointracing.route.RouteElement
import `in`.avimarine.waypointracing.route.RouteElementType
import `in`.avimarine.waypointracing.utils.*
import android.content.SharedPreferences
import android.graphics.Color
import android.location.Location
import android.os.Build
import androidx.lifecycle.ViewModel
import `in`.avimarine.androidutils.*
import `in`.avimarine.androidutils.geo.Direction
import `in`.avimarine.androidutils.geo.Speed
import `in`.avimarine.androidutils.units.SpeedUnits

class LocationViewModel(
    val location: Location,
    private val wpt: RouteElement?,
    val sharedPreferences: SharedPreferences
) : ViewModel() {

    val mock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        location.isMock
    } else {
        location.isFromMockProvider
    }

    fun getCOGData(): String{
        val magnetic = sharedPreferences.getBoolean(SettingsFragment.KEY_MAGNETIC, false)
        return getDirString(Direction(location.bearing.toDouble(), location), magnetic, false, location)
    }

    fun getCOGSOGData(): String{
        val cog = getCOGData()
        val sog = getSOGData()
        return "$cog/$sog"
    }
    fun getCOGColor(): Int{
        if (wpt== null)
            return -65536
        return when (wpt.routeElementType) {
            RouteElementType.WAYPOINT -> Color.BLACK
            else -> {
                val portBearing = getDirection(location, wpt.portWpt)
                val stbdBearing = getDirection(location, wpt.stbdWpt)
                if (isBetweenAngles(portBearing, stbdBearing, Direction(location.bearing.toDouble(), location)) && getVMG(location, wpt.portWpt, wpt.stbdWpt).getValue(SpeedUnits.Knots)>0) {
                    Color.GREEN
                } else {
                    Color.BLACK
                }

            }
        }
    }
    fun getSOGData(): String{
        return getSpeedString(Speed(location.speed.toDouble(), SpeedUnits.MetersPerSecond), SpeedUnits.Knots, false)
    }
    fun getLocationData(): String {
        return getLatString(location.latitude) + "\n" + getLonString(location.longitude)
    }

    fun getAccuracyData():String {
        return getLocationAccuracyString(location)
    }

    fun getTimeData():String {
        return timeStampToDateString(location.time)
    }
    fun getPortData(): String {
        if (wpt == null){
            return "-----"
        }
        val magnetic = sharedPreferences.getBoolean(SettingsFragment.KEY_MAGNETIC, false)
        return getDirString(
            getDirection(location, wpt.portWpt),
            magnetic,
            false,
            location
        ) + "/" + getDistString(getDistance(location, wpt.portWpt))
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
            getDirection(location, wpt.stbdWpt),
            magnetic,
            false,
            location
        ) + "/" + getDistString(getDistance(location, wpt.stbdWpt))
    }

    fun getShortestDistanceToGateData(): String {
        if (wpt==null){
            return "-----"
        }
        return getDistString(pointToLineDist(location, wpt.portWpt, wpt.stbdWpt))
    }

    fun getVMGGateData(): String{
        if (wpt==null){
            return "-----"
        }
        val vmg = getVMG(location, wpt.portWpt, wpt.stbdWpt)
        return getSpeedString(vmg,SpeedUnits.Knots,false)
    }
}