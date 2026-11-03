package martians.domain

/** A change a robot makes to the world, applied by whatever is running the robot. */
sealed interface Effect

/** Leaves a scent at [position], so that later robots will not fall off the world from there. */
data class LeaveScent(val position: Position) : Effect
