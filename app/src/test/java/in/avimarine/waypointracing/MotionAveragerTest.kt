package `in`.avimarine.waypointracing

import org.junit.Assert.assertEquals
import org.junit.Test

class MotionAveragerTest {
    @Test
    fun averagesSpeedWithinWindowAndDropsOldFixes() {
        val averager = MotionAverager()
        averager.add(0, 2.0, 90.0, 30_000)
        assertEquals(3.0, averager.add(10_000, 4.0, 90.0, 30_000).first!!, 0.001)
        assertEquals(8.0, averager.add(40_001, 8.0, 90.0, 30_000).first!!, 0.001)
    }

    @Test
    fun averagesCourseAcrossNorth() {
        val averager = MotionAverager()
        averager.add(0, 2.0, 359.0, 30_000)
        assertEquals(0.0, averager.add(1_000, 2.0, 1.0, 30_000).second!!, 0.001)
    }

    @Test
    fun usesLatestCourseWhenOppositeHeadingsCancel() {
        val averager = MotionAverager()
        averager.add(0, 2.0, 0.0, 30_000)
        assertEquals(180.0, averager.add(1_000, 2.0, 180.0, 30_000).second!!, 0.001)
    }

    @Test
    fun disablesAveragingAndResetsForOlderFixes() {
        val averager = MotionAverager()
        averager.add(1_000, 2.0, 90.0, 30_000)
        assertEquals(8.0, averager.add(2_000, 8.0, 180.0, 0).first!!, 0.001)
        averager.add(5_000, 4.0, 180.0, 30_000)
        assertEquals(10.0, averager.add(4_000, 10.0, 180.0, 30_000).first!!, 0.001)
    }

    @Test
    fun missingMotionValuesDoNotContributeToOtherAverages() {
        val averager = MotionAverager()
        averager.add(0, 2.0, 90.0, 30_000)
        averager.add(1_000, null, null, 30_000)
        val (speed, course) = averager.add(2_000, 4.0, 90.0, 30_000)
        assertEquals(3.0, speed!!, 0.001)
        assertEquals(90.0, course!!, 0.001)
    }
}
