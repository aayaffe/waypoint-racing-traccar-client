package `in`.avimarine.waypointracing.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class TargetSelectionTest {
    @Test
    fun preservesValidSelectionAndRecoversInvalidSelection() {
        assertEquals(2, TargetSelection.validOrFallback(2, 4, 0))
        assertEquals(1, TargetSelection.validOrFallback(7, 4, 1))
        assertEquals(0, TargetSelection.validOrFallback(-1, 0, 5))
    }
}
