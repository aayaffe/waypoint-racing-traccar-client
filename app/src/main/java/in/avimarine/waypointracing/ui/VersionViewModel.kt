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

class VersionViewModel(
    private val appVersion: Long
) : ViewModel() {
    fun getAppVersion(): String{
        return "v: $appVersion"
    }

}