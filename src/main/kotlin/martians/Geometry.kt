package martians

data class Position(val x: Int, val y: Int) {
    fun moved(orientation: Orientation): Position =
        Position(x + orientation.movesX, y + orientation.movesY)
}

/** Compass points, declared in clockwise order so that turning is a step through [entries]. */
enum class Orientation(val movesX: Int, val movesY: Int) {
    N(0, 1),
    E(1, 0),
    S(0, -1),
    W(-1, 0);

    fun left(): Orientation = entries[(ordinal + entries.size - 1) % entries.size]
    fun right(): Orientation = entries[(ordinal + 1) % entries.size]
}

/** Answers whether a robot can stand on a position without being lost. */
fun interface SafeSquares {
    fun isSafe(position: Position): Boolean
}

data class GridBounds(val bottomLeft: Position, val topRight: Position) : SafeSquares {
    init {
        require(bottomLeft.x <= topRight.x && bottomLeft.y <= topRight.y) {
            "Bottom-left $bottomLeft must not be above or right of top-right $topRight"
        }
    }

    override fun isSafe(position: Position) =
        position.x in bottomLeft.x..topRight.x && position.y in bottomLeft.y..topRight.y
}
