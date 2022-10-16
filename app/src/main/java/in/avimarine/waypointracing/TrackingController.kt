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
import `in`.avimarine.waypointracing.ProtocolFormatter.formatRequest
import `in`.avimarine.waypointracing.RequestManager.sendRequestAsync
import `in`.avimarine.waypointracing.PositionProvider.PositionListener
import `in`.avimarine.waypointracing.NetworkManager.NetworkHandler
import android.os.Handler
import android.os.Looper
import androidx.preference.PreferenceManager
import android.util.Log
import `in`.avimarine.waypointracing.database.DatabaseHelper.DatabaseHandler
import `in`.avimarine.waypointracing.RequestManager.RequestHandler
import `in`.avimarine.waypointracing.activities.SettingsFragment
import `in`.avimarine.waypointracing.activities.StatusActivity
import `in`.avimarine.waypointracing.database.DatabaseHelper
import `in`.avimarine.waypointracing.database.FirestoreDatabase
import `in`.avimarine.waypointracing.database.GatePassesDatabaseHelper
import `in`.avimarine.waypointracing.route.*
import `in`.avimarine.waypointracing.utils.pointToLineDist
import `in`.avimarine.waypointracing.utils.toLocation
import `in`.avimarine.waypointracing.utils.toNM
import android.content.SharedPreferences
import android.widget.Toast
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import java.util.*

