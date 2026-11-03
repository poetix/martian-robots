package martians.parsing

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import martians.at
import martians.commands.Forward
import martians.commands.TurnLeft
import martians.commands.TurnRight
import martians.commands.standardCommands
import martians.domain.Command
import martians.domain.GridBounds
import martians.domain.Orientation.E
import martians.domain.Orientation.N
import martians.domain.Outcome
import martians.domain.RobotProgram
import martians.domain.RobotState
import martians.grid5x3

class LineParserTest : FunSpec({

    fun parsing(vararg lines: String) = LineParser(lines.iterator(), standardCommands::get)

    context("grid bounds") {
        test("upper-right coordinates give bounds from 0 0") {
            parsing("5 3").parseGridBounds() shouldBe GridBounds(0 at 0, 5 at 3)
        }

        test("largest coordinate is 50") {
            parsing("50 50").parseGridBounds() shouldBe GridBounds(0 at 0, 50 at 50)
        }

        withData(
            nameFn = { "rejects '$it'" },
            "", "5", "5 3 1", "a 3", "-1 3", "51 3", "5 51",
        ) { line ->
            shouldThrow<InputException> { parsing(line).parseGridBounds() }
        }
    }

    context("robot position") {
        test("coordinates and orientation") {
            parsing("1 2 E").parseRobotState() shouldBe RobotState(1 at 2, E)
        }

        test("any whitespace separates fields") {
            parsing("  3\t2   N ").parseRobotState() shouldBe RobotState(3 at 2, N)
        }

        withData(
            nameFn = { "rejects '$it'" },
            "", "1 2", "1 2 E E", "1 2 X", "1 2 n", "x 2 N", "1 51 N",
        ) { line ->
            shouldThrow<InputException> { parsing(line).parseRobotState() }
        }
    }

    context("instructions") {
        test("each letter becomes a command") {
            parsing("LRF").parseInstructions() shouldBe listOf(TurnLeft, TurnRight, Forward)
        }

        test("an empty line is no commands") {
            parsing("").parseInstructions() shouldBe emptyList()
        }

        test("99 instructions are accepted") {
            parsing("F".repeat(99)).parseInstructions().size shouldBe 99
        }

        withData(
            nameFn = { "rejects '${it.take(10)}...' (${it.length} chars)" },
            "F".repeat(100), "LXF", "lrf", "L F",
        ) { line ->
            shouldThrow<InputException> { parsing(line).parseInstructions() }
        }

        test("letters are looked up with the command resolver supplied") {
            val backward = Command { robot, _ -> Outcome(robot) }
            val resolver = { letter: Char -> if (letter == 'B') backward else standardCommands[letter] }
            LineParser(listOf("FB").iterator(), resolver).parseInstructions() shouldBe listOf(Forward, backward)
        }

        test("a letter the resolver does not know is rejected, naming the letter") {
            shouldThrow<InputException> { parsing("LXF").parseInstructions() }
                .message shouldBe "Line 1: 'X' is not a known instruction"
        }
    }

    context("programs") {
        test("a position line and an instruction line make a program") {
            parsing("1 1 E", "RF").parseProgram(grid5x3) shouldBe
                RobotProgram(RobotState(1 at 1, E), listOf(TurnRight, Forward))
        }

        test("a robot may not start outside the grid") {
            shouldThrow<InputException> { parsing("6 1 E", "F").parseProgram(grid5x3) }
                .message shouldBe "Line 1: robot starts at 6 1, outside the grid"
        }
    }

    context("errors name the line they occur on") {
        test("lines are counted across everything parsed so far") {
            val parser = parsing("5 3", "1 1 E", "F", "1 1 Q", "F")
            val bounds = parser.parseGridBounds()
            parser.parseProgram(bounds)
            shouldThrow<InputException> { parser.parseProgram(bounds) }
                .message shouldBe "Line 4: 'Q' is not an orientation; expected one of N, E, S, W"
        }

        test("a missing line is reported at the number it would have had") {
            val parser = parsing("5 3", "1 1 E")
            val bounds = parser.parseGridBounds()
            shouldThrow<InputException> { parser.parseProgram(bounds) }
                .message shouldBe "Line 3: Expected robot instructions but reached end of input"
        }

        test("no input at all is missing line 1") {
            shouldThrow<InputException> { parsing().parseGridBounds() }
                .message shouldBe "Line 1: Expected grid size but reached end of input"
        }
    }
})
