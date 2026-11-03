package martians.domain

/** A robot's awareness of the world around it. */
interface Environment {
    fun isSafe(position: Position): Boolean
    fun hasScent(position: Position): Boolean
}
