package martians

import io.kotest.matchers.shouldBe
import martians.domain.Command
import martians.domain.Effect
import martians.domain.GridBounds
import martians.domain.LeaveScent
import martians.domain.Orientation
import martians.domain.Position
import martians.domain.RobotProgram
import martians.domain.RobotState
import martians.domain.SafeSquares

infix fun Int.at(y: Int) = Position(this, y)

val grid5x3 = GridBounds(0 at 0, 5 at 3)

fun robot(position: Position, orientation: Orientation) = RobotState(position, orientation)

fun lost(position: Position, orientation: Orientation) = RobotState(position, orientation, isLost = true)

fun commands(letters: String): List<Command> = letters.map(standardCommands::getValue)

/**
 * A robot before and after executing a string of instruction letters, on a surface with the given [scents] and
 * [safeSquares], and the [effects] it should produce along the way.
 */
data class Transition(
    val name: String,
    val before: RobotState,
    val letters: String,
    val after: RobotState,
    val effects: List<Effect> = emptyList(),
    val scents: Set<Position> = emptySet(),
    val safeSquares: SafeSquares = grid5x3,
) {
    fun verify() {
        val produced = mutableListOf<Effect>()
        RobotProgram(before, commands(letters)).run(Surface(safeSquares, scents.toMutableSet()), produced::add) shouldBe
            after
        produced shouldBe effects
    }
}

fun scentAt(position: Position): List<Effect> = listOf(LeaveScent(position))
