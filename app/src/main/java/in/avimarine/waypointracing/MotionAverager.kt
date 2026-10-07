package `in`.avimarine.waypointracing

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Moving averages for GPS speed and course. Course uses a circular mean across north. */
class MotionAverager {
    private data class Sample(val timeMs: Long, val speed: Double?, val course: Double?)

    private val samples = ArrayDeque<Sample>()

    fun clear() = samples.clear()

    fun add(timeMs: Long, speed: Double?, course: Double?, windowMs: Long): Pair<Double?, Double?> {
        if (windowMs <= 0 || samples.lastOrNull()?.timeMs?.let { timeMs < it } == true) {
            clear()
        }
        if (windowMs <= 0) return speed to course

        samples.addLast(Sample(timeMs, speed, course))
        while (samples.first().timeMs < timeMs - windowMs) samples.removeFirst()

        val speeds = samples.mapNotNull { it.speed }
        val averageSpeed = if (speed != null && speeds.isNotEmpty()) speeds.average() else speed

        val courses = samples.mapNotNull { it.course }
        val averageCourse = if (course != null && courses.isNotEmpty()) {
            val east = courses.sumOf { sin(Math.toRadians(it)) }
            val north = courses.sumOf { cos(Math.toRadians(it)) }
            if (east * east + north * north < 1e-12) course
            else (Math.toDegrees(atan2(east, north)) + 360.0) % 360.0
        } else course

        return averageSpeed to averageCourse
    }
}
