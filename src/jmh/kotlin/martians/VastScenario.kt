package martians

import martians.commands.standardCommands
import martians.domain.GridBounds
import martians.domain.Orientation
import martians.domain.RobotProgram
import martians.domain.RobotState
import martians.domain.SafeSquares
import martians.parsing.LineParser
import kotlin.random.Random

/**
 * A square field [size] points across, and [robotCount] robots on random walks of 1..99 instructions from random
 * safe starting points. Deterministic for a given [seed].
 *
 * A point is a hole when the low four bits of its hash code are zero. `Position` is a data class, so its hash is
 * `31 * x + y`, and holes fall on regular diagonals: one point in 16 along every row and column.
 */
class VastScenario(size: Int, val robotCount: Int, seed: Long = 42) {
    val bounds = GridBounds(0 at 0, (size - 1) at (size - 1))
    val safeSquares = SafeSquares { bounds.isSafe(it) && it.hashCode() and 15 != 0 }
    val programs: List<RobotProgram> = makePrograms(size, seed)

    private fun makePrograms(size: Int, seed: Long): List<RobotProgram> {
        val random = Random(seed)
        val letters = standardCommands.keys.toList()
        return List(robotCount) {
            val start = generateSequence { random.nextInt(size) at random.nextInt(size) }.first(safeSquares::isSafe)
            val orientation = Orientation.entries.random(random)
            val commands = List(random.nextInt(1, LineParser.MAX_INSTRUCTION_LENGTH + 1)) {
                standardCommands.getValue(letters.random(random))
            }
            RobotProgram(RobotState(start, orientation), commands)
        }
    }
}
