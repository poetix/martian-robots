package martians

import java.io.InputStream
import java.io.PrintStream
import kotlin.system.exitProcess

fun main() {
    exitProcess(runCli(System.`in`, System.out, System.err))
}

/** Reads the whole of [input], writes one report line per robot to [out], and returns the process exit status. */
fun runCli(input: InputStream, out: PrintStream, err: PrintStream): Int =
    try {
        val output = MartianRobots.run(input.bufferedReader().readText())
        if (output.isNotEmpty()) out.println(output)
        0
    } catch (e: InputException) {
        err.println("Invalid input: ${e.message}")
        1
    }
