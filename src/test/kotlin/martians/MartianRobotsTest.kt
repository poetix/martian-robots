package martians

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MartianRobotsTest : FunSpec({

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

        MartianRobots.run(input) shouldBe expected
    }
})
