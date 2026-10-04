package `in`.avimarine.waypointracing

import android.content.Intent
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import `in`.avimarine.waypointracing.utils.Preferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StopTrackingReceiverTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preferences = Preferences(PreferenceManager.getDefaultSharedPreferences(context))

    @Test
    fun notificationActionStopsTracking() {
        preferences.status = true

        StopTrackingReceiver().onReceive(
            context,
            Intent(context, StopTrackingReceiver::class.java)
                .setAction(StopTrackingReceiver.ACTION_STOP_TRACKING)
        )

        assertFalse(preferences.status)
    }

    @Test
    fun unrelatedBroadcastDoesNotStopTracking() {
        preferences.status = true

        StopTrackingReceiver().onReceive(context, Intent("unrelated"))

        assertTrue(preferences.status)
    }
}
