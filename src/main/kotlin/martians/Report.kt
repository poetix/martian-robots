package martians

fun RobotState.report(): String = "${position.x} ${position.y} $orientation" + if (isLost) " LOST" else ""
