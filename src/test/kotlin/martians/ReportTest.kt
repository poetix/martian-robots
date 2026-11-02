package martians

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import martians.Orientation.E
import martians.Orientation.N

class ReportTest : FunSpec({

    test("a robot on the grid reports its position and orientation") {
        RobotState(1 at 1, E).report() shouldBe "1 1 E"
    }

    test("a lost robot reports its last position and orientation followed by LOST") {
        RobotState(3 at 3, N, isLost = true).report() shouldBe "3 3 N LOST"
    }
})
