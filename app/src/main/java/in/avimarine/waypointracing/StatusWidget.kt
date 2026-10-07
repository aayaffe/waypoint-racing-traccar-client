package `in`.avimarine.waypointracing

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.preference.PreferenceManager
import `in`.avimarine.waypointracing.activities.MainActivity
import `in`.avimarine.waypointracing.utils.Preferences

/** Shows the service state without changing the race or tracking controls. */
class StatusWidget : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            TrackingService.ACTION_STARTED -> updateWidgets(context, true)
            TrackingService.ACTION_STOPPED -> updateWidgets(context, false)
            else -> super.onReceive(context, intent)
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val enabled = Preferences(PreferenceManager.getDefaultSharedPreferences(context)).status
        update(context, manager, ids, enabled)
    }

    private fun updateWidgets(context: Context, enabled: Boolean) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, StatusWidget::class.java))
        update(context, manager, ids, enabled)
    }

    private fun update(context: Context, manager: AppWidgetManager, ids: IntArray, enabled: Boolean) {
        for (id in ids) {
            val views = RemoteViews(context.packageName, R.layout.status_widget)
            views.setTextViewText(
                R.id.tracking_status,
                context.getString(if (enabled) R.string.widget_tracking_on else R.string.widget_tracking_off)
            )
            val intent = Intent(context, MainActivity::class.java)
            val click = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.tracking_status, click)
            manager.updateAppWidget(id, views)
        }
    }
}
