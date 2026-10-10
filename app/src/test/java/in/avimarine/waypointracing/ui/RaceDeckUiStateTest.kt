package `in`.avimarine.waypointracing.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class RaceDeckUiStateTest {
    @Test
    fun stateKeepsHeaderTargetAndHealthIndependent() {
        val target = RaceDeckTargetMapper.map("MARK 5", isMark = true, isFinish = false)
        val state = RaceDeckUiState(courseName = "Spring Series", boatName = "Sea Breeze", target = target)
        assertEquals("Spring Series", state.courseName)
        assertEquals("Sea Breeze", state.boatName)
        assertEquals(RaceDeckTargetLabel.NEXT_MARK, state.target?.label)
    }
}
