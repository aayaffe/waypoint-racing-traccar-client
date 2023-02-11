/*
 * Copyright 2012 - 2016 Anton Tananaev (anton@traccar.org)
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

import `in`.avimarine.waypointracing.route.GatePassing
import android.net.Uri
import java.util.*

object ProtocolFormatter {

    fun formatRequest(url: String, position: Position, alarm: String? = null): String {
        val serverUrl = Uri.parse(url)
        val builder = serverUrl.buildUpon()
            .appendQueryParameter("id", position.deviceId)
            .appendQueryParameter("timestamp", (position.time.time / 1000).toString())
            .appendQueryParameter("lat", position.latitude.toString())
            .appendQueryParameter("lon", position.longitude.toString())
            .appendQueryParameter("speed", position.speed.toString())
            .appendQueryParameter("bearing", position.course.toString())
            .appendQueryParameter("altitude", position.altitude.toString())
            .appendQueryParameter("accuracy", position.accuracy.toString())
            .appendQueryParameter("batt", position.battery.toString())
            .appendQueryParameter("boatname", position.boatName)
        if (position.charging) {
            builder.appendQueryParameter("charge", position.charging.toString())
        }
        if (position.mock) {
            builder.appendQueryParameter("mock", position.mock.toString())
        }
        if (alarm != null) {
            builder.appendQueryParameter("alarm", alarm)
        }
        return builder.build().toString()
    }

    fun formatRequest(url: String, gatePass: GatePassing): String {
        val serverUrl = Uri.parse(url)
        val builder = serverUrl.buildUpon()
            .appendQueryParameter("id", UUID.randomUUID().toString())
            .appendQueryParameter("eventname", gatePass.eventName)
            .appendQueryParameter("deviceid", gatePass.deviceId)
            .appendQueryParameter("gatename", gatePass.gateName)
            .appendQueryParameter("timestamp", (gatePass.time.time / 1000).toString())
            .appendQueryParameter("lat", gatePass.latitude.toString())
            .appendQueryParameter("lon", gatePass.longitude.toString())
            .appendQueryParameter("speed", gatePass.speed.toString())
            .appendQueryParameter("bearing", gatePass.course.toString())
            .appendQueryParameter("accuracy", gatePass.accuracy.toString())
            .appendQueryParameter("batt", gatePass.battery.toString())
            .appendQueryParameter("boatname", gatePass.boatName)
        if (gatePass.mock) {
            builder.appendQueryParameter("mock", gatePass.mock.toString())
        }
        return builder.build().toString()
    }
}
