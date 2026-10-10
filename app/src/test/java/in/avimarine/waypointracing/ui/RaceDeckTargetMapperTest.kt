package `in`.avimarine.waypointracing.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RaceDeckTargetMapperTest {
    @Test
    fun mapsMarksGatesAndFinishesToTheirSupportedPresentation() {
        val mark = RaceDeckTargetMapper.map("MARK 5", isMark = true, isFinish = false)
        val gate = RaceDeckTargetMapper.map("GATE 4", isMark = false, isFinish = false)
        val finish = RaceDeckTargetMapper.map("FINISH", isMark = false, isFinish = true)

        assertEquals(RaceDeckTargetLabel.NEXT_MARK, mark.label)
        assertFalse(mark.showsEndpoints)
        assertEquals(RaceDeckTargetLabel.NEAREST_GATE, gate.label)
        assertTrue(gate.showsEndpoints)
        assertEquals(RaceDeckTargetLabel.NEAREST_FINISH, finish.label)
    }
}
