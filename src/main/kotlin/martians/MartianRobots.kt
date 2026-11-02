package martians

import martians.domain.RobotProgram
import martians.domain.RobotState
import martians.domain.SafeSquares
import martians.parsing.InputParser

object MartianRobots {
    /** Streams [lines] through parsing and execution, passing each robot's report to [emit] as soon as it is known. */
    fun run(lines: Sequence<String>, parser: InputParser = InputParser(standardCommands::get), emit: (String) -> Unit) {
        parser.parse(lines) { bounds, programs -> execute(programs, bounds).forEach { emit(it.report()) } }
    }

    /** Runs all of [input] and returns one report line per robot. */
    fun run(input: String, parser: InputParser = InputParser(standardCommands::get)): String =
        buildList { run(input.reader().buffered().lineSequence(), parser, ::add) }.joinToString("\n")

    /**
     * Lazily runs each robot in turn on one [Surface], so each robot sees the scents left by those before it, and
     * yields each robot's final state.
     */
    fun execute(programs: Sequence<RobotProgram>, safeSquares: SafeSquares): Sequence<RobotState> = sequence {
        val surface = Surface(safeSquares)
        for (program in programs) {
            yield(program.run(surface, surface::apply))
        }
    }
}
