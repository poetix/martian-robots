package martians

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MartianRobotsTest : FunSpec({

    fun run(input: String): String = buildList { MartianRobots.run(input.inputLines(), ::add) }.joinToString("\n")

    test("sample input produces sample output") {
        val input = """
            5 3
            1 1 E
            RFRFRFRF
            3 2 N
            FRRFLLFFRRFLL
            0 3 W
            LLFFFLFLFL
        """.trimIndent()

        val expected = """
            1 1 E
            3 3 N LOST
            2 3 S
        """.trimIndent()

        run(input) shouldBe expected
    }

    test("scents accumulate across robots") {
        val input = """
            5 3
            2 3 N
            F
            4 3 N
            F
            2 3 N
            FRFFLF
        """.trimIndent()

        val expected = """
            2 3 N LOST
            4 3 N LOST
            4 3 N
        """.trimIndent()

        run(input) shouldBe expected
    }

    test("stray whitespace and blank lines between robots do not change the output") {
        val input = "\n5 3  \n1 1 E\nRFRFRFRF \n\n  3 2 N\nFRRFLLFFRRFLL\n\n\n0 3 W\nLLFFFLFLFL\n \n"

        run(input) shouldBe "1 1 E\n3 3 N LOST\n2 3 S"
    }
})
