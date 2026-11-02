package martians

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import martians.Orientation.E
import martians.Orientation.N
import martians.Orientation.S
import martians.Orientation.W

class GeometryTest : FunSpec({

    context("turning left and right steps round the compass") {
        withData(
            nameFn = { (from, left, right) -> "$from: left is $left, right is $right" },
            Triple(N, W, E),
            Triple(E, N, S),
            Triple(S, E, W),
            Triple(W, S, N),
        ) { (from, left, right) ->
            from.left() shouldBe left
            from.right() shouldBe right
        }
    }

    context("moving one step in each orientation") {
        withData(
            nameFn = { (orientation, to) -> "$orientation moves to $to" },
            N to (2 at 2),
            E to (3 at 1),
            S to (2 at 0),
            W to (1 at 1),
        ) { (orientation, to) ->
            (2 at 1).moved(orientation) shouldBe to
        }
    }

    test("a moved robot keeps its orientation") {
        RobotState(2 at 1, E).moved() shouldBe RobotState(3 at 1, E)
    }

    test("a turned robot keeps its position") {
        RobotState(2 at 1, E).turnedLeft() shouldBe RobotState(2 at 1, N)
        RobotState(2 at 1, E).turnedRight() shouldBe RobotState(2 at 1, S)
    }

    context("grid bounds include their edges and exclude everything beyond") {
        withData(
            nameFn = { (position, safe) -> "${position.x},${position.y} is ${if (safe) "safe" else "unsafe"}" },
            (0 at 0) to true,
            (5 at 3) to true,
            (2 at 1) to true,
            (-1 at 1) to false,
            (6 at 1) to false,
            (2 at -1) to false,
            (2 at 4) to false,
        ) { (position, safe) ->
            grid5x3.isSafe(position) shouldBe safe
        }
    }

    test("grid bounds reject a top-right below or left of bottom-left") {
        shouldThrow<IllegalArgumentException> { GridBounds(2 at 2, 1 at 3) }
        shouldThrow<IllegalArgumentException> { GridBounds(2 at 2, 3 at 1) }
    }
})
