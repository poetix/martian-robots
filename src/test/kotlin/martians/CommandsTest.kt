package martians

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import martians.domain.Orientation.E
import martians.domain.Orientation.N
import martians.domain.Orientation.W

class CommandsTest : FunSpec({

    context("turning") {
        test("left changes orientation only, even at the edge of the grid") {
            TurnLeft.execute(robot(5 at 3, N), grid5x3.withoutScent()) shouldBe robot(5 at 3, W).withoutEffects()
        }

        test("right changes orientation only, even at the edge of the grid") {
            TurnRight.execute(robot(5 at 3, N), grid5x3.withoutScent()) shouldBe robot(5 at 3, E).withoutEffects()
        }
    }

    context("forward") {
        test("onto a safe square moves the robot") {
            Forward.execute(robot(2 at 2, N), grid5x3.withoutScent()) shouldBe robot(2 at 3, N).withoutEffects()
        }

        test("onto a safe square moves the robot even from a scented point") {
            Forward.execute(robot(2 at 2, N), grid5x3.withScent(2 at 2)) shouldBe robot(2 at 3, N).withoutEffects()
        }

        test("onto an unsafe square loses the robot where it stood, leaving a scent there") {
            Forward.execute(robot(2 at 3, N), grid5x3.withoutScent()) shouldBe lost(2 at 3, N).leavingScent(2 at 3)
        }

        test("onto an unsafe square from a scented point is ignored") {
            Forward.execute(robot(2 at 3, N), grid5x3.withScent(2 at 3)) shouldBe robot(2 at 3, N).withoutEffects()
        }

        test("onto an unsafe square is not stopped by a scent on a neighbouring point") {
            Forward.execute(robot(3 at 3, N), grid5x3.withScent(2 at 3)) shouldBe lost(3 at 3, N).leavingScent(3 at 3)
        }
    }
})
