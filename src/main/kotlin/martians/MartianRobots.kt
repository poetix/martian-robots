package martians

import kotlinx.collections.immutable.persistentHashSetOf

object MartianRobots {
    /** Parses [input], runs every robot, and returns one report line per robot. */
    fun run(input: String, parser: InputParser = InputParser()): String {
        val mission = parser.parse(input)
        return execute(mission.robots, mission.bounds).joinToString("\n") { it.report() }
    }

    /** Runs each robot in turn against [safeSquares], carrying scents forward, and returns each robot's final state. */
    fun execute(programs: List<RobotProgram>, safeSquares: SafeSquares): List<RobotState> {
        var scents = persistentHashSetOf<Position>()
        return programs.map { program ->
            val finished = WorldState(program.start, safeSquares, scents).execute(program.commands)
            scents = finished.scents
            finished.robot
        }
    }
}
