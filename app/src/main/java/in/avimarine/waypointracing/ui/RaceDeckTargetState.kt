package `in`.avimarine.waypointracing.ui

enum class RaceDeckTargetLabel {
    NEXT_MARK,
    NEAREST_GATE,
    NEAREST_FINISH,
}

data class RaceDeckTargetState(
    val name: String,
    val label: RaceDeckTargetLabel,
    val showsEndpoints: Boolean,
)

/** Pure presentation mapping; route detection and progression remain outside the UI layer. */
object RaceDeckTargetMapper {
    fun map(name: String, isMark: Boolean, isFinish: Boolean): RaceDeckTargetState =
        RaceDeckTargetState(
            name = name,
            label = when {
                isMark -> RaceDeckTargetLabel.NEXT_MARK
                isFinish -> RaceDeckTargetLabel.NEAREST_FINISH
                else -> RaceDeckTargetLabel.NEAREST_GATE
            },
            showsEndpoints = !isMark,
        )
}
