# Martian Robots

Robots move around a rectangular grid on Mars following `L`, `R` and `F`
instructions. A robot that moves off the grid is lost, and leaves a scent that
stops later robots falling off at the same point. This program reads a grid and
a list of robots, runs the robots one after another, and prints where each one
ends up.

Kotlin 2.4 on JVM 25, built with Gradle and tested with Kotest.

## Building and running

You need a JDK (17 or later) to start Gradle. Gradle downloads JDK 25 itself
if it is not installed.

```sh
./gradlew build                      # compile and run the tests
./gradlew -q run < input.txt         # run on a file
printf '5 3\n1 1 E\nRFRFRFRF\n' | ./gradlew -q run
```

The program reads standard input and writes one line per robot to standard
output. The sample input from the brief gives:

```
1 1 E
3 3 N LOST
2 3 S
```

If the input is invalid, the error goes to standard error with its line number,
for example `Invalid input: Line 2: 'Q' is not an orientation; expected one of
N, E, S, W`, and the exit status is 1. Input is streamed, so robots before the
bad line have already been reported.

To run without Gradle, build a distribution and run its script with
`JAVA_HOME` set to a JDK 25:

```sh
./gradlew installDist
build/install/martian-robots/bin/martian-robots < input.txt
```

### Tests and other checks

| Command | What it does |
|---|---|
| `./gradlew test` | Unit, integration, CLI and architecture tests |
| `./gradlew pitest` | Mutation testing with PIT; report in `build/reports/pitest/index.html` |
| `./gradlew benchmarkScenarioTest` | Checks the large benchmark scenario behaves correctly |
| `./gradlew jmh` | Runs the JMH benchmark (after `benchmarkScenarioTest`) |

To run the benchmark with profilers, use the instructions in
[benchmark 0001](docs/benchmarks/0001-vast-scenario.md#how-to-run).

## Documentation

- [`docs/adrs/`](docs/adrs/): architecture decision records. Each decision
  ends with a one-paragraph Y-statement: the choice, the alternatives rejected
  and the trade-off accepted.
  - [0001 Overall design](docs/adrs/0001-overall-design.md): the original
    design, including parsing, validation and input limits. Some sections are
    superseded by 0002.
  - [0002 Commands address robots, not the world](docs/adrs/0002-robot-addressed-commands.md):
    commands, effects and environments, and why robots could run in parallel.
  - [0003 Package structure](docs/adrs/0003-package-structure.md): the
    packages and the dependency rules between them.
- [`docs/benchmarks/0001-vast-scenario.md`](docs/benchmarks/0001-vast-scenario.md):
  benchmark and profile of a very large field with 100,000 robots, including
  the experiments tried and the recommendations made.

## Structure

| Package | Responsibility |
|---|---|
| `martians` | `Main` is the command-line entry point. `MartianRobots` is the one place that puts the parser, standard commands, runner and output together. |
| `martians.domain` | The model: `Position`, `Orientation`, `GridBounds`, `RobotState`, `RobotProgram`, and the `Command`, `Outcome`, `Effect` and `Environment` types. It depends on nothing else in the project. |
| `martians.commands` | The instructions: `TurnLeft`, `TurnRight`, `Forward`, and `standardCommands`, which maps letters to commands. |
| `martians.parsing` | `InputParser` streams robots from lines of input; `LineParser` parses one line at a time and puts the line number in every error. |
| `martians.running` | `RobotStepper` steps one robot through its commands. `SequentialMissionRunner` runs robots one after another. `Surface` holds the scents left so far. |

How a run works:

1. `InputParser` reads the grid, then hands over the robots as a lazy sequence
   of `RobotProgram`s.
2. `SequentialMissionRunner` runs each program against one `Surface`, using
   `RobotStepper`.
3. Each command takes a robot and its `Environment`, which says whether a
   square is safe and whether a point has a scent. It returns an `Outcome`:
   the new robot state, plus any `Effect`s such as `LeaveScent`. Commands never
   change anything themselves; the runner applies their effects to the
   `Surface`.
4. Each robot's final state is printed as soon as it is known.

The main extension points:

- **New command:** implement `Command` and add it to `standardCommands`.
- **New terrain:** a `SafeSquares` other than `GridBounds`.
- **New way of running robots,** such as in parallel: a sibling of
  `SequentialMissionRunner`.

`ArchitectureTest` checks the dependency rules between packages.

## Assumptions

- **Scent is per point, not per direction.** A scent stops a later robot
  falling off from that point in any direction, including both edges at a
  corner. The brief says "the same grid point"; the sample does not
  distinguish.
- **An ignored move changes nothing.** The robot stays where it is, facing the
  same way, and carries on with its remaining instructions.
- **A lost robot executes nothing further.** It is reported at the last
  position it occupied on the grid, facing the direction it was moving, followed
  by `LOST`.
- **Robots run strictly in input order.** Each sees the scents left by every
  robot before it in the same input.
- **Robots must start on the grid.** A robot whose starting position is off
  the grid is an input error.
- **The brief's limits are enforced.** Coordinates must be whole numbers from
  0 to 50, and instruction lines at most 99 characters. They are checked by the
  parser only; the domain itself has no size limits.
- **Input is strict:**
  - letters must be upper case (`N`, `E`, `S`, `W`; `L`, `R`, `F`);
  - any run of spaces or tabs separates the fields on grid and position lines;
  - instruction lines may not contain spaces.
- **Blank lines:**
  - an empty instruction line means a robot with no instructions, which is
    reported where it started;
  - a blank line where a position is expected is an error.
- **Line endings:** a final line terminator does not start an extra line, and
  `\n` and `\r\n` are both accepted.
- **The first error stops the run.** Earlier robots have already been reported;
  no later robot is.
- **A grid of `0 0` is valid.** It is a single point, from which any forward
  move is off the grid.
- **A grid with no robots produces no output.**

## Further work

The design is built to absorb change inside a robot's step: new commands,
avoiding scented squares, scent that fades over time, robots blocking each
other. I would look at these next, roughly in order of how much they would
change:

- **Let the terrain decide where a step lands.** Movement is currently fixed
  in `Position.moved`, and the terrain only answers "safe or not". Wrapping
  edges, a cube-shaped world or sliding on ice would need
  `Forward` to ask the terrain for the next position and orientation instead.
- **A parallel runner.** Robots run in parallel see scents in whatever order
  they are laid, which breaks the brief's strict ordering. To keep the brief's
  results:
  - run every robot in parallel with no scents;
  - then go through them in input order, re-running only those that met a
    scent left by an earlier robot.

  `RobotStepper` and `Surface` already support this (see
  [benchmark 0001](docs/benchmarks/0001-vast-scenario.md#recommendations)).
- **Robots moving together, one instruction per tick,** with collisions. This
  needs a runner that interleaves robots tick by tick. Effects are returned as
  data, so a tick's effects can be gathered and conflicts resolved before
  applying them.
- **Recording each step, not just the final state,** for questions such as
  "how many squares were visited". `RobotStepper` would yield every
  intermediate state.
- **Very large inputs.** Changes that may be needed:
  - `Long` coordinates;
  - detecting cycles to skip ahead through heavily repeated instructions;
  - a bitset-backed terrain for large maps.
- **A richer instruction format,** such as `F12` or repeated groups. This needs
  a tokenizer in `LineParser`; `Command` can stay as it is.
- **Output formatting outside the domain.** This would only matter if a second
  output format were needed.
