package `in`.avimarine.waypointracing

import android.app.NotificationManager
import android.app.Application
import android.content.Context
import android.os.Build
import androidx.core.content.IntentCompat
import androidx.test.core.app.ApplicationProvider
import `in`.avimarine.waypointracing.route.GatePassing
import `in`.avimarine.waypointracing.route.Route
import `in`.avimarine.waypointracing.activities.RouteActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.util.Date

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
class GatePassNotifierTest {
    @Test
    fun regularAndFinishPassesCreateDistinctRouteNotifications() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val json = javaClass.classLoader!!.getResource("test_wptracing.json")!!.readText()
        val route = Route.fromGeoJson(json)
        val gate = route.elements.first()
        val finish = route.elements.last()

        fun pass(id: Int, name: String, time: Long) = GatePassing(
            eventName = route.eventName,
            routeId = route.id,
            routeLastUpdate = route.lastUpdate,
            deviceId = "test-device",
            gateId = id,
            gateName = name,
            time = Date(time),
        )

        val gatePass = pass(gate.id, gate.name, 1000)
        GatePassNotifier.show(context, route, gatePass, gate.routeElementType)
        GatePassNotifier.show(context, route, pass(finish.id, finish.name, 2000), finish.routeElementType)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertEquals(
            NotificationManager.IMPORTANCE_DEFAULT,
            manager.getNotificationChannel(MainApplication.PASS_CHANNEL)?.importance
        )
        val alerts = manager.activeNotifications.map { it.notification }
        assertEquals(2, alerts.size)
        val titles = alerts.map { it.extras.getString("android.title") }
        assertTrue(titles.contains(context.getString(R.string.gate_pass_notification_title)))
        assertTrue(titles.contains(context.getString(R.string.finish_pass_notification_title)))
        assertTrue(alerts.all { it.extras.getString("android.text")?.contains("☐ Upload pending") == true })
        assertTrue(alerts.all { it.contentIntent != null })

        GatePassNotifier.show(context, route, gatePass, gate.routeElementType, PassUploadStatus.State.UPLOADED)
        val updatedAlerts = manager.activeNotifications.map { it.notification }
        assertEquals(2, updatedAlerts.size)
        assertTrue(updatedAlerts.any {
            it.extras.getString("android.text")?.contains("☑ Uploaded to server") == true
        })

        val gateAlert = manager.activeNotifications.first {
            it.notification.extras.getString("android.title") == context.getString(R.string.gate_pass_notification_title)
        }
        manager.cancel(gateAlert.tag, gateAlert.id)
        GatePassNotifier.show(context, route, gatePass, gate.routeElementType, PassUploadStatus.State.FAILED)
        assertEquals(1, manager.activeNotifications.size)

        alerts.first().contentIntent.send()
        val opened = Shadows.shadowOf(context as Application).nextStartedActivity
        assertEquals(RouteActivity::class.java.name, opened.component?.className)
        assertEquals(route.id, IntentCompat.getParcelableExtra(opened, "route", Route::class.java)?.id)
    }
}
