package martians.parsing

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import martians.at
import martians.commands.Forward
import martians.commands.TurnRight
import martians.commands.standardCommands
import martians.domain.Command
import martians.domain.GridBounds
import martians.domain.Orientation.E
import martians.domain.Orientation.W
import martians.domain.Outcome
import martians.domain.RobotProgram
import martians.inputLines
import martians.robot

class InputParserTest : FunSpec({

    val parser = InputParser(standardCommands::get)

    /** Parses all of [input], returning the grid and every robot's program. */
    fun parseAll(input: String) = parser.parse(input.inputLines()) { bounds, programs -> bounds to programs.toList() }

    context("streaming") {
        test("the consumer's result is returned") {
            val result = Any()
            parser.parse(sequenceOf("5 3")) { _, _ -> result } shouldBeSameInstanceAs result
        }

        test("robots are read only as far as the consumer asks, so input may be unbounded") {
            val endless = sequenceOf("5 3") + generateSequence { "1 1 E" }.flatMap { sequenceOf(it, "F") }
            parser.parse(endless) { _, programs -> programs.take(2).toList() } shouldBe
                List(2) { RobotProgram(robot(1 at 1, E), listOf(Forward)) }
        }

        test("an error in a later robot surfaces only when that robot is reached") {
            val lines = sequenceOf("5 3", "1 1 E", "F", "1 1 Q", "F")
            parser.parse(lines) { _, programs -> programs.first().start } shouldBe robot(1 at 1, E)
            shouldThrow<InputException> { parser.parse(lines) { _, programs -> programs.toList() } }
        }

        test("robots can be consumed only once") {
            shouldThrow<IllegalStateException> {
                parser.parse(sequenceOf("5 3", "1 1 E", "F")) { _, programs -> programs.toList() + programs.toList() }
            }
        }
    }

    context("whole input") {
        test("grid followed by a position and instruction line per robot") {
            parseAll("5 3\n1 1 E\nRF\n0 3 W\n\n") shouldBe Pair(
                GridBounds(0 at 0, 5 at 3),
                listOf(
                    RobotProgram(robot(1 at 1, E), listOf(TurnRight, Forward)),
                    RobotProgram(robot(0 at 3, W), emptyList()),
                ),
            )
        }

        test("a grid with no robots gives no programs") {
            parseAll("5 3\n") shouldBe Pair(GridBounds(0 at 0, 5 at 3), emptyList())
        }

        test("Windows line endings are accepted") {
            parseAll("5 3\r\n1 1 E\r\nRF\r\n").second.size shouldBe 1
        }

        test("empty input is missing its grid line") {
            shouldThrow<InputException> { parseAll("") }
                .message shouldBe "Line 1: Expected grid size but reached end of input"
        }

        test("a final line terminator does not start an empty instruction line") {
            shouldThrow<InputException> { parseAll("5 3\n1 1 E\n") }
                .message shouldBe "Line 3: Expected robot instructions but reached end of input"
        }

        test("the command resolver is used for instruction letters") {
            val backward = Command { robot, _ -> Outcome(robot) }
            val resolver = { letter: Char -> if (letter == 'B') backward else standardCommands[letter] }
            InputParser(resolver).parse("5 3\n1 1 E\nFB".inputLines()) { _, programs -> programs.single().commands } shouldBe
                listOf(Forward, backward)
        }
    }
})
