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
import `in`.avimarine.androidutils.units.DistanceUnits
import java.util.Locale
import kotlin.math.roundToLong

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
        return formatDirection(Direction(location.bearing.toDouble(), location), magnetic)
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

    fun getPositionStripData(): String {
        return getLatString(location.latitude) + " " + getLonString(location.longitude)
    }

    fun getAccuracyData():String {
        return getLocationAccuracyString(location)
    }

    fun getTimeData():String {
        return RaceDeckFormatter.clock(location.time)
    }
    fun getPortData(): String {
        if (wpt == null){
            return "-----"
        }
        val magnetic = sharedPreferences.getBoolean(SettingsFragment.KEY_MAGNETIC, false)
        return formatDirection(
            getDirection(location, wpt.portWpt),
            magnetic,
        ) + "/" + getDistString(getDistance(location, wpt.portWpt))
    }

    fun getPortEndpointData(): String = endpointData(wpt?.portWpt)

    fun getStbdEndpointData(): String = endpointData(wpt?.stbdWpt)

    private fun endpointData(endpoint: Location?): String {
        if (endpoint == null) return "—"
        val magnetic = sharedPreferences.getBoolean(SettingsFragment.KEY_MAGNETIC, false)
        return formatDirection(getDirection(location, endpoint), magnetic) + "°\n" +
            getDistString(getDistance(location, endpoint)) + " NM"
    }

    fun getActiveTargetBearingData(): String {
        if (wpt == null) return "—"
        val magnetic = sharedPreferences.getBoolean(SettingsFragment.KEY_MAGNETIC, false)
        val direction = if (wpt.routeElementType == RouteElementType.WAYPOINT) {
            getDirection(location, wpt.portWpt)
        } else {
            pointToLineDir(location, wpt.portWpt, wpt.stbdWpt)
        }
        return formatDirection(direction, magnetic) + "°"
    }

    fun getActiveTargetDistanceData(): String {
        if (wpt == null) return "—"
        val distance = if (wpt.routeElementType == RouteElementType.WAYPOINT) {
            getDistance(location, wpt.portWpt)
        } else {
            pointToLineDist(location, wpt.portWpt, wpt.stbdWpt)
        }
        return getDistString(distance) + " NM"
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
        return formatDirection(
            getDirection(location, wpt.stbdWpt),
            magnetic,
        ) + "/" + getDistString(getDistance(location, wpt.stbdWpt))
    }

    private fun formatDirection(direction: Direction, magnetic: Boolean): String {
        val formatted = getDirString(direction, magnetic, false, location)
        return if (formatted.startsWith("360")) "000" + formatted.substring(3) else formatted
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

    fun getETAData(): String {
        if (wpt == null) return "-----"

        val distance = if (wpt.routeElementType == RouteElementType.WAYPOINT) {
            getDistance(location, wpt.portWpt)
        } else {
            pointToLineDist(location, wpt.portWpt, wpt.stbdWpt)
        }
        val vmg = getVMG(location, wpt.portWpt, wpt.stbdWpt)
        val arrival = EtaCalculator.arrivalTimeMillis(
            distance.getValue(DistanceUnits.NauticalMiles),
            vmg.getValue(SpeedUnits.Knots),
            location.time
        ) ?: return "-----"

        return RaceDeckFormatter.eta(arrival)
    }

    fun getTTGData(): String {
        if (wpt == null) return "—"
        val distance = if (wpt.routeElementType == RouteElementType.WAYPOINT) {
            getDistance(location, wpt.portWpt)
        } else {
            pointToLineDist(location, wpt.portWpt, wpt.stbdWpt)
        }
        val vmgKnots = getVMG(location, wpt.portWpt, wpt.stbdWpt).getValue(SpeedUnits.Knots)
        val distanceNm = distance.getValue(DistanceUnits.NauticalMiles)
        if (!vmgKnots.isFinite() || !distanceNm.isFinite() || vmgKnots <= 0 || distanceNm < 0) {
            return "—"
        }
        val seconds = (distanceNm / vmgKnots * 3_600).roundToLong()
        val hours = seconds / 3_600
        val minutes = (seconds % 3_600) / 60
        val remainderSeconds = seconds % 60
        return if (hours == 0L) {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, remainderSeconds)
        } else {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, remainderSeconds)
        }
    }
}
