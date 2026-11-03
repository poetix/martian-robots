package martians.domain

/**
 * A grid point. North is the direction of increasing [y], east of increasing [x].
 *
 * @property x the east-west coordinate.
 * @property y the north-south coordinate.
 */
data class Position(val x: Int, val y: Int) {
    /** The neighbouring point one step in the direction of [orientation]. */
    fun moved(orientation: Orientation): Position =
        Position(x + orientation.movesX, y + orientation.movesY)
}

/**
 * Compass points, declared in clockwise order so that turning is a step through [entries].
 *
 * @property movesX the change in x when moving one step this way.
 * @property movesY the change in y when moving one step this way.
 */
enum class Orientation(val movesX: Int, val movesY: Int) {
    N(0, 1),
    E(1, 0),
    S(0, -1),
    W(-1, 0);

    /** The orientation 90 degrees anticlockwise from this one. */
    fun left(): Orientation = entries[(ordinal + entries.size - 1) % entries.size]
    /** The orientation 90 degrees clockwise from this one. */
    fun right(): Orientation = entries[(ordinal + 1) % entries.size]
}

/** Answers whether a robot can stand on a position without being lost. */
fun interface SafeSquares {
    /** Whether a robot can stand on [position] without being lost. */
    fun isSafe(position: Position): Boolean
}

/**
 * A rectangular grid, including its edges. Points outside it are unsafe.
 *
 * @property bottomLeft the lowest x and y on the grid.
 * @property topRight the highest x and y on the grid; must not be below or left of [bottomLeft].
 */
data class GridBounds(val bottomLeft: Position, val topRight: Position) : SafeSquares {
    init {
        require(bottomLeft.x <= topRight.x && bottomLeft.y <= topRight.y) {
            "Bottom-left $bottomLeft must not be above or right of top-right $topRight"
        }
    }

    override fun isSafe(position: Position) =
        position.x in bottomLeft.x..topRight.x && position.y in bottomLeft.y..topRight.y
}
