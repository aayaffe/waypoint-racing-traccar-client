/*
 * Copyright 2016 - 2021 Anton Tananaev (anton@traccar.org)
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

import androidx.multidex.MultiDexApplication
import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Notification
import android.graphics.Color
import android.os.Build
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import `in`.avimarine.waypointracing.utils.Preferences
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.FirebaseApp

open class MainApplication : MultiDexApplication() {

    private var startedActivities = 0

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit

            override fun onActivityStarted(activity: Activity) {
                val openingApp = startedActivities == 0
                startedActivities++
                isAppVisible = true
                if (openingApp) {
                    val prefs = Preferences(PreferenceManager.getDefaultSharedPreferences(this@MainApplication))
                    if (prefs.status) {
                        if (TrackingService.refreshNotificationIfRunning() == null) {
                            try {
                                ContextCompat.startForegroundService(
                                    this@MainApplication,
                                    Intent(this@MainApplication, TrackingService::class.java)
                                )
                            } catch (e: RuntimeException) {
                                Log.w("MainApplication", "Unable to start tracking service", e)
                            }
                        }
                    }
                }
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivities = (startedActivities - 1).coerceAtLeast(0)
                if (startedActivities == 0) {
                    val prefs = Preferences(PreferenceManager.getDefaultSharedPreferences(this@MainApplication))
                    if (prefs.status) {
                        val restored = TrackingService.refreshNotificationIfRunning()
                        isAppVisible = false
                        if (restored == false ||
                            (restored == null && !TrackingService.canShowTrackingNotification(this@MainApplication))) {
                            prefs.status = false
                            stopService(Intent(this@MainApplication, TrackingService::class.java))
                        }
                    } else {
                        isAppVisible = false
                    }
                }
            }
        })
        FirebaseApp.initializeApp(this)?.let {
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
        System.setProperty("http.keepAliveDuration", (30 * 60 * 1000).toString())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerChannel()
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun registerChannel() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val previousChannelBlocked = manager.getNotificationChannel("tracking_active_v2")?.importance ==
            NotificationManager.IMPORTANCE_NONE
        val channel = NotificationChannel(
            PRIMARY_CHANNEL,
            getString(R.string.channel_tracking),
            if (previousChannelBlocked) NotificationManager.IMPORTANCE_NONE else NotificationManager.IMPORTANCE_LOW
        )
        channel.lightColor = Color.GREEN
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        channel.setSound(null, null)
        channel.enableVibration(false)
        manager.createNotificationChannel(channel)
        val passChannel = NotificationChannel(
            PASS_CHANNEL, getString(R.string.channel_gate_passes), NotificationManager.IMPORTANCE_DEFAULT
        )
        passChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        manager.createNotificationChannel(passChannel)
    }

    open fun handleRatingFlow(activity: Activity) {}

    companion object {
        @Volatile
        var isAppVisible = false
            private set
        // Existing channel sound settings are immutable, so tracking uses a new silent channel.
        const val PRIMARY_CHANNEL = "tracking_active_v3_silent"
        // Keep the celebration card visible instead of covering it with a heads-up alert.
        const val PASS_CHANNEL = "gate_passes_v2"
    }

}
