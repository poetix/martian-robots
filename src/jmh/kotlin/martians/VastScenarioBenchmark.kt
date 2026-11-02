package martians

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
open class VastScenarioBenchmark {

    /** 1,000 makes scents collide often; 1,000,000,000 makes them almost never collide. */
    @Param("1000", "1000000000")
    @JvmField
    var size = 0

    @Param("100000")
    @JvmField
    var robots = 0

    private lateinit var scenario: VastScenario

    @Setup
    fun setUp() {
        scenario = VastScenario(size, robots)
    }

    @Benchmark
    fun execute(): List<RobotState> = MartianRobots.execute(scenario.programs, scenario.safeSquares)
}
