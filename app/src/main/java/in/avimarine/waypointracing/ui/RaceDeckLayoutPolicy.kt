package `in`.avimarine.waypointracing.ui

/** Layout decisions that can be verified without an Activity or device. */
object RaceDeckLayoutPolicy {
    fun showTargetSelector(landscape: Boolean, screenHeightDp: Int): Boolean =
        !landscape && screenHeightDp >= PORTRAIT_MAP_MIN_HEIGHT_DP

    private const val PORTRAIT_MAP_MIN_HEIGHT_DP = 800
    private const val TABLET_MIN_WIDTH_DP = 600
    private const val PHONE_LANDSCAPE_MAP_FRACTION = 0.45f
    private const val TABLET_LANDSCAPE_MAP_FRACTION = 0.62f
    private const val PORTRAIT_MAP_TOP_FRACTION = 0.70f
    private const val MAX_PORTRAIT_MAP_TOP_FRACTION = 0.90f
    private const val FALLBACK_EXPANDED_TOP_FRACTION = 0.42f
    private const val MIN_EXPANDED_TOP_FRACTION = 0.30f
    private const val MAX_EXPANDED_TOP_FRACTION = 0.90f

    /**
     * Keep the measured target, endpoints, and position above the map while giving the map
     * as much height as the current screen allows. The bottom action row remains separate.
     */
    fun expandedMapTopFraction(
        rootHeightPx: Int,
        criticalContentBottomPx: Int,
        breathingRoomPx: Int,
    ): Float {
        if (rootHeightPx <= 0 || criticalContentBottomPx <= 0) {
            return FALLBACK_EXPANDED_TOP_FRACTION
        }
        return ((criticalContentBottomPx + breathingRoomPx).toFloat() / rootHeightPx)
            .coerceIn(MIN_EXPANDED_TOP_FRACTION, MAX_EXPANDED_TOP_FRACTION)
    }

    fun shouldShowPortraitMap(screenHeightDp: Int): Boolean =
        screenHeightDp >= PORTRAIT_MAP_MIN_HEIGHT_DP

    fun portraitMapTopFraction(
        rootHeightPx: Int,
        navigationContentBottomPx: Int,
        breathingRoomPx: Int,
    ): Float {
        if (rootHeightPx <= 0 || navigationContentBottomPx <= 0) {
            return PORTRAIT_MAP_TOP_FRACTION
        }
        return ((navigationContentBottomPx + breathingRoomPx).toFloat() / rootHeightPx)
            .coerceIn(PORTRAIT_MAP_TOP_FRACTION, MAX_PORTRAIT_MAP_TOP_FRACTION)
    }

    fun usesTargetsSidePanel(smallestScreenWidthDp: Int): Boolean =
        smallestScreenWidthDp >= TABLET_MIN_WIDTH_DP

    fun landscapeMapWidthFraction(smallestScreenWidthDp: Int): Float =
        if (usesTargetsSidePanel(smallestScreenWidthDp)) {
            TABLET_LANDSCAPE_MAP_FRACTION
        } else {
            PHONE_LANDSCAPE_MAP_FRACTION
        }
}
