package martians.running

import martians.domain.Effect
import martians.domain.Environment
import martians.domain.LeaveScent
import martians.domain.Position
import martians.domain.SafeSquares

/** The surface of Mars as robots perceive it: fixed terrain plus the scents left so far. */
class Surface(private val safeSquares: SafeSquares) : Environment {
    private val scents = HashSet<Position>()

    override fun isSafe(position: Position) = safeSquares.isSafe(position)

    override fun hasScent(position: Position) = position in scents

    /** Applies [effect] to this surface, so that robots perceive it from now on. */
    fun apply(effect: Effect) {
        when (effect) {
            is LeaveScent -> scents.add(effect.position)
        }
    }
}
