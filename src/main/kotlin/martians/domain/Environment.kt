package martians.domain

/** A robot's awareness of the world around it. */
interface Environment {
    /** Whether a robot can stand on [position] without being lost. */
    fun isSafe(position: Position): Boolean

    /** Whether a robot has been lost from [position], leaving a scent there. */
    fun hasScent(position: Position): Boolean
}
