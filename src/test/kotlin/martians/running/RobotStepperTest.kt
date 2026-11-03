package martians.running

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import martians.at
import martians.domain.Command
import martians.domain.Effect
import martians.domain.Environment
import martians.domain.LeaveScent
import martians.domain.Orientation.N
import martians.domain.Outcome
import martians.domain.Position
import martians.domain.RobotProgram
import martians.domain.RobotState
import martians.lost
import martians.robot

class RobotStepperTest : FunSpec({

    val start = robot(1 at 1, N)

    /** An environment where everything is safe and scents are whatever has been applied so far. */
    class Recording : Environment {
        val effects = mutableListOf<Effect>()
        override fun isSafe(position: Position) = true
        override fun hasScent(position: Position) = LeaveScent(position) in effects
    }

    fun run(vararg commands: Command, environment: Environment = Recording()): RobotState =
        RobotStepper.run(RobotProgram(start, commands.toList()), environment) {
            (environment as Recording).effects += it
        }

    test("each effect is applied before the next command executes") {
        val leaveScent = Command { robot, _ -> Outcome(robot, listOf(LeaveScent(robot.position))) }
        val loseIfScented = Command { robot, environment ->
            Outcome(robot.copy(isLost = environment.hasScent(robot.position)))
        }

        run(leaveScent, loseIfScented) shouldBe lost(1 at 1, N)
    }

    test("several effects from one command are passed on in order") {
        val twoScents = Command { robot, _ -> Outcome(robot, listOf(LeaveScent(1 at 1), LeaveScent(2 at 2))) }
        val environment = Recording()

        run(twoScents, environment = environment)
        environment.effects shouldBe listOf(LeaveScent(1 at 1), LeaveScent(2 at 2))
    }

    test("no command executes once the robot is lost") {
        val lose = Command { robot, _ -> Outcome(robot.copy(isLost = true)) }
        val fail = Command { _, _ -> error("executed after loss") }

        run(lose, fail) shouldBe lost(1 at 1, N)
    }

    test("a program with no commands leaves the robot where it started") {
        run() shouldBe start
    }
})
