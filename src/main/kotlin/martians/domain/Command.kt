package martians.domain


/**
 * One step of a robot's instructions. Given the robot and what it can perceive of its [Environment], returns the
 * robot as it becomes and the [Effect]s it has on the world, in order. Commands never change the world themselves.
 */
fun interface Command {
    fun execute(robot: RobotState, environment: Environment): Outcome
}

data class Outcome(val robot: RobotState, val effects: List<Effect> = emptyList())
