package martians

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeSameInstanceAs
import martians.Orientation.E
import martians.Orientation.N
import martians.Orientation.W

class InputParserTest : FunSpec({

    val parser = InputParser()

    context("grid bounds") {
        test("upper-right coordinates give bounds from 0 0") {
            parser.parseBounds("5 3") shouldBe GridBounds(0 at 0, 5 at 3)
        }

        test("largest coordinate is 50") {
            parser.parseBounds("50 50") shouldBe GridBounds(0 at 0, 50 at 50)
        }

        withData(
            nameFn = { "rejects '$it'" },
            "", "5", "5 3 1", "a 3", "-1 3", "51 3", "5 51",
        ) { line ->
            shouldThrow<InputException> { parser.parseBounds(line) }
        }
    }

    context("robot position") {
        test("coordinates and orientation") {
            parser.parseRobotState("1 2 E") shouldBe RobotState(1 at 2, E)
        }

        test("any whitespace separates fields") {
            parser.parseRobotState("  3\t2   N ") shouldBe RobotState(3 at 2, N)
        }

        withData(
            nameFn = { "rejects '$it'" },
            "", "1 2", "1 2 E E", "1 2 X", "1 2 n", "x 2 N", "1 51 N",
        ) { line ->
            shouldThrow<InputException> { parser.parseRobotState(line) }
        }
    }

    context("instructions") {
        test("each letter becomes a command") {
            parser.parseCommands("LRF") shouldBe listOf(TurnLeft, TurnRight, Forward)
        }

        test("an empty line is no commands") {
            parser.parseCommands("") shouldBe emptyList()
        }

        test("99 instructions are accepted") {
            parser.parseCommands("F".repeat(99)).size shouldBe 99
        }

        withData(
            nameFn = { "rejects '${it.take(10)}...' (${it.length} chars)" },
            "F".repeat(100), "LXF", "lrf", "L F",
        ) { line ->
            shouldThrow<InputException> { parser.parseCommands(line) }
        }

        test("further command types can be supplied") {
            val backward = Command { it }
            InputParser(standardCommands + ('B' to backward)).parseCommands("FB") shouldBe listOf(Forward, backward)
        }
    }

    context("streaming") {
        test("the consumer's result is returned") {
            val result = Any()
            parser.parse(sequenceOf("5 3")) { _, _ -> result } shouldBeSameInstanceAs result
        }

        test("robots are read only as far as the consumer asks, so input may be unbounded") {
            val endless = sequenceOf("5 3") + generateSequence { "1 1 E" }.flatMap { sequenceOf(it, "F") }
            parser.parse(endless) { _, programs -> programs.take(2).toList() } shouldBe
                List(2) { RobotProgram(RobotState(1 at 1, E), listOf(Forward)) }
        }

        test("an error in a later robot surfaces only when that robot is reached") {
            val lines = sequenceOf("5 3", "1 1 E", "F", "1 1 Q", "F")
            parser.parse(lines) { _, programs -> programs.first().start } shouldBe RobotState(1 at 1, E)
            shouldThrow<InputException> { parser.parse(lines) { _, programs -> programs.toList() } }
                .message shouldStartWith "Line 4:"
        }

        test("robots can be consumed only once") {
            shouldThrow<IllegalStateException> {
                parser.parse(sequenceOf("5 3", "1 1 E", "F")) { _, programs -> programs.toList() + programs.toList() }
            }
        }
    }

    context("whole input") {
        test("grid followed by a position and instruction line per robot") {
            parser.parse("5 3\n1 1 E\nRF\n0 3 W\n\n") shouldBe Mission(
                GridBounds(0 at 0, 5 at 3),
                listOf(
                    RobotProgram(RobotState(1 at 1, E), listOf(TurnRight, Forward)),
                    RobotProgram(RobotState(0 at 3, W), emptyList()),
                ),
            )
        }

        test("a grid with no robots is a mission with no robots") {
            parser.parse("5 3\n") shouldBe Mission(GridBounds(0 at 0, 5 at 3), emptyList())
        }

        test("Windows line endings are accepted") {
            parser.parse("5 3\r\n1 1 E\r\nRF\r\n").robots.size shouldBe 1
        }

        withData(
            nameFn = { (input, error) -> "'${input.replace("\n", "\\n")}' fails with '$error'" },
            "" to "Line 1:",
            "5 3\n1 1 E" to "Line 2: robot position has no instruction line",
            "5 3\n6 1 E\nF" to "Line 2: robot starts at 6 1, outside the grid",
            "5 3\n1 1 E\nF\n1 1 Q\nF" to "Line 4:",
            "5 3\n1 1 E\nFXF" to "Line 3:",
        ) { (input, error) ->
            shouldThrow<InputException> { parser.parse(input) }.message shouldStartWith error
        }
    }
})
