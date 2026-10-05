package `in`.avimarine.waypointracing.activities

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Looper
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import `in`.avimarine.waypointracing.R
import `in`.avimarine.waypointracing.route.GatePassing
import `in`.avimarine.waypointracing.route.GatePassings
import `in`.avimarine.waypointracing.route.Route
import `in`.avimarine.waypointracing.ui.RouteElementFullAdapter
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.util.Date

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
class RouteActivityTest {
    @Test
    fun refreshesGatePassesRecordedWhileResultsScreenWasPaused() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val json = javaClass.classLoader!!.getResource("test_wptracing.json")!!.readText()
        val route = Route.fromGeoJson(json)
        GatePassings.reset(context, route, resetCurrentRouteOnly = false)
        val intent = Intent(context, RouteActivity::class.java).putExtra("route", route)
        val controller = Robolectric.buildActivity(RouteActivity::class.java, intent).create().start().resume()
        val activity = controller.get()

        controller.pause()
        for (gateIndex in 2..3) {
            val gate = route.elements[gateIndex]
            GatePassings.addGatePass(context, GatePassing(
                eventName = route.eventName,
                routeId = route.id,
                routeLastUpdate = route.lastUpdate,
                deviceId = "test-device",
                gateId = gate.id,
                gateName = gate.name,
                time = Date(1_000L + gateIndex),
            ))
        }
        assertEquals(2, GatePassings.getCurrentRouteGatePassings(context, route.id).passes.size)
        controller.resume()

        val adapter = activity.findViewById<RecyclerView>(R.id.route_recycler_view).adapter as RouteElementFullAdapter
        for (attempt in 0 until 50) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            if (adapter.currentList.size > 3 && adapter.currentList[2].gp != null && adapter.currentList[3].gp != null) break
            Thread.sleep(20)
        }
        assertNotNull(adapter.currentList[2].gp)
        assertNotNull(adapter.currentList[3].gp)
        controller.pause().stop().destroy()
    }
}
