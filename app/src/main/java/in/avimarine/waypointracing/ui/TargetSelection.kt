package `in`.avimarine.waypointracing.ui

/** Presentation-safe target index recovery; route semantics remain with Route. */
object TargetSelection {
    fun validOrFallback(requestedIndex: Int, targetCount: Int, fallbackIndex: Int): Int {
        if (targetCount <= 0) return 0
        return if (requestedIndex in 0 until targetCount) requestedIndex
        else fallbackIndex.coerceIn(0, targetCount - 1)
    }
}
