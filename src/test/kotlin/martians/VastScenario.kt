package martians

import kotlin.random.Random

/**
 * A square field [size] points across with roughly one point in 16 a hole, and [robotCount] robots on random
 * walks of 1..99 instructions from random safe starting points. Deterministic for a given [seed].
 */
class VastScenario(val size: Int, val robotCount: Int, seed: Long = 42) {
    val bounds = GridBounds(0 at 0, (size - 1) at (size - 1))
    val safeSquares = SafeSquares { bounds.isSafe(it) && !isHole(it) }
    val programs: List<RobotProgram>

    init {
        val random = Random(seed)
        val letters = standardCommands.keys.toList()
        programs = List(robotCount) {
            val start = generateSequence { random.nextInt(size) at random.nextInt(size) }.first(safeSquares::isSafe)
            val orientation = Orientation.entries.random(random)
            val commands = List(random.nextInt(1, InputParser.MAX_INSTRUCTION_LENGTH + 1)) {
                standardCommands.getValue(letters.random(random))
            }
            RobotProgram(RobotState(start, orientation), commands)
        }
    }

    companion object {
        /** Mixes both coordinates (SplitMix64 finaliser) so holes scatter rather than forming lines. */
        fun isHole(position: Position): Boolean {
            var h = position.x.toLong() * -7046029254386353131L + position.y.toLong()
            h = (h xor (h ushr 33)) * -49064778989728563L
            h = (h xor (h ushr 33)) * -4265267296055464877L
            h = h xor (h ushr 33)
            return h and 15L == 0L
        }
    }
}
