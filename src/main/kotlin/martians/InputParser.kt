package martians

class InputException(message: String) : IllegalArgumentException(message)

data class RobotProgram(val start: RobotState, val commands: List<Command>)

data class Mission(val bounds: GridBounds, val robots: List<RobotProgram>)

/**
 * Parses the problem input: a grid line, then a position line and an instruction line per robot.
 * An empty instruction line means a robot with no instructions.
 */
class InputParser(private val commands: Map<Char, Command> = standardCommands) {

    /**
     * Reads the grid from the first of [lines], then hands it to [consume] with the robots as a lazy, single-use
     * sequence: each robot's lines are read and parsed only when the consumer asks for it, so input of any length
     * can be processed in constant memory. Errors in a robot's lines surface when that robot is reached.
     */
    fun <T> parse(lines: Sequence<String>, consume: (GridBounds, Sequence<RobotProgram>) -> T): T {
        val lineParser = LineParser(lines.iterator(), commands)
        val gridBounds = lineParser.parseGridBounds()

        val programs = sequence {
            while (lineParser.hasMore()) {
                yield(lineParser.parseProgram(gridBounds))
            }
        }.constrainOnce()

        return consume(gridBounds, programs)
    }

    /** Parses the whole of [input] into memory. */
    fun parse(input: String): Mission = parse(input.inputLines()) { bounds, programs -> Mission(bounds, programs.toList()) }
}

/** The lines of [this] text, ignoring a single trailing line terminator as a line reader would. */
fun String.inputLines(): Sequence<String> =
    lines().let { if (it.last().isEmpty()) it.dropLast(1) else it }.asSequence()
