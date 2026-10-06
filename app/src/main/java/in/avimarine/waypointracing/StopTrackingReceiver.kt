package `in`.avimarine.waypointracing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.ActivityManager
import android.app.NotificationManager
import androidx.preference.PreferenceManager
import `in`.avimarine.waypointracing.activities.StatusActivity
import `in`.avimarine.waypointracing.utils.Preferences

class StopTrackingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val closeApp = intent.action == ACTION_STOP_TRACKING
        val notificationDismissed = intent.action == ACTION_NOTIFICATION_DISMISSED
        val notificationsBlocked = intent.action == NotificationManager.ACTION_APP_BLOCK_STATE_CHANGED ||
            intent.action == NotificationManager.ACTION_NOTIFICATION_CHANNEL_BLOCK_STATE_CHANGED
        if (!closeApp) {
            if (!notificationDismissed && !notificationsBlocked) return
            if (MainApplication.isAppVisible || TrackingService.refreshNotificationIfRunning() != false) return
        }
        Preferences(PreferenceManager.getDefaultSharedPreferences(context)).status = false
        if (closeApp) {
            StatusActivity.addMessage(context.getString(R.string.tracking_stopped_from_notification))
        }
        context.stopService(Intent(context, TrackingService::class.java))
        if (closeApp) {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.appTasks.forEach { it.finishAndRemoveTask() }
        }
    }

    companion object {
        const val ACTION_STOP_TRACKING = "in.avimarine.waypointracing.STOP_TRACKING"
        const val ACTION_NOTIFICATION_DISMISSED = "in.avimarine.waypointracing.NOTIFICATION_DISMISSED"
    }
}
