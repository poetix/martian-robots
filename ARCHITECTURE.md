# Architecture

The decisions that shape this code, what I rejected, and what each one costs.
The packages and how a run works are described in the
[README](README.md#structure).

## Commands are pure functions

A `Command` takes a `RobotState` and an `Environment` and returns an `Outcome`:
the robot as it becomes, plus any effects on the world. `TurnLeft`, `TurnRight`
and `Forward` each implement it, and a new command is a new implementation.

I rejected a `when` over an instruction enum, because every new command would
edit that central `when`. I also rejected mutating a robot in place: it hides
state, and tests can no longer compare a robot before and after a command.

## Commands describe their effects; the runner applies them

A command that changes the world returns the change as data. Today the only
`Effect` is `LeaveScent`. The runner applies effects to the `Surface`, which
holds the scents.

I rejected a mutating `leaveScent()` on the environment. Commands would no
longer be pure, and tests would have to inspect the environment instead of
checking a returned value. The cost is an extra hop, and a sealed type with one
case, for what is today a single kind of change.

## Instruction letters resolve through an injected function

The parser takes a `(Char) -> Command?` and knows nothing about which commands
exist. `MartianRobots` supplies `standardCommands::get`. Adding a command means
writing it and adding one entry to `standardCommands`; the parser and runner do
not change.

I rejected a `when` in the parser, and a map parameter defaulting to the
standard commands, which would tie the parser to them. The cost is that an
unknown letter's error names the letter but cannot list the valid ones.

## Loss is checked once, in `RobotStepper`

`RobotStepper` stops executing commands once the robot is lost. No command
checks for loss itself, so a new command cannot forget to.

I rejected checks in each command for that reason. I also rejected a sealed
`Active`/`Lost` robot type: it adds ceremony to every command for a rule already
enforced in one place. The cost is that `isLost` is a plain flag that a command
could, in principle, clear.

## Safety is a question for the terrain

`Forward` asks `SafeSquares.isSafe(position)` and never mentions grid edges.
`GridBounds` is the only implementation. Holes or irregular shapes would be a
new `SafeSquares`, with no change to `Forward`.

The cost is that terrain needing more than a point to decide, such as an edge
that is unsafe in one direction only, would widen the interface.

## The brief's limits belong to the parser

The parser enforces the coordinate limit of 50 and the instruction limit of 99
characters. The domain types have no limits. The limits describe what Earth
sends, not how robots behave on Mars: no rule of movement, loss or scent
depends on them.

## Scent is per point

A scent stops a later robot falling off from that point in any direction. The
reasoning is under [Assumptions](README.md#assumptions).

## Parsing streams programs; the runner threads scents between them

The parser turns each robot's two lines into a `RobotProgram`, a start state and
a list of commands, and knows nothing about scent. The runner carries scents
from each robot to the next. So the parser can be tested without running
robots, and scents flow through one place.

`InputParser.parse(lines, consume)` passes the grid and a lazy, single-use
sequence of programs to a consumer, rather than returning the sequence. The
sequence reads the caller's input as it goes, so it must not outlive the reader
behind it; handing it to a consumer keeps its use inside the call. This is a
convention, not a guarantee, since a consumer could still return the sequence.
Each report is printed before the next robot is read, so memory does not grow
with the input. The cost is that an input error can surface after earlier
robots have been reported.

## Package dependencies

Which packages may depend on which is defined by `ArchitectureTest`; that test
is the documentation.
