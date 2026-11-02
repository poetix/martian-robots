package martians

import kotlinx.collections.immutable.toPersistentHashSet

infix fun Int.at(y: Int) = Position(this, y)

val grid5x3 = GridBounds(0 at 0, 5 at 3)

fun world(
    position: Position,
    orientation: Orientation,
    scents: Set<Position> = emptySet(),
    lost: Boolean = false,
    safeSquares: SafeSquares = grid5x3,
) = WorldState(RobotState(position, orientation, lost), safeSquares, scents.toPersistentHashSet())

fun commands(letters: String): List<Command> = letters.map(standardCommands::getValue)

/** A world before and after executing a string of instruction letters. */
data class Transition(val name: String, val before: WorldState, val letters: String, val after: WorldState)
