package martians.domain

/** A robot's awareness of the world around it: which squares are safe, and where scents have been left. */
interface Environment : SafeSquares {
    /** Whether a robot has been lost from [position], leaving a scent there. */
    fun hasScent(position: Position): Boolean
}
