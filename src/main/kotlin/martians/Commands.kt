package martians

import martians.domain.Command
import martians.domain.Environment
import martians.domain.LeaveScent
import martians.domain.Outcome
import martians.domain.RobotState

data object TurnLeft : Command {
    override fun execute(robot: RobotState, environment: Environment) = Outcome(robot.turnedLeft())
}

data object TurnRight : Command {
    override fun execute(robot: RobotState, environment: Environment) = Outcome(robot.turnedRight())
}

/**
 * Moves one square in the direction faced. Moving onto an unsafe square loses the robot and leaves a scent
 * where it stood, unless a scent is already there, in which case the move is ignored.
 */
data object Forward : Command {
    override fun execute(robot: RobotState, environment: Environment): Outcome {
        val moved = robot.moved()
        return when {
            environment.isSafe(moved.position) -> Outcome(moved)
            environment.hasScent(robot.position) -> Outcome(robot)
            else -> Outcome(robot.copy(isLost = true), listOf(LeaveScent(robot.position)))
        }
    }
}

/** The instruction letters understood by default; new command types are added here. */
val standardCommands: Map<Char, Command> = mapOf(
    'L' to TurnLeft,
    'R' to TurnRight,
    'F' to Forward,
)
