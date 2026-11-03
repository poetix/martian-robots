package martians.parsing

import martians.domain.Command
import martians.domain.GridBounds
import martians.domain.Orientation
import martians.domain.Position
import martians.domain.RobotProgram
import martians.domain.RobotState

/**
 * Takes input lines one at a time, numbering them so that every failure can say which line it is on.
 * Every line is trimmed. Blank lines are skipped, except the line after a robot's position, where a blank line
 * means a robot with no instructions. [parseGridBounds] consumes one non-blank line; [parseProgram] consumes a
 * robot's position line and the line after it.
 */
internal class LineParser(
    private val iterator: Iterator<String>,
    private val commandResolver: (Char) -> Command?,
) {
    /** The number of the last line taken; skipped blank lines are counted. */
    private var lineNumber = 0
    private var peeked: String? = null

    /** Whether any non-blank line remains, which can only be the start of another program. */
    fun hasMorePrograms(): Boolean {
        skipBlankLines()
        return peek() != null
    }

    fun parseGridBounds(): GridBounds =
        takeAndParse("grid size") { line ->
            val (xCoordinate, yCoordinate) = line.takeFields(2, "grid size as 'x y'")
            GridBounds.fromOriginTo(parsePosition(xCoordinate, yCoordinate))
        }

    fun parseProgram(gridBounds: GridBounds): RobotProgram =
        RobotProgram(
            parseRobotState().also { start ->
                if (!gridBounds.isSafe(start.position)) {
                    fail("robot starts at ${start.position.x} ${start.position.y}, outside the grid")
                }
            },
            parseInstructions(),
        )

    private fun parseRobotState(): RobotState =
        takeAndParse("robot position") { line ->
            val (xCoordinate, yCoordinate, orientationName) =
                line.takeFields(3, "robot position as 'x y orientation'")

            val orientation = Orientation.entries.find { it.name == orientationName }
                ?: fail("'$orientationName' is not an orientation; expected one of N, E, S, W")

            RobotState(parsePosition(xCoordinate, yCoordinate), orientation)
        }

    private fun parseInstructions(): List<Command> =
        takeAndParse("robot instructions", skipBlank = false) { line ->
            if (line.length > MAX_INSTRUCTION_LENGTH) {
                fail("Instructions are ${line.length} characters; at most $MAX_INSTRUCTION_LENGTH allowed")
            }
            line.map { letter ->
                commandResolver(letter) ?: fail("'$letter' is not a known instruction")
            }
        }

    private fun parsePosition(xCoordinate: String, yCoordinate: String): Position =
        Position(parseCoordinate(xCoordinate), parseCoordinate(yCoordinate))

    private fun parseCoordinate(field: String): Int {
        val value = field.toIntOrNull() ?: fail("'$field' is not a whole number")
        if (value !in 0..MAX_COORDINATE) fail("Coordinate $value is outside 0..$MAX_COORDINATE")
        return value
    }

    private fun <T> takeAndParse(expected: String, skipBlank: Boolean = true, parse: (String) -> T): T {
        if (skipBlank) skipBlankLines()
        val line = take() ?: run {
            lineNumber += 1
            fail("Expected $expected but reached end of input")
        }
        return parse(line)
    }

    private fun peek(): String? {
        if (peeked == null && iterator.hasNext()) peeked = iterator.next().trim()
        return peeked
    }

    private fun take(): String? = peek()?.also {
        peeked = null
        lineNumber += 1
    }

    private fun skipBlankLines() {
        while (peek()?.isEmpty() == true) take()
    }

    private fun String.takeFields(count: Int, description: String): List<String> {
        val fields = split(WHITESPACE)
        if (fields.size != count) fail("Expected $description but got '$this'")
        return fields
    }

    private fun fail(message: String): Nothing = throw InputException("Line $lineNumber: $message")

    companion object {
        const val MAX_COORDINATE = 50
        const val MAX_INSTRUCTION_LENGTH = 99
        private val WHITESPACE = Regex("\\s+")
    }
}
