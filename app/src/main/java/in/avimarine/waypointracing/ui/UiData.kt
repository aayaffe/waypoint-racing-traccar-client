package `in`.avimarine.waypointracing.ui

import `in`.avimarine.waypointracing.Position
import `in`.avimarine.waypointracing.activities.SettingsFragment
import `in`.avimarine.waypointracing.route.RouteElement
import `in`.avimarine.waypointracing.route.RouteElementType
import `in`.avimarine.waypointracing.utils.*
import android.content.SharedPreferences


class UiData {

    companion object {

        fun getPortData(
            position: Position,
            wpt: RouteElement?,
            sharedPreferences: SharedPreferences
        ): String {
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

        fun getStbdData(
            position: Position,
            wpt: RouteElement?,
            sharedPreferences: SharedPreferences
        ): String {
            if (wpt == null){
                return "-----"
            }
            if (wpt.type == RouteElementType.WAYPOINT) {
                return getPointOfCompass(
                    wpt.proofArea.bearings[0],
                    wpt.proofArea.bearings[1]
                )
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

        fun getShortestDistanceToGateData(position: Position, wpt: RouteElement?): String {
            if (wpt==null){
                return "-----"
            }
            return getDistString(pointToLineDist(position.toLocation(), wpt.portWpt, wpt.stbdWpt))
        }

        fun getCOGData(position: Position, sharedPreferences: SharedPreferences): String{
            val magnetic = sharedPreferences.getBoolean(SettingsFragment.KEY_MAGNETIC, false)
            return getDirString(position.course, magnetic, false, position, position.time.time)
        }

        fun getLocationData(position: Position): String {
            return getLatString(position.latitude) + "\n" + getLonString(position.longitude)
        }

        fun getVMGGateData(position: Position, wpt: RouteElement?): String{
            if (wpt==null){
                return "-----"
            }
            return getDistString(pointToLineDist(position.toLocation(), wpt.portWpt, wpt.stbdWpt))
        }

    }
}
