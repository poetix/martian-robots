package martians

import martians.commands.standardCommands
import martians.parsing.InputParser
import martians.running.SequentialMissionRunner

/**
 * The application as the brief specifies it: the text input format, the standard commands, robots run one after
 * another, and one report line per robot. This is the one place those choices are put together.
 */
object MartianRobots {
    private val parser = InputParser(standardCommands::get)

    /** Streams [lines] through parsing and execution, passing each robot's report to [emit] as soon as it is known. */
    fun run(lines: Sequence<String>, emit: (String) -> Unit) {
        parser.parse(lines) { bounds, programs ->
            SequentialMissionRunner.run(programs, bounds).forEach { emit(it.report()) }
        }
    }
}
