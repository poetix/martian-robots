package martians.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import martians.at
import martians.domain.Orientation.E
import martians.domain.Orientation.N
import martians.domain.Orientation.S
import martians.lost
import martians.robot

class RobotStateTest : FunSpec({

    context("moving and turning") {
        test("a moved robot keeps its orientation") {
            robot(2 at 1, E).moved() shouldBe robot(3 at 1, E)
        }

        test("a turned robot keeps its position") {
            robot(2 at 1, E).turnedLeft() shouldBe robot(2 at 1, N)
            robot(2 at 1, E).turnedRight() shouldBe robot(2 at 1, S)
        }
    }

    context("reporting") {
        test("a robot on the grid reports its position and orientation") {
            robot(1 at 1, E).report() shouldBe "1 1 E"
        }

        test("a lost robot reports its last position and orientation followed by LOST") {
            lost(3 at 3, N).report() shouldBe "3 3 N LOST"
        }
    }
})
