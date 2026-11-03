package martians

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeUnique
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import martians.running.SequentialMissionRunner

class VastScenarioTest : FunSpec({

    val scenario = VastScenario(size = 1_000, robotCount = 10_000)
    val finished = SequentialMissionRunner.run(scenario.programs.asSequence(), scenario.safeSquares).toList()

    test("about one point in sixteen is a hole") {
        val sample = (0 until 400).flatMap { x -> (0 until 400).map { y -> x at y } }
        sample.count { !scenario.safeSquares.isSafe(it) }.toDouble() / sample.size shouldBe (1.0 / 16 plusOrMinus 0.005)
    }

    test("every robot ends on a safe square, lost or not") {
        finished.all { scenario.safeSquares.isSafe(it.position) } shouldBe true
    }

    test("no two robots are lost from the same point, because scent stops the second") {
        finished.filter { it.isLost }.map { it.position }.shouldBeUnique()
    }

    test("the same seed gives the same outcome") {
        val again = VastScenario(size = 1_000, robotCount = 10_000)
        SequentialMissionRunner.run(again.programs.asSequence(), again.safeSquares).toList() shouldBe finished
    }
})
