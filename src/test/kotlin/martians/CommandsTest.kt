package martians

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import martians.domain.GridBounds
import martians.domain.Orientation.E
import martians.domain.Orientation.N
import martians.domain.Orientation.S
import martians.domain.Orientation.W
import martians.domain.SafeSquares

class CommandsTest : FunSpec({

    fun verify(transition: Transition) {
        transition.before.execute(commands(transition.letters)) shouldBe transition.after
    }

    context("turning") {
        withData(
            nameFn = Transition::name,
            Transition("left turns in place", world(1 at 1, N), "L", world(1 at 1, W)),
            Transition("right turns in place", world(1 at 1, N), "R", world(1 at 1, E)),
            Transition("four lefts make a full circle", world(1 at 1, S), "LLLL", world(1 at 1, S)),
            Transition("turning on an edge is safe", world(5 at 3, N), "RRR", world(5 at 3, W)),
        ) { verify(it) }
    }

    context("moving") {
        withData(
            nameFn = Transition::name,
            Transition("forward moves in the direction faced", world(2 at 1, E), "F", world(3 at 1, E)),
            Transition("robot can travel up to an edge", world(0 at 0, N), "FFF", world(0 at 3, N)),
            Transition("no commands leaves the world unchanged", world(2 at 2, S), "", world(2 at 2, S)),
            Transition(
                "commands combine",
                world(1 at 1, E), "RFRFRFRF",
                world(1 at 1, E),
            ),
            Transition(
                "largest grid can be crossed corner to corner",
                world(0 at 0, N, safeSquares = GridBounds(0 at 0, 50 at 50)), "F".repeat(50) + "R" + "F".repeat(50),
                world(50 at 50, E, safeSquares = GridBounds(0 at 0, 50 at 50)),
            ),
        ) { verify(it) }
    }

    context("getting lost") {
        withData(
            nameFn = Transition::name,
            Transition(
                "moving off an edge loses the robot and leaves a scent where it stood",
                world(2 at 3, N), "F",
                world(2 at 3, N, lost = true, scents = setOf(2 at 3)),
            ),
            Transition(
                "commands after being lost are not executed",
                world(0 at 1, W), "FRRFF",
                world(0 at 1, W, lost = true, scents = setOf(0 at 1)),
            ),
            Transition(
                "lost robot keeps the orientation it was lost in",
                world(2 at 2, E), "LFF",
                world(2 at 3, N, lost = true, scents = setOf(2 at 3)),
            ),
            Transition(
                "scents elsewhere on the edge give no protection",
                world(3 at 0, S, scents = setOf(2 at 0)), "F",
                world(3 at 0, S, lost = true, scents = setOf(2 at 0, 3 at 0)),
            ),
            Transition(
                "single-point grid loses a robot on any move",
                world(0 at 0, W, safeSquares = GridBounds(0 at 0, 0 at 0)), "F",
                world(0 at 0, W, lost = true, scents = setOf(0 at 0), safeSquares = GridBounds(0 at 0, 0 at 0)),
            ),
        ) { verify(it) }
    }

    context("scent") {
        withData(
            nameFn = Transition::name,
            Transition(
                "a move off the world from a scented point is ignored",
                world(2 at 3, N, scents = setOf(2 at 3)), "F",
                world(2 at 3, N, scents = setOf(2 at 3)),
            ),
            Transition(
                "robot carries on with remaining commands after the ignored move",
                world(2 at 3, N, scents = setOf(2 at 3)), "FRF",
                world(3 at 3, E, scents = setOf(2 at 3)),
            ),
            Transition(
                "scent does not hinder moves across or inward from the scented point",
                world(1 at 3, E, scents = setOf(2 at 3)), "FFRRFLF",
                world(2 at 2, S, scents = setOf(2 at 3)),
            ),
            Transition(
                "scent at a corner prevents falling off in either outward direction",
                world(5 at 3, N, scents = setOf(5 at 3)), "FRF",
                world(5 at 3, E, scents = setOf(5 at 3)),
            ),
        ) { verify(it) }
    }

    context("unsafe squares need not be off the grid") {
        val hole = 2 at 2
        val gridWithHole = SafeSquares { it != hole && grid5x3.isSafe(it) }

        withData(
            nameFn = Transition::name,
            Transition(
                "moving into a hole loses the robot and leaves a scent beside it",
                world(2 at 1, N, safeSquares = gridWithHole), "F",
                world(2 at 1, N, lost = true, scents = setOf(2 at 1), safeSquares = gridWithHole),
            ),
            Transition(
                "scent beside a hole stops later robots falling in",
                world(2 at 1, N, scents = setOf(2 at 1), safeSquares = gridWithHole), "F",
                world(2 at 1, N, scents = setOf(2 at 1), safeSquares = gridWithHole),
            ),
        ) { verify(it) }
    }
})
