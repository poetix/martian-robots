package martians

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import martians.domain.GridBounds
import martians.domain.LeaveScent
import martians.domain.Orientation.E
import martians.domain.Orientation.N
import martians.domain.Orientation.S
import martians.domain.Orientation.W
import martians.domain.Outcome
import martians.domain.SafeSquares

class CommandsTest : FunSpec({

    context("each command maps a robot and its environment to an outcome") {
        val surface = Surface(grid5x3, mutableSetOf(5 at 3))

        test("turning changes only orientation and has no effect") {
            TurnLeft.execute(robot(1 at 1, N), surface) shouldBe Outcome(robot(1 at 1, W))
            TurnRight.execute(robot(1 at 1, N), surface) shouldBe Outcome(robot(1 at 1, E))
        }

        test("forward onto a safe square moves the robot and has no effect") {
            Forward.execute(robot(2 at 1, E), surface) shouldBe Outcome(robot(3 at 1, E))
        }

        test("forward onto an unsafe square loses the robot and leaves a scent where it stood") {
            Forward.execute(robot(2 at 3, N), surface) shouldBe Outcome(lost(2 at 3, N), listOf(LeaveScent(2 at 3)))
        }

        test("forward onto an unsafe square from a scented point is ignored") {
            Forward.execute(robot(5 at 3, N), surface) shouldBe Outcome(robot(5 at 3, N))
        }
    }

    context("turning") {
        withData(
            nameFn = Transition::name,
            Transition("left turns in place", robot(1 at 1, N), "L", robot(1 at 1, W)),
            Transition("right turns in place", robot(1 at 1, N), "R", robot(1 at 1, E)),
            Transition("four lefts make a full circle", robot(1 at 1, S), "LLLL", robot(1 at 1, S)),
            Transition("turning on an edge is safe", robot(5 at 3, N), "RRR", robot(5 at 3, W)),
        ) { it.verify() }
    }

    context("moving") {
        withData(
            nameFn = Transition::name,
            Transition("forward moves in the direction faced", robot(2 at 1, E), "F", robot(3 at 1, E)),
            Transition("robot can travel up to an edge", robot(0 at 0, N), "FFF", robot(0 at 3, N)),
            Transition("no commands leaves the robot unchanged", robot(2 at 2, S), "", robot(2 at 2, S)),
            Transition("commands combine", robot(1 at 1, E), "RFRFRFRF", robot(1 at 1, E)),
            Transition(
                "largest grid can be crossed corner to corner",
                robot(0 at 0, N), "F".repeat(50) + "R" + "F".repeat(50), robot(50 at 50, E),
                safeSquares = GridBounds(0 at 0, 50 at 50),
            ),
        ) { it.verify() }
    }

    context("getting lost") {
        withData(
            nameFn = Transition::name,
            Transition(
                "moving off an edge loses the robot and leaves a scent where it stood",
                robot(2 at 3, N), "F", lost(2 at 3, N), effects = scentAt(2 at 3),
            ),
            Transition(
                "commands after being lost are not executed",
                robot(0 at 1, W), "FRRFF", lost(0 at 1, W), effects = scentAt(0 at 1),
            ),
            Transition(
                "lost robot keeps the orientation it was lost in",
                robot(2 at 2, E), "LFF", lost(2 at 3, N), effects = scentAt(2 at 3),
            ),
            Transition(
                "scents elsewhere on the edge give no protection",
                robot(3 at 0, S), "F", lost(3 at 0, S), effects = scentAt(3 at 0), scents = setOf(2 at 0),
            ),
            Transition(
                "single-point grid loses a robot on any move",
                robot(0 at 0, W), "F", lost(0 at 0, W), effects = scentAt(0 at 0),
                safeSquares = GridBounds(0 at 0, 0 at 0),
            ),
        ) { it.verify() }
    }

    context("scent") {
        withData(
            nameFn = Transition::name,
            Transition(
                "a move off the world from a scented point is ignored",
                robot(2 at 3, N), "F", robot(2 at 3, N), scents = setOf(2 at 3),
            ),
            Transition(
                "robot carries on with remaining commands after the ignored move",
                robot(2 at 3, N), "FRF", robot(3 at 3, E), scents = setOf(2 at 3),
            ),
            Transition(
                "scent does not hinder moves across or inward from the scented point",
                robot(1 at 3, E), "FFRRFLF", robot(2 at 2, S), scents = setOf(2 at 3),
            ),
            Transition(
                "scent at a corner prevents falling off in either outward direction",
                robot(5 at 3, N), "FRF", robot(5 at 3, E), scents = setOf(5 at 3),
            ),
        ) { it.verify() }
    }

    context("unsafe squares need not be off the grid") {
        val hole = 2 at 2
        val gridWithHole = SafeSquares { it != hole && grid5x3.isSafe(it) }

        withData(
            nameFn = Transition::name,
            Transition(
                "moving into a hole loses the robot and leaves a scent beside it",
                robot(2 at 1, N), "F", lost(2 at 1, N), effects = scentAt(2 at 1), safeSquares = gridWithHole,
            ),
            Transition(
                "scent beside a hole stops later robots falling in",
                robot(2 at 1, N), "F", robot(2 at 1, N), scents = setOf(2 at 1), safeSquares = gridWithHole,
            ),
        ) { it.verify() }
    }
})
