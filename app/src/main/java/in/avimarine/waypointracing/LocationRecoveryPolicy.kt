package `in`.avimarine.waypointracing

internal object LocationRecoveryPolicy {
    private const val MIN_TIMEOUT_MS = 120_000L

    fun shouldRestart(
        nowElapsedMs: Long,
        lastRequestElapsedMs: Long,
        lastRawFixElapsedMs: Long,
        lastAcceptedFixElapsedMs: Long,
        intervalMs: Long,
        requireAcceptedFix: Boolean,
    ): Boolean {
        val timeoutMs = maxOf(MIN_TIMEOUT_MS, intervalMs.coerceIn(1_000L, 86_400_000L) * 4)
        val lastRaw = maxOf(lastRequestElapsedMs, lastRawFixElapsedMs)
        val lastAccepted = maxOf(lastRequestElapsedMs, lastAcceptedFixElapsedMs)
        return nowElapsedMs - lastRaw >= timeoutMs ||
            (requireAcceptedFix && nowElapsedMs - lastAccepted >= timeoutMs)
    }
}
