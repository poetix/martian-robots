# ADR 0001 — Martian Robots overall design

- **Status:** Accepted
- **Date:** 2026-11-02
- **Scope:** Shape of the domain model, command execution, input parsing and
  output reporting for the Martian Robots exercise. Each decision below closes
  with a Y-statement summarising the choice and its trade-off.

## Context

Robots move over a bounded rectangular grid, driven by strings of single-letter
instructions (`L`, `R`, `F`). A robot that moves off the grid is lost, and
leaves a "scent" at its last position. A later robot is not allowed to fall off
from a scented point. Robots run one after another, and scents carry over from
each robot to the next.

The brief signals that **more command types may be needed in future**. We also
treat the exercise like an Advent of Code puzzle, where part two often changes
the rules or scales up the input. Likely changes include:

- new commands;
- squares that are unsafe for reasons other than the grid edge;
- scent rules that leave many more scents;
- much larger grids.

The design should absorb those changes without being rebuilt, but should not
build any of them ahead of need.

Constraints:

- Kotlin on JVM 25, Gradle, Kotest.
- Run from a fresh clone with only a JDK able to launch Gradle; the toolchain
  is provisioned automatically.
- Coordinates are at most 50; instruction strings are under 100 characters.

## Decision

### 1. The world is an immutable value; commands are functions over it

`WorldState(robot, safeSquares, scents)` holds everything a command can see or
change:

- `robot: RobotState(position, orientation, isLost)`;
- `safeSquares: SafeSquares`;
- `scents: PersistentSet<Position>`.

A `Command` is `fun interface Command { fun execute(world: WorldState): WorldState }`.
`TurnLeft`, `TurnRight` and `Forward` are `data object`s.

Most behaviour lives on the value types, so each command body is a line or two:

- `Position.moved(orientation)`;
- `Orientation.left()` and `right()`;
- `RobotState.moved()`, `turnedLeft()` and `turnedRight()`;
- `WorldState.updateRobot { ... }` for nested immutable updates.

Tests state behaviour as *world before, instruction letters, world after*,
compared by value equality.

> In the context of **a command set expected to grow**, facing **the need to
> add commands that may touch the robot, the terrain or the scents**, we
> decided for **commands as pure `WorldState -> WorldState` functions** and
> neglected **commands that mutate a robot in place, or a `when` over an
> instruction enum**, to achieve **commands that are independent, composable
> and testable as before/after values**, accepting **one allocated world per
> command executed**.

### 2. Instruction letters map to commands through a replaceable table

`standardCommands: Map<Char, Command>` maps `L`, `R` and `F` to their commands.
`InputParser` takes the map as a constructor parameter, with the standard map
as its default. Adding a command type means writing a new `Command` and adding
an entry; neither the parser nor the runner changes.

> In the context of **parsing instruction strings**, facing **the brief's
> warning that new command types will arrive**, we decided for **a letter →
> `Command` map injected into the parser** and neglected **a hard-coded `when`
> in the parser, and a plugin or service-loader mechanism**, to achieve **new
> commands added in one place with no parser change**, accepting **that
> commands are limited to single letters until the input format changes**.

### 3. Losing a robot stops execution in one place

`WorldState.execute(commands)` folds the commands over the world and stops
applying them once `robot.isLost` is true. Individual commands do not check
whether the robot is lost.

> In the context of **executing a robot's instructions**, facing **the rule
> that a lost robot does nothing further**, we decided for **checking for loss
> once, in the fold that runs the commands**, and neglected **every command
> checking `isLost` itself, and a sealed `Active`/`Lost` robot type**, to
> achieve **new commands that cannot forget the rule**, accepting **a boolean
> flag on `RobotState` that a command could, in principle, clear**.

### 4. "Is this square safe?" is an interface, not the grid

`SafeSquares` is a one-method `fun interface`: `isSafe(position)`.
`GridBounds(bottomLeft, topRight)` is the only production implementation.
`Forward` asks `safeSquares.isSafe(target)` and never refers to grid edges
directly. A test shows a grid with a hole, built as a lambda, needs no change
to `Forward`.

