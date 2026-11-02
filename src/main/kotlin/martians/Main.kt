package martians

import java.io.InputStream
import java.io.PrintStream
import kotlin.system.exitProcess

fun main() {
    exitProcess(runCli(System.`in`, System.out, System.err))
}

/**
 * Streams [input] line by line, writing each robot's report to [out] as soon as it is known, and returns the process
 * exit status. On invalid input, robots before the bad line have already been reported.
 */
fun runCli(input: InputStream, out: PrintStream, err: PrintStream): Int =
    try {
        input.bufferedReader().useLines { lines -> MartianRobots.run(lines, emit = out::println) }
        0
    } catch (e: InputException) {
        err.println("Invalid input: ${e.message}")
        1
    }
