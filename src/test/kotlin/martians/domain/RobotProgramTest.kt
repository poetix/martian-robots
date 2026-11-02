package martians.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import martians.at
import martians.domain.Orientation.N

class RobotProgramTest : FunSpec({

    val start = RobotState(1 at 1, N)

    /** An environment where everything is safe and scents are whatever has been applied so far. */
    class Recording : Environment {
        val effects = mutableListOf<Effect>()
        override fun isSafe(position: Position) = true
        override fun hasScent(position: Position) = LeaveScent(position) in effects
    }

    test("each effect is applied before the next command executes") {
        val leaveScent = Command { robot, _ -> Outcome(robot, listOf(LeaveScent(robot.position))) }
        val loseIfScented = Command { robot, environment -> Outcome(robot.copy(isLost = environment.hasScent(robot.position))) }
        val environment = Recording()

        RobotProgram(start, listOf(leaveScent, loseIfScented)).run(environment, environment.effects::add).isLost shouldBe true
    }

    test("several effects from one command are passed on in order") {
        val twoScents = Command { robot, _ -> Outcome(robot, listOf(LeaveScent(1 at 1), LeaveScent(2 at 2))) }
        val environment = Recording()

        RobotProgram(start, listOf(twoScents)).run(environment, environment.effects::add)
        environment.effects shouldBe listOf(LeaveScent(1 at 1), LeaveScent(2 at 2))
    }

    test("no command executes once the robot is lost") {
        val lose = Command { robot, _ -> Outcome(robot.copy(isLost = true)) }
        val fail = Command { _, _ -> error("executed after loss") }
        val environment = Recording()

        RobotProgram(start, listOf(lose, fail)).run(environment, environment.effects::add) shouldBe start.copy(isLost = true)
    }
})
