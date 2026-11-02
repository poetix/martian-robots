package martians

import kotlinx.collections.immutable.persistentSetOf

object MartianRobots {
    /** Runs each robot in turn, carrying scents forward, and returns one report line per robot. */
    fun run(input: String, parser: InputParser = InputParser()): String {
        val mission = parser.parse(input)
        var scents = persistentSetOf<Position>()
        return mission.robots.joinToString("\n") { program ->
            val finished = WorldState(program.start, mission.bounds, scents).execute(program.commands)
            scents = finished.scents
            finished.robot.report()
        }
    }
}
