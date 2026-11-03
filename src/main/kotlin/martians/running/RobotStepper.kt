package martians.running

import martians.domain.Effect
import martians.domain.Environment
import martians.domain.RobotProgram
import martians.domain.RobotState

/** Steps a single robot through its program, one command at a time. Used by every mission runner. */
object RobotStepper {
    /**
     * Executes the program's commands in order against [environment], passing each effect to [apply], in order, as
     * soon as it occurs, and returns the robot's final state. Once the robot is lost, remaining commands are not
     * executed.
     */
    fun run(program: RobotProgram, environment: Environment, apply: (Effect) -> Unit): RobotState {
        var robot = program.start
        for (command in program.commands) {
            if (robot.isLost) break
            val outcome = command.execute(robot, environment)
            outcome.effects.forEach(apply)
            robot = outcome.robot
        }
        return robot
    }
}
