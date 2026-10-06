/*
 * Copyright 2013 - 2022 Anton Tananaev (anton@traccar.org)
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
import `in`.avimarine.waypointracing.utils.Preferences
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.location.Location
import android.os.BatteryManager
import android.os.SystemClock
import androidx.preference.PreferenceManager
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import `in`.avimarine.androidutils.BatteryStatus
import `in`.avimarine.androidutils.TAG
import kotlin.math.abs

abstract class PositionProvider(
        protected val context: Context,
        protected val listener: PositionListener,
        ) : SharedPreferences.OnSharedPreferenceChangeListener {


    interface PositionListener {
        fun onPositionUpdate(position: Position, location: Location)
        fun onPositionError(error: Throwable)
    }

    protected var sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    protected val deviceId: String get() = Preferences(sharedPreferences).deviceId
    protected val boatName: String get() = sharedPreferences.getString(SettingsFragment.KEY_BOAT_NAME, "boat_undefined")!!
    protected var interval = sharedPreferences.getString(SettingsFragment.KEY_INTERVAL, "600")!!.toLong() * 1000
    protected var distance: Double = sharedPreferences.getString(SettingsFragment.KEY_DISTANCE, "0")!!.toInt().toDouble()
    protected var angle: Double = sharedPreferences.getString(SettingsFragment.KEY_ANGLE, "0")!!.toInt().toDouble()
    private var lastLocation: Location? = null
    private val userId: String get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private var updatesActive = false
    var lastRawFixElapsedMs: Long = 0L
        private set
    var lastAcceptedFixElapsedMs: Long = 0L
        private set

    protected fun markUpdatesStarted() {
        if (!updatesActive) {
            updatesActive = true
            sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }
    }

    protected fun markUpdatesStopped() {
        if (updatesActive) {
            updatesActive = false
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        }
    }

    protected val isUpdatesActive: Boolean get() = updatesActive

    protected fun resetLocationFilter() {
        lastLocation = null
    }

    abstract fun startUpdates()
    abstract fun stopUpdates()
    abstract fun requestSingleLocation()
    open fun requestFreshLocation() = Unit

    protected fun processLocation(location: Location?) {
        if (location != null) lastRawFixElapsedMs = SystemClock.elapsedRealtime()
        val lastLocation = this.lastLocation
        if (location != null &&
                (lastLocation == null || location.time - lastLocation!!.time >= 0.5 * interval || distance > 0
                && location.distanceTo(lastLocation) >= distance || angle > 0
                && abs(location.bearing - lastLocation.bearing) >= angle)
        ) {
            Log.v(TAG, "location new")
            this.lastLocation = location
            lastAcceptedFixElapsedMs = SystemClock.elapsedRealtime()
            listener.onPositionUpdate(Position(deviceId, userId, boatName, location, getBatteryStatus(context)), location)

        } else {
            Log.v(TAG, if (location != null) "location ignored" else "location nil")
        }
    }

    protected fun getBatteryStatus(context: Context): BatteryStatus {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (batteryIntent != null) {
            val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
            val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 1)
            val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            return BatteryStatus(
                level = level * 100.0 / scale,
                charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
            )
        }
        return BatteryStatus()
    }
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == SettingsFragment.KEY_INTERVAL) {
            val newInterval = sharedPreferences?.getString(SettingsFragment.KEY_INTERVAL, "600")?.toLongOrNull()?.times(1000)
            if (newInterval != null && newInterval > 0 && newInterval != interval) {
                interval = newInterval
                if (isUpdatesActive) {
                    stopUpdates()
                    startUpdates()
                }
            }
        }
    }
    companion object {
        const val MINIMUM_INTERVAL: Long = 1000
    }

}
