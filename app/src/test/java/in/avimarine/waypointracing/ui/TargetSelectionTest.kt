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

    @Test
    fun mapPreviewUsesAnExplicitActionLabelForEachTargetKind() {
        assertEquals(
            `in`.avimarine.waypointracing.R.string.map_target_selection_confirm_mark,
            MapTargetSelectionPresentation.confirmLabel(`in`.avimarine.waypointracing.route.RouteElementType.WAYPOINT),
        )
        assertEquals(
            `in`.avimarine.waypointracing.R.string.map_target_selection_confirm_gate,
            MapTargetSelectionPresentation.confirmLabel(`in`.avimarine.waypointracing.route.RouteElementType.GATE),
        )
        assertEquals(
            `in`.avimarine.waypointracing.R.string.map_target_selection_confirm_finish,
            MapTargetSelectionPresentation.confirmLabel(`in`.avimarine.waypointracing.route.RouteElementType.FINISH),
        )
    }
}
