package `in`.avimarine.waypointracing.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EtaCalculatorTest {
    @Test
    fun arrivalUsesNauticalMilesAndKnots() {
        val fixTime = 1_700_000_000_000L
        assertEquals(fixTime + 5_400_000L, EtaCalculator.arrivalTimeMillis(3.0, 2.0, fixTime))
    }

    @Test
    fun arrivalIsUnavailableWithoutProgressTowardTarget() {
        val fixTime = 1_700_000_000_000L
        assertNull(EtaCalculator.arrivalTimeMillis(3.0, 0.0, fixTime))
        assertNull(EtaCalculator.arrivalTimeMillis(3.0, -1.0, fixTime))
        assertNull(EtaCalculator.arrivalTimeMillis(Double.NaN, 2.0, fixTime))
        assertNull(EtaCalculator.arrivalTimeMillis(3.0, Double.POSITIVE_INFINITY, fixTime))
        assertNull(EtaCalculator.arrivalTimeMillis(3.0, 2.0, 0L))
    }
}
