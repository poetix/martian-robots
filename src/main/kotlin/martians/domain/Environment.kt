package martians.domain

/** A robot's awareness of the world around it. */
interface Environment {
    fun isSafe(position: Position): Boolean
    fun hasScent(position: Position): Boolean
}

/** A change a robot makes to the world, applied by whatever is running the robot. */
sealed interface Effect

data class LeaveScent(val position: Position) : Effect
