package `in`.avimarine.waypointracing.route

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import `in`.avimarine.waypointracing.PassUploadStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class GatePassPersistenceTest {
    @Test
    fun localLegacyPassFieldsAndUploadStateSurviveSerialization() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        val courseJson = javaClass.classLoader!!.getResource("test_wptracing.json")!!.readText()
        val route = Route.fromGeoJson(courseJson)
        val finish = route.elements.last()
        val pass = GatePassing(
            eventName = route.eventName,
            routeId = route.id,
            routeLastUpdate = route.lastUpdate,
            deviceId = "legacy-test-device",
            userId = "legacy-test-user",
            boatName = "Sea Test",
            gateId = finish.id,
            gateName = finish.name,
            time = Date(1_700_000_045_000L),
            latitude = 32.1655,
            longitude = 34.786667,
            speed = 5.8,
            course = 101.0,
            accuracy = 4.0,
            battery = 82.0,
            appVersion = 123L,
            eventId = "legacy-event",
            boatId = "legacy-boat",
            routeVersion = "legacy-route-version",
            sessionId = "legacy-session",
            sourceEventId = "legacy-source-event",
        )

        GatePassings.addGatePass(context, pass)

        val persisted = GatePassings.getCurrentRouteGatePassings(context, route.id).passes.single()
        assertEquals(pass, persisted)
        assertEquals(PassUploadStatus.State.PENDING, PassUploadStatus.get(context, persisted))
        assertTrue(PassUploadStatus.isStatusKey(PassUploadStatus.key(persisted)))

        PassUploadStatus.set(context, persisted, PassUploadStatus.State.UPLOADED)
        assertEquals(PassUploadStatus.State.UPLOADED, PassUploadStatus.get(context, persisted))
    }
}
