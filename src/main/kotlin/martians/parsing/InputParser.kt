package martians.parsing

import martians.domain.Command
import martians.domain.GridBounds
import martians.domain.RobotProgram

class InputException(message: String) : IllegalArgumentException(message)

data class Mission(val bounds: GridBounds, val robots: List<RobotProgram>)

/**
 * Parses the problem input: a grid line, then a position line and an instruction line per robot.
 * An empty instruction line means a robot with no instructions.
 *
 * [commandResolver] gives the command for an instruction letter, or null if the letter is not an instruction.
 */
class InputParser(private val commandResolver: (Char) -> Command?) {

    /**
     * Reads the grid from the first of [lines], then hands it to [consume] with the robots as a lazy, single-use
     * sequence: each robot's lines are read and parsed only when the consumer asks for it, so input of any length
     * can be processed in constant memory. Errors in a robot's lines surface when that robot is reached.
     */
    fun <T> parse(lines: Sequence<String>, consume: (GridBounds, Sequence<RobotProgram>) -> T): T {
        val lineParser = LineParser(lines.iterator(), commandResolver)
        val gridBounds = lineParser.parseGridBounds()

        val programs = sequence {
            while (lineParser.hasMore()) {
                yield(lineParser.parseProgram(gridBounds))
            }
        }.constrainOnce()

        return consume(gridBounds, programs)
    }

    /** Parses the whole of [input] into memory, splitting lines exactly as a line reader over a stream would. */
    fun parse(input: String): Mission = parse(input.reader().buffered().lineSequence()) { bounds, programs ->
        Mission(bounds, programs.toList())
    }
}
