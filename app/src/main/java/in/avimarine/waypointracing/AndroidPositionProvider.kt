/*
 * Copyright 2019 - 2021 Anton Tananaev (anton@traccar.org)
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

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import `in`.avimarine.androidutils.TAG
import `in`.avimarine.waypointracing.activities.SettingsFragment
import java.util.*

class AndroidPositionProvider(context: Context, listener: PositionListener) :
    PositionProvider(context, listener), LocationListener {

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val provider = getProvider(sharedPreferences.getString(SettingsFragment.KEY_ACCURACY, "high"))
    private val recoveryHandler = Handler(Looper.getMainLooper())
    private var updatesRequested = false
    private var lastRequestElapsedMs = 0L
    private val recoveryCheck = object : Runnable {
        override fun run() {
            if (!updatesRequested) return
            val now = SystemClock.elapsedRealtime()
            val providerEnabled = try {
                locationManager.isProviderEnabled(provider)
            } catch (e: RuntimeException) {
                listener.onPositionError(e)
                false
            }
            if (providerEnabled &&
                (!isUpdatesActive || LocationRecoveryPolicy.shouldRestart(
                    now, lastRequestElapsedMs, lastRawFixElapsedMs, lastAcceptedFixElapsedMs,
                    requestedIntervalMs, distance == 0.0 && angle == 0.0
                ))
            ) {
                Log.w(TAG, "Location updates stalled; restarting $provider")
                listener.onPositionError(IllegalStateException("Location updates stalled; restarting $provider"))
                stopUpdates()
                resetLocationFilter()
                startUpdates()
            } else {
                recoveryHandler.postDelayed(this, RECOVERY_CHECK_INTERVAL_MS)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun startUpdates() {
        updatesRequested = true
        if (isUpdatesActive) return
        lastRequestElapsedMs = SystemClock.elapsedRealtime()
        try {
            locationManager.requestLocationUpdates(
                provider, requestedIntervalMs, 0f, this,
                Looper.getMainLooper()
            )
            markUpdatesStarted()
        } catch (e: RuntimeException) {
            listener.onPositionError(e)
        }
        recoveryHandler.removeCallbacks(recoveryCheck)
        recoveryHandler.postDelayed(recoveryCheck, RECOVERY_CHECK_INTERVAL_MS)
    }

    override fun stopUpdates() {
        updatesRequested = false
        recoveryHandler.removeCallbacks(recoveryCheck)
        if (!isUpdatesActive) return
        markUpdatesStopped()
        try {
            locationManager.removeUpdates(this)
        } catch (e: RuntimeException) {
            listener.onPositionError(e)
        }
    }

    @Suppress("DEPRECATION", "MissingPermission")
    override fun requestSingleLocation() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        try {
            val location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            if (location != null) {
                listener.onPositionUpdate(Position(deviceId, userId, boatName, location, getBatteryStatus(context)), location)
            } else {
                locationManager.requestSingleUpdate(provider, object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        listener.onPositionUpdate(Position(deviceId, userId, boatName, location, getBatteryStatus(context)), location)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }, Looper.getMainLooper())
            }
        } catch (e: RuntimeException) {
            listener.onPositionError(e)
        }
    }

    override fun onLocationChanged(location: Location) {
        //GPS Week rollover fix. Add 1024 weeks if OS time is off by more than ~992 weeks
        //https://stackoverflow.com/questions/56147606/
        val c = Calendar.getInstance()
        if (c.time.time - location.time > 600000000000L) {
            location.time = location.time + 619315200000L
        }
        processLocation(location)
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
    override fun onProviderEnabled(provider: String) {
        if (provider == this.provider && updatesRequested) {
            stopUpdates()
            resetLocationFilter()
            startUpdates()
        }
    }
    override fun onProviderDisabled(provider: String) {
        if (provider == this.provider) {
            listener.onPositionError(IllegalStateException("Location provider disabled: $provider"))
        }
    }

    private fun getProvider(accuracy: String?): String {
        return when (accuracy) {
            "high" -> LocationManager.GPS_PROVIDER
            "low" -> LocationManager.PASSIVE_PROVIDER
            else -> LocationManager.NETWORK_PROVIDER
        }
    }

    private val requestedIntervalMs: Long
        get() = if (distance > 0 || angle > 0) MINIMUM_INTERVAL else interval

    companion object {
        private const val RECOVERY_CHECK_INTERVAL_MS = 30_000L
    }

}
