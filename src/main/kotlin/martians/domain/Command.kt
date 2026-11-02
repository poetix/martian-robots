package martians.domain

/** One step of a robot's instructions: takes the world as it is and returns the world as it becomes. */
fun interface Command {
    fun execute(world: WorldState): WorldState
}