class TrackingController(private val context: Context) :
    PositionListener, NetworkHandler, SharedPreferences.OnSharedPreferenceChangeListener {


    private var route: Route? = null
    private val handler = Handler(Looper.getMainLooper())
    private val handlerGP = Handler(Looper.getMainLooper())
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private var nextWpt = sharedPreferences.getInt(SettingsFragment.KEY_NEXT_WPT, -1)
    private val positionProvider = PositionProviderFactory.create(context, this)
    private val databaseHelper = DatabaseHelper(context)
    private val gatePassDatabaseHelper = GatePassesDatabaseHelper(context)
    private val networkManager = NetworkManager(context, this)
    private val deviceId = sharedPreferences.getString(SettingsFragment.KEY_DEVICE, "undefined")
    private val boatName = sharedPreferences.getString(SettingsFragment.KEY_BOAT_NAME, "boat_undefined")
    private lateinit var firebaseAnalytics: FirebaseAnalytics



    private val url: String = sharedPreferences.getString(
        SettingsFragment.KEY_URL,
        context.getString(R.string.settings_url_default_value)
    )!!

    private val urlGates: String = sharedPreferences.getString(
        SettingsFragment.KEY_URL_GATES,
        context.getString(R.string.settings_url_gates_default_value)
    )!!
    private val buffer: Boolean = sharedPreferences.getBoolean(SettingsFragment.KEY_BUFFER, true)

    private var isOnline = networkManager.isOnline
    private var isWaiting = false
    private var isWaitingGP = true


    fun start() {
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        firebaseAnalytics = Firebase.analytics
        nextWpt = sharedPreferences.getInt(SettingsFragment.KEY_NEXT_WPT, 0)
        if (isOnline) {
            read()
            readGatePass()
        }
        try {
            positionProvider.startUpdates()
        } catch (e: SecurityException) {
            Log.w(TAG, e)
        }
        networkManager.start()
    }

    fun stop() {
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

    override fun onPositionUpdate(position: Position) {
        val inArea = updateIsInArea(position, nextWpt)
        if (sharedPreferences.getBoolean(SettingsFragment.KEY_STATUS, false) && sharedPreferences.getBoolean(SettingsFragment.KEY_TRACKING, false)) {
            sendPosition(position)
        }
        if (inArea && route != null) {
            if ((GatePassings.getLastGatePass(context)?.gateId
                    ?: "") == route!!.elements[nextWpt].id &&
                (GatePassings.getLastGatePass(context)?.routeId ?: "") == route!!.id
            ) {
                return
            }
            Log.d(TAG, "inArea ${route!!.elements[nextWpt].name}")
            StatusActivity.addMessage("Passed " + route!!.elements[nextWpt].name)
            val gp = GatePassing(
                route!!.eventName, route!!.id, route!!.lastUpdate,
                deviceId!!,
                boatName!!, route!!.elements[nextWpt].id, route!!.elements[nextWpt].name, position.time, position
            )
            if (buffer) {
                write(gp)
            } else {
                send(gp)
            }
            firebaseAnalytics.logEvent("gate_pass") {
                param("route", route!!.eventName)
                param("next_wpt", nextWpt.toString())
            }
            FirestoreDatabase.addGatePass(gp, { documentReference ->
                Log.d(TAG, "Gatepass added with ID: ${documentReference.id}")
                Toast.makeText(context, "Uploaded successfully", Toast.LENGTH_LONG).show()
            },{ e ->
                Log.e(TAG, "Error adding gatepass", e)
                Toast.makeText(context, "--FAILED-- to upload", Toast.LENGTH_LONG).show()
            }
            )
            GatePassings.addGatePass(context, gp)
            if (route!!.eventType == EventType.WPTRACING) { //Enable auto waypoint advance for waypoint racing event only
                nextWpt += 1
                setNextWpt(nextWpt)
            }
        }
        setNewGPSInterval(position, route, nextWpt)
    }



    private fun setNewGPSInterval(position: Position, route: Route?, nextWpt: Int) {
        val uiVisible = sharedPreferences.getBoolean(SettingsFragment.KEY_IS_UI_VISIBLE,true)
        if (route!=null && !uiVisible){
            val wpt = route.elements.elementAtOrNull(nextWpt)
            val lastGatePass = GatePassings.getLastGatePass(context)

            if (wpt != null){
                if (lastGatePass!= null && lastGatePass.routeId == route.id && lastGatePass.gateId == wpt.id){
                    setGPSInterval(MAX_INTERVAL)

                } else {
                    val interval = distance2interval(wpt, position)
                    setGPSInterval(interval)
                }
            }
        }
    }

    private fun distance2interval(wpt: RouteElement, position: Position): Int {
        val dist = toNM(pointToLineDist(position.toLocation(), wpt.portWpt, wpt.stbdWpt))
        val ttg = (dist/position.speed) * 3600 //Conversion to seconds
        return when {
            ttg > 120 -> MAX_INTERVAL
            ttg > 80 -> 40
            ttg > 40 -> 20
            ttg > 20 -> 10
            ttg > 10 -> 5
            else -> 1
        }
    }

    private fun setGPSInterval(i: Int){
        val sharedPref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        with(sharedPref.edit()) {
            putString(SettingsFragment.KEY_INTERVAL, i.toString())
            commit()
        }
        Log.d(TAG, "New interval is $i")
    }

    private fun setNextWpt(n: Int) {
        val sharedPref: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        with(sharedPref.edit()) {
            putInt(SettingsFragment.KEY_NEXT_WPT, n)
            commit()
        }
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

//
// State transition examples:
//
// write -> read -> send -> delete -> read
//
// read -> send -> retry -> read -> send
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

    private fun log(action: String, gatePass: GatePassing) {
        var formattedAction: String = action
        formattedAction +=
            " (id:" + gatePass.id +
                    " time:" + gatePass.time.time / 1000 +
                    " lat:" + gatePass.latitude +
                    " lon:" + gatePass.longitude + ")" //TODO: Add more logging
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

    private fun write(gatePass: GatePassing) {
        log("writeGatePass", gatePass)
        gatePassDatabaseHelper.insertGatePassAsync(
            gatePass,
            object : GatePassesDatabaseHelper.DatabaseHandler<Unit?> {
                override fun onComplete(success: Boolean, result: Unit?) {
                    if (success) {
                        if (isOnline && isWaitingGP) {
                            readGatePass()
                            isWaitingGP = false
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

    private fun readGatePass() {
        log("readGatePass", null)
        gatePassDatabaseHelper.selectGatePassAsync(object :
            GatePassesDatabaseHelper.DatabaseHandler<GatePassing?> {
            override fun onComplete(success: Boolean, result: GatePassing?) {
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
                        isWaitingGP = true
                    }
                } else {
                    retryGatePass()
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

    private fun delete(gatePass: GatePassing) {
        log("deleteGatePass", gatePass)
        gatePassDatabaseHelper.deleteGatePassAsync(
            gatePass.id,
            object : GatePassesDatabaseHelper.DatabaseHandler<Unit?> {
                override fun onComplete(success: Boolean, result: Unit?) {
                    if (success) {
                        readGatePass()
                    } else {
                        retryGatePass()
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
                    with(sharedPreferences.edit()) {
                        putLong(SettingsFragment.KEY_LAST_SEND, Date().time)
                        commit()
                    }
                } else {
                    StatusActivity.addMessage(context.getString(R.string.status_send_fail))
                    if (buffer) {
                        retry()
                    }
                }
            }
        })
    }

    private fun send(gatePass: GatePassing) {
        log("sendGatePass", gatePass)
        val request = formatRequest(urlGates, gatePass)
        sendRequestAsync(request, object : RequestHandler {
            override fun onComplete(success: Boolean) {
                if (success) {
                    if (buffer) {
                        delete(gatePass)
                    }
                } else {
                    StatusActivity.addMessage(context.getString(R.string.status_send_fail))
                    if (buffer) {
                        retryGatePass()
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

    private fun retryGatePass() {
        log("retryGatePass", null)
        handlerGP.postDelayed({ //TODO: Might need to use a different handler!
            if (isOnline) {
                readGatePass()
            }
        }, RETRY_DELAY.toLong())
    }


    private fun updateIsInArea(location: Position, nextWpt: Int): Boolean {
        val l = location.toLocation()
        val wpt = route?.elements?.elementAtOrNull(nextWpt)
        if (wpt != null) {
            return wpt.isInProofArea(l)
        }
        return false
    }


    fun updateRoute(route: Route?) {
        this.route = route
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == SettingsFragment.KEY_NEXT_WPT) {
            nextWpt = sharedPreferences.getInt(SettingsFragment.KEY_NEXT_WPT, 0)
        }
    }

    companion object {
        private const val RETRY_DELAY = 30 * 1000
        private const val MAX_INTERVAL = 60
    }


}
