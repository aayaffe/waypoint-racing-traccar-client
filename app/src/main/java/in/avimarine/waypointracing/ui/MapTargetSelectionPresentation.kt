package `in`.avimarine.waypointracing.ui

import `in`.avimarine.waypointracing.R
import `in`.avimarine.waypointracing.route.RouteElementType

/** Map preview labels only; selection is committed by the confirmation action in MapFragment. */
object MapTargetSelectionPresentation {
    fun confirmLabel(type: RouteElementType): Int = when (type) {
        RouteElementType.WAYPOINT -> R.string.map_target_selection_confirm_mark
        RouteElementType.FINISH -> R.string.map_target_selection_confirm_finish
        RouteElementType.GATE, RouteElementType.START -> R.string.map_target_selection_confirm_gate
    }
}
