/*
 * Copyright 2013 - 2021 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package `in`.avimarine.waypointracing

import `in`.avimarine.waypointracing.activities.SettingsFragment
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.location.Location
import android.os.BatteryManager
import androidx.preference.PreferenceManager
import android.util.Log
import kotlin.math.abs

abstract class PositionProvider(
        protected val context: Context,
        protected val listener: PositionListener,
        ) : SharedPreferences.OnSharedPreferenceChangeListener {


    interface PositionListener {
        fun onPositionUpdate(position: Position)
        fun onPositionError(error: Throwable)
    }

    protected var sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    init{
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }
    protected var deviceId = sharedPreferences.getString(SettingsFragment.KEY_DEVICE, "undefined")!!
    protected var boatName = sharedPreferences.getString(SettingsFragment.KEY_BOAT_NAME, "boat_undefined")!!
    protected var interval = sharedPreferences.getString(SettingsFragment.KEY_INTERVAL, "600")!!.toLong() * 1000
    protected var distance: Double = sharedPreferences.getString(SettingsFragment.KEY_DISTANCE, "0")!!.toInt().toDouble()
    protected var angle: Double = sharedPreferences.getString(SettingsFragment.KEY_ANGLE, "0")!!.toInt().toDouble()
    private var lastLocation: Location? = null

    abstract fun startUpdates()
    abstract fun stopUpdates()
    abstract fun requestSingleLocation()

    protected fun processLocation(location: Location?) {
        if (location != null &&
                (lastLocation == null || location.time - lastLocation!!.time >= 0.5 * interval || distance > 0
                && location.distanceTo(lastLocation) >= distance || angle > 0
                && abs(location.bearing - lastLocation!!.bearing) >= angle)
        ) {
            Log.v(TAG, "location new")
            lastLocation = location
            listener.onPositionUpdate(Position(deviceId, boatName, location, getBatteryLevel(context)))
        } else {
            Log.i(TAG, if (location != null) "location ignored" else "location nil")
        }
    }

    protected fun getBatteryLevel(context: Context): Double {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (batteryIntent != null) {
            val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
            val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 1)
            return level * 100.0 / scale
        }
        return 0.0
    }
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == SettingsFragment.KEY_INTERVAL) {
            interval = sharedPreferences.getString(SettingsFragment.KEY_INTERVAL, "600")!!.toLong() * 1000
            stopUpdates()
            startUpdates()
        }
    }
    companion object {
        const val MINIMUM_INTERVAL: Long = 1000
    }

}