> In the context of **deciding whether a move loses the robot**, facing
> **terrain rules that may change (holes, irregular shapes, very large
> worlds)**, we decided for **a `SafeSquares` predicate with `GridBounds` as
> its first implementation** and neglected **bounds checks inside `Forward`,
> and a materialised grid of cells**, to achieve **new terrain without changing
> any command, at O(1) memory for a rectangle of any size**, accepting **that a
> robot's loss is defined only by the predicate, so terrain needing more
> context (e.g. per-direction edges) would widen the interface**.

### 5. Scents are a persistent set

`scents` is a `PersistentSet<Position>` from `kotlinx-collections-immutable`,
created with `persistentHashSetOf()`. The library's default `persistentSetOf()`
keeps insertion order, which we never use, and
[benchmark 0001](../benchmarks/0001-vast-scenario.md) measured it as about 7%
slower and 16% more allocation-heavy.
`Forward` records a scent with `scents.adding(position)`, which costs O(log n)
and shares structure with the previous world. We call `adding` explicitly.
Without an extra import, `scents + position` resolves to the standard library's
`plus`, which copies the whole set into a plain `Set`; the compiler catches the
type mismatch, but `adding` makes the intent plain.

Under the original rules scents are few: at most one per lost robot, and only
on edge points. The choice is driven by the plausible follow-up rule "leave a
scent when your move would take you onto a scented position" on a large grid.
That rule could leave a scent on almost every move.

> In the context of **scents carried between robots and across worlds**,
> facing **a possible rule change that leaves a scent on most moves of a large
> grid**, we decided for **a persistent hash set (`kotlinx-collections-immutable`)**
> and neglected **a stdlib read-only `Set` (O(n) copy per scent) and a shared
> `MutableSet` (breaks value semantics and before/after testing)**, to achieve
> **O(log n) scent updates while worlds stay immutable values**, accepting
> **one small runtime dependency**.

### 6. A scent blocks falling off from that point in any direction

The brief says a scent "prohibits future robots from dropping off the world at
the same grid point". We read this literally: a scent at a corner stops a robot
falling off either outward edge, not only the edge the first robot left by.
The sample input does not test the difference.

> In the context of **interpreting scent**, facing **an ambiguity at corners
> that the sample does not settle**, we decided for **scent keyed by grid point
> alone** and neglected **scent keyed by point and direction**, to achieve
> **the reading closest to the brief's wording and the simplest
> representation**, accepting **that a different reading would change
> `scents` to hold (position, orientation) pairs**.

### 7. Parsing streams programs; the runner assembles worlds

`InputParser.parse(lines: Sequence<String>, consume)` reads the grid from the
first line. It then calls `consume(bounds, programs)` and returns its result.
`programs` is a lazy, single-use `Sequence<RobotProgram>`: a robot's two lines
are read and parsed only when the consumer asks for that robot. Each robot is a
`RobotProgram(start: RobotState, commands: List<Command>)`.

A pair of input lines cannot produce a `WorldState` on its own, because the
world also needs the bounds and the scents left by earlier robots.
`MartianRobots.execute` lazily builds each world from the robot's program, the
bounds and the scents so far. It runs the world and passes the resulting scents
to the next robot. `RobotState.report()` formats the result as `x y O` or
`x y O LOST`.

The command-line program streams standard input through all three stages. Each
report is printed before the next robot's lines are read, so memory use does
not grow with the number of robots. Five million robots run in a 32 MB heap.
`InputParser.parse(input: String): Mission` and
`MartianRobots.run(input: String): String` read everything into memory, for
tests and small inputs.

> In the context of **turning text into worlds**, facing **the fact that a
> robot's world depends on the robots before it**, we decided for **the parser
> producing world-independent `RobotProgram`s and the runner threading scents
> between them**, and neglected **the parser building `WorldState`s directly**,
> to achieve **a parser that can be tested without running robots, and one
> place where scents flow from robot to robot**, accepting **a small
> `Mission`/`RobotProgram` layer between parsing and execution**.

