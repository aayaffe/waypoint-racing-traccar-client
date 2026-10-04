package `in`.avimarine.waypointracing.event

import androidx.test.core.app.ApplicationProvider
import androidx.preference.PreferenceManager
import `in`.avimarine.waypointracing.utils.Preferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EventPreferencesTest {
    @Test
    fun consentPersistsForSameAssignmentAndResetsForNewAssignment() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferences.edit().clear().commit()
        val preferences = Preferences(sharedPreferences)

        preferences.eventSession = EventSession("event-a", "boat-a", "route-a", "v1", "session-a")
        preferences.eventLocationUploadConsent = true
        preferences.eventSession = EventSession("event-a", "boat-a", "route-a", "v1", "session-b")
        assertTrue(preferences.eventLocationUploadConsent)

        preferences.eventSession = EventSession("event-b", "boat-a", "route-b", "v1", "session-c")
        assertFalse(preferences.eventLocationUploadConsent)

        preferences.beginEventAssignmentLookup()
        assertTrue(preferences.eventAssignmentLookupPending)
        preferences.clearEventSession()
        assertFalse(preferences.eventAssignmentLookupPending)
    }
}
