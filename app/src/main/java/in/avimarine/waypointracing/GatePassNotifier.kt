package `in`.avimarine.waypointracing

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import `in`.avimarine.waypointracing.activities.RouteActivity
import `in`.avimarine.waypointracing.route.GatePassing
import `in`.avimarine.waypointracing.route.Route
import `in`.avimarine.waypointracing.route.RouteElementType

object GatePassNotifier {
    fun show(
        context: Context,
        route: Route,
        pass: GatePassing,
        type: RouteElementType,
        status: PassUploadStatus.State = PassUploadStatus.State.PENDING,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val finish = type == RouteElementType.FINISH
        val title = context.getString(
            if (finish) R.string.finish_pass_notification_title else R.string.gate_pass_notification_title
        )
        val uploadStatus = when (status) {
            PassUploadStatus.State.PENDING -> R.string.pass_upload_pending
            PassUploadStatus.State.UPLOADED -> R.string.pass_uploaded_checked
            PassUploadStatus.State.FAILED -> R.string.pass_upload_failed_status
        }
        val message = context.getString(
            R.string.pass_notification_message,
            pass.gateName,
            context.getString(R.string.pass_saved_checked),
            context.getString(uploadStatus),
        )
        val intent = Intent(context, RouteActivity::class.java).apply {
            putExtra("route", route)
            // A unique identity keeps earlier pass alerts linked to their own route status.
            data = Uri.Builder().scheme("waypointracing").authority("pass")
                .appendPath(pass.routeId).appendPath(pass.gateId.toString())
                .appendPath(pass.time.time.toString())
                .appendPath(pass.sourceEventId).build()
        }
        val notificationTag = intent.data.toString()
        if (status != PassUploadStatus.State.PENDING) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.activeNotifications.none { it.tag == notificationTag && it.id == PASS_NOTIFICATION_ID }) return
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, flags)
        val notification = NotificationCompat.Builder(context, MainApplication.PASS_CHANNEL)
            .setSmallIcon(if (finish) R.drawable.ic_finish_flag else R.drawable.ic_stat_gate_pass)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationTag, PASS_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.w("GatePassNotifier", "Unable to display pass notification", e)
        }
    }

    private const val PASS_NOTIFICATION_ID = 2
}
