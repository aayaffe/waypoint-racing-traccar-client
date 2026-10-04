package `in`.avimarine.waypointracing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationRecoveryPolicyTest {
    @Test
    fun restartsWhenNoFixArrivesAfterRegistration() {
        assertFalse(LocationRecoveryPolicy.shouldRestart(119_999, 0, 0, 0, 8_000, true))
        assertTrue(LocationRecoveryPolicy.shouldRestart(120_000, 0, 0, 0, 8_000, true))
    }

    @Test
    fun restartsWhenRawFixesContinueButAllAreFiltered() {
        assertTrue(LocationRecoveryPolicy.shouldRestart(130_000, 0, 125_000, 1_000, 8_000, true))
    }

    @Test
    fun recentAcceptedFixKeepsSubscriptionAlive() {
        assertFalse(LocationRecoveryPolicy.shouldRestart(130_000, 0, 125_000, 125_000, 8_000, true))
    }

    @Test
    fun respectsLongerRequestedIntervals() {
        assertFalse(LocationRecoveryPolicy.shouldRestart(239_999, 0, 0, 0, 60_000, true))
        assertTrue(LocationRecoveryPolicy.shouldRestart(240_000, 0, 0, 0, 60_000, true))
    }

    @Test
    fun distanceFilteringDoesNotTriggerRecoveryWhileRawFixesContinue() {
        assertFalse(LocationRecoveryPolicy.shouldRestart(130_000, 0, 125_000, 1_000, 8_000, false))
    }
}
