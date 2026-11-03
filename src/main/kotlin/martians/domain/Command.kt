package martians.domain


/**
 * One step of a robot's instructions. Given the robot and what it can perceive of its [Environment], returns the
 * robot as it becomes and the [Effect]s it has on the world, in order. Commands never change the world themselves.
 */
fun interface Command {
    /** Returns what becomes of [robot] when this command is executed, given what it perceives of [environment]. */
    fun execute(robot: RobotState, environment: Environment): Outcome
}

/**
 * The result of executing a [Command].
 *
 * @property robot the robot's state after the command.
 * @property effects changes the command makes to the world, in the order they should be applied.
 */
data class Outcome(val robot: RobotState, val effects: List<Effect> = emptyList())
