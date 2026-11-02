package martians

fun interface Command {
    fun execute(world: WorldState): WorldState
}

data object TurnLeft : Command {
    override fun execute(world: WorldState) = world.updateRobot(RobotState::turnedLeft)
}

data object TurnRight : Command {
    override fun execute(world: WorldState) = world.updateRobot(RobotState::turnedRight)
}

/**
 * Moves one square in the direction faced. Moving onto an unsafe square loses the robot and leaves a scent
 * where it stood, unless a scent is already there, in which case the move is ignored.
 */
data object Forward : Command {
    override fun execute(world: WorldState) = world.run {
        val moved = robot.moved()
        when {
            safeSquares.isSafe(moved.position) -> copy(robot = moved)
            robot.position in scents -> this
            else -> copy(robot = robot.copy(isLost = true), scents = scents.add(robot.position))
        }
    }
}

/** The instruction letters understood by default; new command types are added here. */
val standardCommands: Map<Char, Command> = mapOf(
    'L' to TurnLeft,
    'R' to TurnRight,
    'F' to Forward,
)
