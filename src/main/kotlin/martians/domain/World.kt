package martians.domain

data class RobotState(
    val position: Position,
    val orientation: Orientation,
    val isLost: Boolean = false,
) {
    /** This robot one step ahead in the direction it faces. */
    fun moved(): RobotState = copy(position = position.moved(orientation))

    fun turnedLeft(): RobotState = copy(orientation = orientation.left())

    fun turnedRight(): RobotState = copy(orientation = orientation.right())

    /** The robot's position and orientation as `x y O`, followed by `LOST` if it has been lost. */
    fun report(): String = "${position.x} ${position.y} $orientation" + if (isLost) " LOST" else ""
}

/** A robot's starting state and the commands it is to execute. */
data class RobotProgram(val start: RobotState, val commands: List<Command>) {
    /**
     * Executes the commands in order against [environment], passing each effect to [apply], in order, as soon as it occurs, and
     * returns the robot's final state. Once the robot is lost, remaining commands are not executed.
     */
    fun run(environment: Environment, apply: (Effect) -> Unit): RobotState {
        var robot = start
        for (command in commands) {
            if (robot.isLost) break
            val outcome = command.execute(robot, environment)
            outcome.effects.forEach(apply)
            robot = outcome.robot
        }
        return robot
    }
}
