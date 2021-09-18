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
package `in`.avimarine.waypointracing;

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
import `in`.avimarine.waypointracing.activities.Main2Activity
import `in`.avimarine.waypointracing.activities.MainFragment
import `in`.avimarine.waypointracing.activities.StatusActivity
import `in`.avimarine.waypointracing.database.DatabaseHelper
import `in`.avimarine.waypointracing.database.GatePassesDatabaseHelper
import `in`.avimarine.waypointracing.route.GatePassing
import `in`.avimarine.waypointracing.route.Route
import `in`.avimarine.waypointracing.route.RouteElement
import `in`.avimarine.waypointracing.route.Waypoint
import android.location.Location
import kotlinx.android.synthetic.main.activity_main2.*

class TrackingController(private val context: Context, private val routeHandler: RouteHandler?) : PositionListener, NetworkHandler {

    private var nextWpt: Int = -1
    private var route: Route? = null
    private val handler = Handler(Looper.getMainLooper())
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val positionProvider = PositionProviderFactory.create(context, this)
    private val databaseHelper = DatabaseHelper(context)
    private val gatePassDatabaseHelper = GatePassesDatabaseHelper(context)
    private val networkManager = NetworkManager(context, this)
    private val deviceId = preferences.getString(MainFragment.KEY_DEVICE, "undefined")
    private val boatName = preferences.getString(MainFragment.KEY_NAME, "boat_undefined")

    private val url: String = preferences.getString(
        MainFragment.KEY_URL,
        context.getString(R.string.settings_url_default_value)
    )!!
    private val buffer: Boolean = preferences.getBoolean(MainFragment.KEY_BUFFER, true)

    private var isOnline = networkManager.isOnline
    private var isWaiting = false
    private var isWaitingGP = true


    interface PositionListener {
        fun onPositionUpdate(position: Position?)
        fun onPositionError(error: Throwable?)
    }

    interface RouteHandler {
        fun onRouteUpdate(nextWpt: Int)
    }


    fun start() {
        if (isOnline) {
            read()
        }
        try {
            positionProvider.startUpdates()
        } catch (e: SecurityException) {
            Log.w(TAG, e)
        }
        networkManager.start()
    }

    fun stop() {
        networkManager.stop()
        try {
            positionProvider.stopUpdates()
        } catch (e: SecurityException) {
            Log.w(TAG, e)
        }
        handler.removeCallbacksAndMessages(null)
    }

    override fun onPositionUpdate(position: Position) {
        StatusActivity.addMessage(context.getString(R.string.status_location_update))
        val inArea = updateIsInArea(position, nextWpt)
        if (buffer) {
            write(position)
            if (inArea && route != null) {
                val gp = GatePassing(route!!.eventName,
                    deviceId!!,
                    boatName!!, route!!.elements?.get(nextWpt)?.name,position.time, position)
                write(gp)
                nextWpt = nextWpt++
                routeHandler?.onRouteUpdate(nextWpt)
            }
        } else {
            send(position)
            if (inArea && route != null) {
                val gp = GatePassing(route!!.eventName,
                    deviceId!!,
                    boatName!!, route!!.elements?.get(nextWpt)?.name,position.time, position)
                send(gp)
                nextWpt = nextWpt++
                routeHandler?.onRouteUpdate(nextWpt)
            }
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
        if (gatePass != null) {
            formattedAction +=
                " (id:" + gatePass.id +
                        " time:" + gatePass.time.time / 1000 +
                        " lat:" + gatePass.latitude +
                        " lon:" + gatePass.longitude + ")"
        } //TODO: Add more logging
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
        gatePassDatabaseHelper.insertGatePassAsync(gatePass, object : GatePassesDatabaseHelper.DatabaseHandler<Unit?> {
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
                        if (result.deviceId == preferences.getString(
                                MainFragment.KEY_DEVICE,
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
        gatePassDatabaseHelper.selectGatePassAsync(object : GatePassesDatabaseHelper.DatabaseHandler<GatePassing?> {
            override fun onComplete(success: Boolean, result: GatePassing?) {
                if (success) {
                    if (result != null) {
                        if (result.deviceId == preferences.getString(
                                MainFragment.KEY_DEVICE,
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
        databaseHelper.deletePositionAsync(gatePass.id, object : DatabaseHandler<Unit?> {
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
        val request = formatRequest(url, gatePass)
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
        handler.postDelayed({ //TODO: Might need to use a different handler!
            if (isOnline) {
                readGatePass()
            }
        }, RETRY_DELAY.toLong())
    }


    private fun updateIsInArea(location: Position, nextWpt: Int) : Boolean {
        val l = Location("")
        l.latitude = location.latitude
        l.longitude = location.longitude
        val wpt = route?.elements?.elementAtOrNull(nextWpt)
        if (wpt != null) {
            if (wpt!!.isInProofArea(l)) {
                if (wpt!!.passedGate(location)) {
                    StatusActivity.addMessage("Passed " + wpt!!.name)
                    if (wpt!!.isInProofArea(l)){
                        return true
                    }
                }
            } else {
                return false
            }
        }
        return false
    }


    fun updateRoute(route: Route?, nextWpt: Int) {
        this.route = route
        this.nextWpt = nextWpt
    }

    companion object {
        private const val RETRY_DELAY = 30 * 1000
    }



}
