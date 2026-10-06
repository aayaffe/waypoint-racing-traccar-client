/*
 * Copyright 2012 - 2020 Anton Tananaev (anton@traccar.org)
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

import `in`.avimarine.waypointracing.activities.MainActivity
import `in`.avimarine.waypointracing.activities.StatusActivity
import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import `in`.avimarine.androidutils.TAG
import `in`.avimarine.waypointracing.utils.Preferences


class TrackingService : Service() {

    private var wakeLock: WakeLock? = null
    private var trackingController: TrackingController? = null
    private lateinit var prefs: Preferences
    private var screenOffReceiverRegistered = false
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) trackingController?.onScreenOff()
        }
    }


    @SuppressLint("WakelockTimeout")
    override fun onCreate() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        prefs = Preferences(sharedPreferences)
        if (!prefs.status) {
            stopSelf()
            return
        }
        if (!MainApplication.isAppVisible && !canShowTrackingNotification(this)) {
            stopTrackingDueToError()
            return
        }
        try {
            showTrackingNotification()
            activeService = this
            Log.i(TAG, "service create")
            sendBroadcast(Intent(ACTION_STARTED).setPackage(packageName))
            StatusActivity.addMessage(getString(R.string.status_service_create))

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                if (prefs.wakeLock) {
                    try {
                        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
                        wakeLock =
                            powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, javaClass.name)
                        wakeLock?.acquire()
                    } catch (e: RuntimeException) {
                        Log.w(TAG, "Unable to acquire tracking wake lock", e)
                        prefs.wakeLock = false
                    }
                }
                Log.i(TAG, "Tracking wake lock held: ${wakeLock?.isHeld == true}")
                val powerManager = getSystemService(POWER_SERVICE) as PowerManager
                Log.i(TAG, "Battery optimization ignored: ${powerManager.isIgnoringBatteryOptimizations(packageName)}")
                trackingController = TrackingController(this)
                trackingController?.start()
                try {
                    registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
                    screenOffReceiverRegistered = true
                } catch (e: RuntimeException) {
                    Log.w(TAG, "Unable to register screen-off recovery receiver", e)
                }
            } else {
                Log.w(TAG, "Tracking stopped: precise location permission is missing")
                StatusActivity.addMessage("Tracking stopped: precise location permission is missing")
                stopTrackingDueToError()
            }
        } catch (e: RuntimeException) {
            Log.w(TAG, e)
            StatusActivity.addMessage("Tracking could not start: ${e.message ?: e.javaClass.simpleName}")
            stopTrackingDueToError()
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        WakefulBroadcastReceiver.completeWakefulIntent(intent)
        if (!prefs.status) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (canShowTrackingNotification(this)) {
            try {
                showTrackingNotification()
            } catch (e: RuntimeException) {
                Log.w(TAG, "Unable to restore tracking notification", e)
                stopTrackingDueToError()
                return START_NOT_STICKY
            }
        }
        if (!canTrackNow(this)) {
            stopTrackingDueToError()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun stopTrackingDueToError() {
        prefs.status = false
        stopSelf()
    }

    private fun showTrackingNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, createNotification(this), FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, createNotification(this))
        }
    }

    override fun onDestroy() {
        if (screenOffReceiverRegistered) {
            unregisterReceiver(screenOffReceiver)
            screenOffReceiverRegistered = false
        }
        if (activeService === this) activeService = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        Log.i(TAG, "service destroy")
        sendBroadcast(Intent(ACTION_STOPPED).setPackage(packageName))
        StatusActivity.addMessage(getString(R.string.status_service_destroy))
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        trackingController?.stop()
    }

    companion object {

        // Explicit package name should be specified when broadcasting START/STOP notifications -
        // it is required for manifest-declared receiver of the status widget (when running on Android 8+).
        // Refer to https://developer.android.com/guide/components/broadcasts#manifest-declared-receivers
        const val ACTION_STARTED = "org.traccar.action.SERVICE_STARTED"
        const val ACTION_STOPPED = "org.traccar.action.SERVICE_STOPPED"
        private const val NOTIFICATION_ID = 1
        @Volatile
        private var activeService: TrackingService? = null

        // null means no service is running, false means it could not show the notification.
        fun refreshNotificationIfRunning(): Boolean? {
            val service = activeService ?: return null
            if (!service.prefs.status || !canShowTrackingNotification(service)) return false
            return try {
                service.showTrackingNotification()
                isTrackingNotificationVisible(service)
            } catch (e: RuntimeException) {
                Log.w(TAG, "Unable to restore tracking notification", e)
                false
            }
        }

        fun canShowTrackingNotification(context: Context): Boolean {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                val channel = manager.getNotificationChannel(MainApplication.PRIMARY_CHANNEL)
                return channel != null && channel.importance != NotificationManager.IMPORTANCE_NONE
            }
            return true
        }

        fun isTrackingNotificationVisible(context: Context): Boolean {
            if (!canShowTrackingNotification(context)) return false
            val manager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            return manager.activeNotifications.any { it.id == NOTIFICATION_ID }
        }

        fun canTrackNow(context: Context): Boolean =
            MainApplication.isAppVisible || isTrackingNotificationVisible(context)

        @SuppressLint("UnspecifiedImmutableFlag")
        private fun createNotification(context: Context): Notification {
            val builder = NotificationCompat.Builder(context, MainApplication.PRIMARY_CHANNEL)
                .setSmallIcon(R.drawable.app_icon)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentText(context.getString(R.string.tracking_notification_text))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            val intent: Intent
            if (!BuildConfig.HIDDEN_APP) {
                intent = Intent(context, MainActivity::class.java)
                builder
                    .setContentTitle(context.getString(R.string.settings_status_on_summary))
                    .setTicker(context.getString(R.string.settings_status_on_summary))
                    .color = ContextCompat.getColor(context, R.color.primary_dark)
            } else {
                intent = Intent(Settings.ACTION_SETTINGS)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            builder.setContentIntent(PendingIntent.getActivity(context, 0, intent, flags))
            val dismissIntent = Intent(context, StopTrackingReceiver::class.java)
                .setAction(StopTrackingReceiver.ACTION_NOTIFICATION_DISMISSED)
            builder.setDeleteIntent(PendingIntent.getBroadcast(context, 2, dismissIntent, flags))
            val stopIntent = Intent(context, StopTrackingReceiver::class.java)
                .setAction(StopTrackingReceiver.ACTION_STOP_TRACKING)
            builder.addAction(
                R.drawable.ic_baseline_x_24,
                context.getString(R.string.tracking_notification_stop),
                PendingIntent.getBroadcast(context, 1, stopIntent, flags)
            )
            return builder.build()
        }
    }
}
