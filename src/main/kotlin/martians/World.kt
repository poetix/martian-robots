package martians

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashSetOf

data class RobotState(
    val position: Position,
    val orientation: Orientation,
    val isLost: Boolean = false,
) {
    /** This robot one step ahead in the direction it faces. */
    fun moved(): RobotState = copy(position = position.moved(orientation))

    fun turnedLeft(): RobotState = copy(orientation = orientation.left())

    fun turnedRight(): RobotState = copy(orientation = orientation.right())
}

/**
 * Everything a command can see or change: the current robot, where it is safe to go, and scents left so far.
 * Scents are a persistent set, so leaving one shares structure with the previous world rather than copying it.
 */
data class WorldState(
    val robot: RobotState,
    val safeSquares: SafeSquares,
    val scents: PersistentSet<Position> = persistentHashSetOf(),
) {
    fun updateRobot(update: RobotState.() -> RobotState): WorldState = copy(robot = robot.update())

    /** Applies [commands] in order; once the robot is lost, remaining commands are not executed. */
    fun execute(commands: List<Command>): WorldState =
        commands.fold(this) { world, command -> if (world.robot.isLost) world else command.execute(world) }
}
