package martians

import martians.domain.Effect
import martians.domain.Environment
import martians.domain.LeaveScent
import martians.domain.Position
import martians.domain.SafeSquares

/**
 * The surface of Mars as robots perceive it: fixed terrain plus the scents left so far. The runner chooses the
 * [scents] store: a plain set for robots run one after another, or a concurrent one (such as
 * `ConcurrentHashMap.newKeySet()`) for robots run in parallel.
 */
class Surface(
    private val safeSquares: SafeSquares,
    private val scents: MutableSet<Position> = HashSet(),
) : Environment {
    override fun isSafe(position: Position) = safeSquares.isSafe(position)

    override fun hasScent(position: Position) = position in scents

    fun apply(effect: Effect) {
        when (effect) {
            is LeaveScent -> scents.add(effect.position)
        }
    }
}
