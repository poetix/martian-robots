package martians

import kotlinx.collections.immutable.persistentHashSetOf

object MartianRobots {
    /** Streams [lines] through parsing and execution, passing each robot's report to [emit] as soon as it is known. */
    fun run(lines: Sequence<String>, parser: InputParser = InputParser(), emit: (String) -> Unit) {
        parser.parse(lines) { bounds, programs -> execute(programs, bounds).forEach { emit(it.report()) } }
    }

    /** Runs all of [input] and returns one report line per robot. */
    fun run(input: String, parser: InputParser = InputParser()): String =
        buildList { run(input.inputLines(), parser, ::add) }.joinToString("\n")

    /** Lazily runs each robot in turn against [safeSquares], carrying scents forward, yielding each final state. */
    fun execute(programs: Sequence<RobotProgram>, safeSquares: SafeSquares): Sequence<RobotState> = sequence {
        var scents = persistentHashSetOf<Position>()
        for (program in programs) {
            val finished = WorldState(program.start, safeSquares, scents).execute(program.commands)
            scents = finished.scents
            yield(finished.robot)
        }
    }
}
