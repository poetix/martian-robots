package martians.domain

/**
 * Where a robot is and which way it faces.
 *
 * @property position the robot's grid point; for a lost robot, the last point it occupied on the grid.
 * @property orientation the direction the robot faces.
 * @property isLost whether the robot has moved off the world.
 */
data class RobotState(
    val position: Position,
    val orientation: Orientation,
    val isLost: Boolean = false,
) {
    /** This robot one step ahead in the direction it faces. */
    fun moved(): RobotState = copy(position = position.moved(orientation))

    /** This robot turned 90 degrees left, in the same place. */
    fun turnedLeft(): RobotState = copy(orientation = orientation.left())

    /** This robot turned 90 degrees right, in the same place. */
    fun turnedRight(): RobotState = copy(orientation = orientation.right())

    /** The robot's position and orientation as `x y O`, followed by `LOST` if it has been lost. */
    fun report(): String = "${position.x} ${position.y} $orientation" + if (isLost) " LOST" else ""
}

/**
 * A robot's starting state and the commands it is to execute.
 *
 * @property start the robot's state before any command executes.
 * @property commands the commands to execute, in order.
 */
data class RobotProgram(val start: RobotState, val commands: List<Command>)
