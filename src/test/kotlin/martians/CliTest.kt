package martians

import io.kotest.assertions.fail
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class CliTest : FunSpec({

    /** Runs the CLI on [input]; a successful run must write nothing to stderr, and a failed run must exit 1. */
    fun cli(input: String): CliResult {
        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()
        val status = runCli(input.byteInputStream(), PrintStream(out, true), PrintStream(err, true))
        return when (status) {
            0 -> CliResult.Success(out.toString()).also { err.toString() shouldBe "" }
            1 -> CliResult.Failure(out.toString(), err.toString())
            else -> fail("unexpected exit status $status")
        }
    }

    test("valid input prints one report line per robot and exits 0") {
        cli("5 3\n1 1 E\nRFRFRFRF\n3 2 N\nFRRFLLFFRRFLL\n") shouldBe
            CliResult.Success("1 1 E\n3 3 N LOST\n")
    }

    test("a grid with no robots prints nothing and exits 0") {
        cli("5 3\n") shouldBe CliResult.Success("")
    }

    test("robots before an invalid line are still reported before the error") {
        cli("5 3\n1 1 E\nRFRFRFRF\n1 1 Q\nF\n") shouldBe CliResult.Failure(
            "1 1 E\n",
            "Invalid input: Line 4: 'Q' is not an orientation; expected one of N, E, S, W\n",
        )
    }

    test("invalid input prints the error to stderr and exits 1") {
        cli("5 3\n9 9 N\nF\n") shouldBe
            CliResult.Failure("", "Invalid input: Line 2: robot starts at 9 9, outside the grid\n")
    }
})

private sealed interface CliResult {
    data class Success(val out: String) : CliResult
    data class Failure(val out: String, val err: String) : CliResult
}
