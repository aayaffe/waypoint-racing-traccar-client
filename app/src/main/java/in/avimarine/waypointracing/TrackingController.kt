/*
 * Copyright 2015 - 2019 Anton Tananaev (anton@traccar.org)
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

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Firebase
import `in`.avimarine.androidutils.TAG
import `in`.avimarine.androidutils.Utils.Companion.getInstalledVersion
import `in`.avimarine.androidutils.pointToLineDist
import `in`.avimarine.androidutils.units.DistanceUnits
import `in`.avimarine.waypointracing.NetworkManager.NetworkHandler
import `in`.avimarine.waypointracing.PositionProvider.PositionListener
import `in`.avimarine.waypointracing.ProtocolFormatter.formatRequest
import `in`.avimarine.waypointracing.RequestManager.RequestHandler
import `in`.avimarine.waypointracing.RequestManager.sendRequestAsync
import `in`.avimarine.waypointracing.activities.SettingsFragment
import `in`.avimarine.waypointracing.activities.StatusActivity
import `in`.avimarine.waypointracing.database.DatabaseHelper
import `in`.avimarine.waypointracing.database.DatabaseHelper.DatabaseHandler
import `in`.avimarine.waypointracing.database.FirestoreDatabase
import `in`.avimarine.waypointracing.route.EventType
import `in`.avimarine.waypointracing.route.GatePassing
import `in`.avimarine.waypointracing.route.GatePassings
import `in`.avimarine.waypointracing.route.Route
import `in`.avimarine.waypointracing.route.RouteElement
import `in`.avimarine.waypointracing.utils.Preferences
import `in`.avimarine.waypointracing.utils.RemoteConfig
import `in`.avimarine.waypointracing.utils.RouteParser
import java.util.Date
import java.util.concurrent.atomic.AtomicLong

class TrackingController(private val context: Context) :
    PositionListener, NetworkHandler, SharedPreferences.OnSharedPreferenceChangeListener {


    private var route: Route? = null
    private val handler = Handler(Looper.getMainLooper())
    private val handlerGP = Handler(Looper.getMainLooper())
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val prefs = Preferences(sharedPreferences)
    private val positionProvider = PositionProviderFactory.create(context, this)
    private val databaseHelper = DatabaseHelper(context)
    private val networkManager = NetworkManager(context, this)
    private val deviceId = sharedPreferences.getString(SettingsFragment.KEY_DEVICE, "undefined")
    private val boatName =
        sharedPreferences.getString(SettingsFragment.KEY_BOAT_NAME, "boat_undefined")
    private lateinit var firebaseAnalytics: FirebaseAnalytics


    private val url: String = sharedPreferences.getString(
        SettingsFragment.KEY_URL,
        context.getString(R.string.settings_url_default_value)
    )!!

    private val buffer: Boolean = sharedPreferences.getBoolean(SettingsFragment.KEY_BUFFER, true)

    private var isOnline = networkManager.isOnline
    private var isWaiting = false

    private var lastPositionTime = AtomicLong(0L)



    fun start() {
        Log.d(TAG, "TrackingController started")
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        firebaseAnalytics = Firebase.analytics
        if (isOnline) {
            read()
        }
        try {
            positionProvider.startUpdates()
        } catch (e: SecurityException) {
            Log.w(TAG, e)
        }
        networkManager.start()
        route = RouteParser.parseRoute(prefs.currentRoute)
        val name = route?.eventName?: ""
        val lastUpdate = route?.lastUpdate?.time?.toString() ?: ""
        FirestoreDatabase.addEvent(`in`.avimarine.waypointracing.database.EventType.TRACKING_START, "$name $lastUpdate")

    }

    fun stop() {
        route = RouteParser.parseRoute(prefs.currentRoute)
        val name = route?.eventName?: ""
        val lastUpdate = route?.lastUpdate?.time?.toString() ?: ""
        FirestoreDatabase.addEvent(`in`.avimarine.waypointracing.database.EventType.TRACKING_STOP, "$name $lastUpdate")
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        networkManager.stop()
        try {
            positionProvider.stopUpdates()
        } catch (e: SecurityException) {
            Log.w(TAG, e)
        }
        handler.removeCallbacksAndMessages(null)
        handlerGP.removeCallbacksAndMessages(null)
    }

    private fun sendPosition(position: Position) {
        if (buffer) {
            write(position)
        } else {
            send(position)
        }
    }

    override fun onPositionUpdate(position: Position, location: Location) {
        Log.d(TAG, "onPositionUpdate")
        if (route == null) {
            route = RouteParser.parseRoute(prefs.currentRoute)
        }
        val inArea = updateIsInArea(location, prefs.nextWpt)
        if (prefs.status && prefs.tracking) {
            sendPosition(position)
        }
        //Upload position to Firestore
        if (RemoteConfig.getBool("save_all_locations")) {
            val minPositionUploadInterval = RemoteConfig.getLong("min_position_upload_interval") * 1000 //Convert to ms
            if (position.time.time - lastPositionTime.get() > minPositionUploadInterval) {
                lastPositionTime.set(position.time.time)
                FirestoreDatabase.addPosition(position, { documentReference ->
                    Log.d(TAG, "Position added with ID: ${documentReference.id}")
                }, { e ->
                    Log.e(TAG, "Error adding position", e)
                })
            }
        }
        if (inArea && route != null) {
            if ((GatePassings.getLastGatePass(context, route!!.id)?.gateId
                    ?: "") == route!!.elements[prefs.nextWpt].id &&
                (GatePassings.getLastGatePass(context, route!!.id)?.routeId ?: "") == route!!.id
            ) {
                // Check if The last recorded gatepass is the same as the current one
                // then do nothing
                return
            }
            Log.d(TAG, "inArea ${route!!.elements[prefs.nextWpt].name}")
            StatusActivity.addMessage("Passed " + route!!.elements[prefs.nextWpt].name)
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val gp = GatePassing(
                route!!.eventName,
                route!!.id,
                route!!.lastUpdate,
                deviceId!!,
                userId,
                boatName!!,
                route!!.elements[prefs.nextWpt].id,
                route!!.elements[prefs.nextWpt].name,
                position.time,
                position,
                getInstalledVersion(context)
            )
            firebaseAnalytics.logEvent("gate_pass") {
                param("route", route!!.eventName)
                param("next_wpt", prefs.nextWpt.toString())
            }
            FirestoreDatabase.addGatePass(gp, { documentReference ->
                Log.d(TAG, "Gatepass added with ID: ${documentReference.id}")
                Toast.makeText(context, "Uploaded successfully", Toast.LENGTH_LONG).show()
            }, { e ->
                Log.e(TAG, "Error adding gatepass", e)
                Toast.makeText(context, "--FAILED-- to upload", Toast.LENGTH_LONG).show()
            }
            )
            GatePassings.addGatePass(context, gp)
            if (route!!.eventType == EventType.WPTRACING) { //Enable auto waypoint advance for waypoint racing event only
                prefs.nextWpt = route!!.getNextNonOptionalWpt(prefs.nextWpt)
            }
        }
        setNewGPSInterval(location, route, prefs.nextWpt)
    }


    private fun setNewGPSInterval(location: Location, route: Route?, nextWpt: Int) {
        val uiVisible = sharedPreferences.getBoolean(SettingsFragment.KEY_IS_UI_VISIBLE, true)
        if (route != null && !uiVisible) {
            val wpt = route.elements.elementAtOrNull(nextWpt)
            val lastGatePass = GatePassings.getLastGatePass(context, route.id)

            if (wpt != null) {
                if ((lastGatePass != null) && (lastGatePass.routeId == route.id) && (lastGatePass.gateId == wpt.id)) {
                    setGPSInterval(MAX_INTERVAL)
                } else {
                    if (sharedPreferences.getBoolean(
                            SettingsFragment.KEY_ADAPTIVE_INTERVAL,
                            true
                        )
                    ) {
                        val interval = distance2interval(wpt, location)
                        setGPSInterval(interval)
                    } else {
                        setGPSInterval(prefs.initialGPSInterval.toInt())
                    }
                }
            }
        }
    }

    private fun distance2interval(wpt: RouteElement, location: Location): Int {
        val dist = pointToLineDist(
            location,
            wpt.portWpt,
            wpt.stbdWpt
        ).getValue(DistanceUnits.NauticalMiles)
        val ttg = (dist / location.speed) * 3600 //Conversion to seconds
        return when {
            ttg > 120 -> MAX_INTERVAL
            ttg > 80 -> 40
            ttg > 40 -> 20
            ttg > 20 -> 10
            ttg > 10 -> 5
            else -> 1
        }
    }

    private fun setGPSInterval(i: Int) {
        prefs.GPSInterval = i.toString()
        Log.d(TAG, "New interval is $i")
    }

    override fun onPositionError(error: Throwable) {}
    override fun onNetworkUpdate(isOnline: Boolean) {
        val message =
            if (isOnline) R.string.status_network_online else R.string.status_network_offline
        StatusActivity.addMessage(context.getString(message))
        if (!this.isOnline && isOnline) {
            read()
        }
        this.isOnline = isOnline
    }

    ////
//// State transition examples:
////
//// write -> read -> send -> delete -> read
////
//// read -> send -> retry -> read -> send
////
//
    private fun log(action: String, position: Position?) {
        var formattedAction: String = action
        if (position != null) {
            formattedAction +=
                " (id:" + position.id +
                        " time:" + position.time.time / 1000 +
                        " lat:" + position.latitude +
                        " lon:" + position.longitude + ")"
        }
        Log.d(TAG, formattedAction)
    }

    private fun write(position: Position) {
        log("write", position)
        databaseHelper.insertPositionAsync(position, object : DatabaseHandler<Unit?> {
            override fun onComplete(success: Boolean, result: Unit?) {
                if (success) {
                    if (isOnline && isWaiting) {
                        read()
                        isWaiting = false
                    }
                }
            }
        })
    }

    private fun read() {
        log("read", null)
        databaseHelper.selectPositionAsync(object : DatabaseHandler<Position?> {
            override fun onComplete(success: Boolean, result: Position?) {
                if (success) {
                    if (result != null) {
                        if (result.deviceId == sharedPreferences.getString(
                                SettingsFragment.KEY_DEVICE,
                                null
                            )
                        ) {
                            send(result)
                        } else {
                            delete(result)
                        }
                    } else {
                        isWaiting = true
                    }
                } else {
                    retry()
                }
            }
        })
    }

    private fun delete(position: Position) {
        log("delete", position)
        databaseHelper.deletePositionAsync(position.id, object : DatabaseHandler<Unit?> {
            override fun onComplete(success: Boolean, result: Unit?) {
                if (success) {
                    read()
                } else {
                    retry()
                }
            }
        })
    }

    private fun send(position: Position) {
        log("send", position)
        val request = formatRequest(url, position)
        sendRequestAsync(request, object : RequestHandler {
            override fun onComplete(success: Boolean) {
                if (success) {
                    if (buffer) {
                        delete(position)
                    }
                    prefs.lastSend = Date().time
                } else {
                    StatusActivity.addMessage(context.getString(R.string.status_send_fail))
                    if (buffer) {
                        retry()
                    }
                }
            }
        })
    }

    private fun retry() {
        log("retry", null)
        handler.postDelayed({
            if (isOnline) {
                read()
            }
        }, RETRY_DELAY.toLong())
    }

    private fun updateIsInArea(location: Location, nextWpt: Int): Boolean {
        val l = location
        val wpt = route?.elements?.elementAtOrNull(nextWpt)
        if (wpt != null) {
            return wpt.isInProofArea(l)
        }
        return false
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == SettingsFragment.KEY_NEXT_WPT) {
            setGPSInterval(1)
        }
        if (key == SettingsFragment.KEY_ROUTE) {
            route = RouteParser.parseRoute(prefs.currentRoute)
        }
    }

    companion object {
        private const val RETRY_DELAY = 30 * 1000
        private const val MAX_INTERVAL = 60
    }
}
