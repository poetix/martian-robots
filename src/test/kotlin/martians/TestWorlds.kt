package martians

import martians.domain.GridBounds
import martians.domain.LeaveScent
import martians.domain.Orientation
import martians.domain.Outcome
import martians.domain.Position
import martians.domain.RobotState
import martians.domain.SafeSquares

infix fun Int.at(y: Int) = Position(this, y)

val grid5x3 = GridBounds(0 at 0, 5 at 3)

fun robot(position: Position, orientation: Orientation) = RobotState(position, orientation)

fun lost(position: Position, orientation: Orientation) = RobotState(position, orientation, isLost = true)

fun SafeSquares.withoutScent() = Surface(this)

fun SafeSquares.withScent(vararg positions: Position) = Surface(this, positions.toMutableSet())

fun RobotState.withoutEffects() = Outcome(this)

fun RobotState.leavingScent(position: Position) = Outcome(this, listOf(LeaveScent(position)))
