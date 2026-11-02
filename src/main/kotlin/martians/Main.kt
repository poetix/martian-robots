package martians

import kotlin.system.exitProcess

fun main() {
    val input = System.`in`.bufferedReader().readText()
    try {
        val output = MartianRobots.run(input)
        if (output.isNotEmpty()) println(output)
    } catch (e: InputException) {
        System.err.println("Invalid input: ${e.message}")
        exitProcess(1)
    }
}
