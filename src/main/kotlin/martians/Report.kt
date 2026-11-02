package martians

import martians.domain.RobotState

fun RobotState.report(): String = "${position.x} ${position.y} $orientation" + if (isLost) " LOST" else ""
