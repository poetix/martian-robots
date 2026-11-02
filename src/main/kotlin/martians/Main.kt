package martians

fun main() {
    print(MartianRobots.run(generateSequence(::readlnOrNull).joinToString("\n")))
}
