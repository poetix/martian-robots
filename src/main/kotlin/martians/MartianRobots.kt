package martians

import kotlinx.collections.immutable.persistentHashSetOf
import martians.domain.Position
import martians.domain.RobotProgram
import martians.domain.RobotState
import martians.domain.SafeSquares
import martians.domain.WorldState
import martians.parsing.InputParser

object MartianRobots {
    /** Streams [lines] through parsing and execution, passing each robot's report to [emit] as soon as it is known. */
    fun run(lines: Sequence<String>, parser: InputParser = InputParser(standardCommands::get), emit: (String) -> Unit) {
        parser.parse(lines) { bounds, programs -> execute(programs, bounds).forEach { emit(it.report()) } }
    }

    /** Runs all of [input] and returns one report line per robot. */
    fun run(input: String, parser: InputParser = InputParser(standardCommands::get)): String =
        buildList { run(input.reader().buffered().lineSequence(), parser, ::add) }.joinToString("\n")

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
