package martians

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class CliTest : FunSpec({

    data class Result(val status: Int, val out: String, val err: String)

    fun cli(input: String): Result {
        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()
        val status = runCli(input.byteInputStream(), PrintStream(out, true), PrintStream(err, true))
        return Result(status, out.toString(), err.toString())
    }

    test("valid input prints one report line per robot and exits 0") {
        cli("5 3\n1 1 E\nRFRFRFRF\n3 2 N\nFRRFLLFFRRFLL\n") shouldBe
            Result(0, "1 1 E\n3 3 N LOST\n", "")
    }

    test("a grid with no robots prints nothing and exits 0") {
        cli("5 3\n") shouldBe Result(0, "", "")
    }

    test("invalid input prints the error to stderr and exits 1") {
        cli("5 3\n9 9 N\nF\n") shouldBe
            Result(1, "", "Invalid input: Line 2: robot starts at 9 9, outside the grid\n")
    }
})
