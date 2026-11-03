package martians.running

import martians.domain.RobotProgram
import martians.domain.RobotState
import martians.domain.SafeSquares

/** Runs robots strictly one after another, as the brief requires, so each sees the scents left by those before it. */
object SequentialMissionRunner {
    /** Lazily runs each of [programs] in turn on one [Surface] over [safeSquares], yielding each robot's final state. */
    fun run(programs: Sequence<RobotProgram>, safeSquares: SafeSquares): Sequence<RobotState> = sequence {
        val surface = Surface(safeSquares)
        for (program in programs) {
            yield(RobotStepper.run(program, surface, surface::apply))
        }
    }
}
