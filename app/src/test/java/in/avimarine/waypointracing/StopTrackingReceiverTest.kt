package `in`.avimarine.waypointracing

import android.content.Intent
import android.app.Activity
import android.app.NotificationManager
import android.os.Build
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import `in`.avimarine.waypointracing.utils.Preferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23, 26, 33])
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
    fun swipingNotificationDoesNotStopTracking() {
        preferences.status = true

        StopTrackingReceiver().onReceive(
            context,
            Intent(context, StopTrackingReceiver::class.java)
                .setAction(StopTrackingReceiver.ACTION_NOTIFICATION_DISMISSED)
        )

        assertTrue(preferences.status)
    }

    @Test
    fun swipingNotificationWhileAppIsVisibleDoesNotStopTracking() {
        val activity = Robolectric.buildActivity(Activity::class.java).create().start().resume()
        preferences.status = true

        StopTrackingReceiver().onReceive(
            context,
            Intent(context, StopTrackingReceiver::class.java)
                .setAction(StopTrackingReceiver.ACTION_NOTIFICATION_DISMISSED)
        )

        assertTrue(preferences.status)
        activity.pause().stop().destroy()
        assertTrue(preferences.status)
    }

    @Test
    fun unrelatedBroadcastDoesNotStopTracking() {
        preferences.status = true

        StopTrackingReceiver().onReceive(context, Intent("unrelated"))

        assertTrue(preferences.status)
    }

    @Test
    fun trackingChannelIsSilent() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = manager.getNotificationChannel(MainApplication.PRIMARY_CHANNEL)

        assertEquals(NotificationManager.IMPORTANCE_LOW, channel.importance)
        assertNull(channel.sound)
        assertFalse(channel.shouldVibrate())
    }
}
