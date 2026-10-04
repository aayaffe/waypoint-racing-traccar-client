package `in`.avimarine.waypointracing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.ActivityManager
import androidx.preference.PreferenceManager
import `in`.avimarine.waypointracing.activities.StatusActivity
import `in`.avimarine.waypointracing.utils.Preferences

class StopTrackingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_STOP_TRACKING) return
        Preferences(PreferenceManager.getDefaultSharedPreferences(context)).status = false
        StatusActivity.addMessage(context.getString(R.string.tracking_stopped_from_notification))
        context.stopService(Intent(context, TrackingService::class.java))
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.appTasks.forEach { it.finishAndRemoveTask() }
    }

    companion object {
        const val ACTION_STOP_TRACKING = "in.avimarine.waypointracing.STOP_TRACKING"
    }
}
