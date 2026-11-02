package martians.parsing

import martians.domain.Command
import martians.domain.GridBounds
import martians.domain.Orientation
import martians.domain.Position
import martians.domain.RobotProgram
import martians.domain.RobotState

/**
 * Takes input lines one at a time, numbering them so that every failure can say which line it is on.
 * Each `parse` method consumes exactly one line, except [parseProgram], which consumes a robot's two.
 */
internal class LineParser(
    private val iterator: Iterator<String>,
    private val commandResolver: (Char) -> Command?,
) {
    private var lineNumber = 0

    fun hasMore(): Boolean = iterator.hasNext()

    fun parseGridBounds(): GridBounds =
        takeAndParse("grid size") { line ->
            val (xCoordinate, yCoordinate) = line.takeFields(2, "grid size as 'x y'")
            GridBounds(Position(0, 0), parsePosition(xCoordinate, yCoordinate))
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

    fun parseRobotState(): RobotState =
        takeAndParse("robot position") { line ->
            val (xCoordinate, yCoordinate, orientationName) =
                line.takeFields(3, "robot position as 'x y orientation'")

            val orientation = Orientation.entries.find { it.name == orientationName }
                ?: fail("'$orientationName' is not an orientation; expected one of N, E, S, W")

            RobotState(parsePosition(xCoordinate, yCoordinate), orientation)
        }

    fun parseInstructions(): List<Command> =
        takeAndParse("robot instructions") { line ->
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

    private fun <T> takeAndParse(expected: String, parse: (String) -> T): T {
        lineNumber += 1
        if (!iterator.hasNext()) fail("Expected $expected but reached end of input")
        return parse(iterator.next())
    }

    private fun String.takeFields(count: Int, description: String): List<String> {
        val fields = trim().split(WHITESPACE)
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
