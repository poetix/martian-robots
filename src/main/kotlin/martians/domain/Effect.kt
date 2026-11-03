package martians.domain

/** A change a robot makes to the world, applied by whatever is running the robot. */
sealed interface Effect

data class LeaveScent(val position: Position) : Effect
