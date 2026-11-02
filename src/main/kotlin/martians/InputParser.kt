package martians

class InputException(message: String) : IllegalArgumentException(message)

data class RobotProgram(val start: RobotState, val commands: List<Command>)

data class Mission(val bounds: GridBounds, val robots: List<RobotProgram>)

/**
 * Parses the problem input: a grid line, then a position line and an instruction line per robot.
 * A trailing line terminator is ignored; an empty instruction line means a robot with no instructions.
 */
class InputParser(private val commands: Map<Char, Command> = standardCommands) {

    fun parse(input: String): Mission {
        val lines = input.lines().let { if (it.size > 1 && it.last().isEmpty()) it.dropLast(1) else it }
        val bounds = atLine(1) { parseBounds(lines.first()) }

        val robotLines = lines.drop(1)
        if (robotLines.size % 2 != 0) {
            throw InputException("Line ${lines.size}: robot position has no instruction line after it")
        }

        val robots = robotLines.chunked(2).mapIndexed { index, (positionLine, instructionLine) ->
            val lineNumber = 2 + 2 * index
            val start = atLine(lineNumber) { parseRobotState(positionLine) }
            if (!bounds.isSafe(start.position)) {
                val (x, y) = start.position
                throw InputException("Line $lineNumber: robot starts at $x $y, outside the grid")
            }
            RobotProgram(start, atLine(lineNumber + 1) { parseCommands(instructionLine) })
        }
        return Mission(bounds, robots)
    }

    fun parseBounds(line: String): GridBounds {
        val fields = fields(line, expected = 2, "grid size as 'x y'")
        return GridBounds(Position(0, 0), Position(coordinate(fields[0]), coordinate(fields[1])))
    }

    fun parseRobotState(line: String): RobotState {
        val fields = fields(line, expected = 3, "robot position as 'x y orientation'")
        val orientation = Orientation.entries.find { it.name == fields[2] }
            ?: throw InputException("'${fields[2]}' is not an orientation; expected one of N, E, S, W")
        return RobotState(Position(coordinate(fields[0]), coordinate(fields[1])), orientation)
    }

    fun parseCommands(line: String): List<Command> {
        if (line.length > MAX_INSTRUCTION_LENGTH) {
            throw InputException("Instructions are ${line.length} characters; at most $MAX_INSTRUCTION_LENGTH allowed")
        }
        return line.map { letter ->
            commands[letter] ?: throw InputException(
                "'$letter' is not a known instruction; expected one of ${commands.keys.joinToString(", ")}"
            )
        }
    }

    private fun fields(line: String, expected: Int, description: String): List<String> {
        val fields = line.trim().split(WHITESPACE).filter { it.isNotEmpty() }
        if (fields.size != expected) throw InputException("Expected $description but got '$line'")
        return fields
    }

    private fun coordinate(field: String): Int {
        val value = field.toIntOrNull() ?: throw InputException("'$field' is not a whole number")
        if (value !in 0..MAX_COORDINATE) throw InputException("Coordinate $value is outside 0..$MAX_COORDINATE")
        return value
    }

    private fun <T> atLine(lineNumber: Int, parse: () -> T): T =
        try {
            parse()
        } catch (e: InputException) {
            throw InputException("Line $lineNumber: ${e.message}")
        }

    companion object {
        const val MAX_COORDINATE = 50
        const val MAX_INSTRUCTION_LENGTH = 99
        private val WHITESPACE = Regex("\\s+")
    }
}