> In the context of **inputs that may hold very many robots**, facing **memory
> that grows with the input if it is read whole**, we decided for **the parser
> handing a lazy, single-use sequence of programs to a caller-supplied
> consumer** and neglected **returning a fully built `Mission`, and returning
> a lazy sequence that outlives the parse call**, to achieve **constant-memory
> streaming from input lines to output reports, with the consumer deciding
> whether to stream or collect**, accepting **that input errors surface partway
> through consumption rather than before any robot runs**.

### 8. Invalid input stops the run, with a line number

The parser rejects the following with an `InputException` whose message starts
`Line N:`:

- wrong field counts;
- non-numeric coordinates, or coordinates outside 0..50;
- lower-case or unknown orientation and instruction letters;
- instruction lines of 100 characters or more;
- robots starting on an unsafe square;
- a position line with no instruction line after it.

`main` prints the message to stderr and exits with status 1. Because input is
streamed (§7), robots before the bad line have already been reported to stdout
by then. Nothing after it is reported. An empty instruction line is a robot
with no instructions. A final line terminator does not start another line, so
`...\n1 1 E\n` is a missing instruction line, not an empty one. Both `\n` and
`\r\n` are accepted.

> In the context of **input from Earth**, facing **malformed or out-of-range
> lines**, we decided for **strict validation that stops the run with a
> line-numbered message and a failing exit status** and neglected **skipping
> bad robots, and accepting lower-case letters or out-of-range values**, to
> achieve **no silent misbehaviour and errors that point at the offending
> line**, accepting **that one bad robot stops the robots that follow it from
> being reported, and that a failed run may already have printed some
> reports**.

### 9. Size limits belong to the input format, not the domain

The brief caps coordinates at 50 and instruction strings at under 100
characters. `InputParser` enforces both, as `MAX_COORDINATE = 50` and
`MAX_INSTRUCTION_LENGTH = 99`. The domain types (`Position`, `GridBounds`,
`RobotState`, `RobotProgram`, `WorldState`) have no size limits and do not
check them. We could instead have made the domain refuse to represent anything
larger, for example by having `RobotProgram` or `GridBounds` reject such values
when constructed. We chose not to.

The limits describe what Earth will send, not how robots behave on Mars. No
rule of movement, loss or scent depends on 50 or 99. Keeping the domain
unbounded is what lets [benchmark 0001](../benchmarks/0001-vast-scenario.md)
build a field a billion points across directly. It also leaves room for a
future rule change that raises or removes the limits.

> In the context of **the brief's input limits (coordinates ≤ 50, instructions
> < 100 characters)**, facing **the choice of where to enforce them**, we
> decided for **checking them in `InputParser` only** and neglected **refusing
> to construct `GridBounds`, `Position` or `RobotProgram` values beyond them**,
> to achieve **a domain that models movement, loss and scent at any size, with
> the limits kept alongside the input format they describe**, accepting **that
> code constructing domain objects directly, such as tests and benchmarks, is
> not held to the brief's limits**.

## Consequences

**Positive**

- A new command is a new `Command` and one map entry. New terrain is a new
  `SafeSquares`. Neither touches the existing commands.
- Every rule from the brief is tested as a before/after world, with no file
  I/O or multi-robot setup needed to exercise scent.
- Scent updates stay cheap if scents become numerous.

**Negative / costs**

- One `WorldState` is allocated per command executed. This is negligible at
  the brief's sizes and cheap at any realistic size.
- A runtime dependency on `kotlinx-collections-immutable`.
- `isLost` is a plain boolean, so the type system does not stop a command
  from moving a lost robot. Only the fold in `execute` enforces it.

**Deferred (intentional)**

- Multi-letter or parameterised commands: the input format has none.
- Per-direction scents, terrain beyond the rectangle, scent-propagation rules:
  the extension points exist, the rules do not.

## Alternatives considered

- **Mutable robot plus a mutable scent set, updated in place.** This needs the
  least code, but commands become procedures with hidden shared state. Tests
  then have to snapshot state by hand, and the "world before" can change
  under a test.
- **A `when` over an `Instruction` enum in a single interpreter.** This is
  compact today, but every new command edits the central `when` and the
  parser.
- **A sealed `Robot` type (`Active` / `Lost`).** This would stop a lost robot
  being moved at compile time. It costs more ceremony in every command for a
  rule already enforced in one place.
