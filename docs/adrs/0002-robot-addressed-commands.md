# ADR 0002 — Commands address robots, not the world

- **Status:** Accepted
- **Date:** 2026-11-02
- **Scope:** The shape of `Command`, how a robot perceives and changes its
  surroundings, and where scent is stored. Supersedes
  [ADR 0001](0001-overall-design.md) §1 (the world is an immutable value) and
  §5 (scents are a persistent set), and amends §3 (losing a robot stops
  execution in one place).

## Context

Under ADR 0001, a command took the whole `WorldState` and returned a new one:
the robot, the terrain, and every scent left so far. That fits the brief, where
robots run strictly one after another. But it ties every robot's step to one
shared value. Running robots in parallel would mean contending for each
step's update of that value, even though robots interact only through scent.

We want running robots in parallel to be easy to add if the rules ask for it,
without changing any robot or command logic. Only the code that runs programs
should differ between a sequential and a parallel runner. We are not building a
parallel runner now.

Two things a robot needs from the world: whether a square is safe, and whether
a point has a scent. One thing it does to the world: leave a scent.

## Decision

### 1. A command maps a robot and its environment to an outcome

```kotlin
fun interface Command {
    fun execute(robot: RobotState, environment: Environment): Outcome
}

data class Outcome(val robot: RobotState, val effects: List<Effect> = emptyList())
```

`Environment` is read-only: `isSafe(position)` and `hasScent(position)`.
Commands are still pure functions. They read the environment, return the robot
as it becomes, and *describe* their changes to the world as effects instead of
making them.

> In the context of **robots that may one day run in parallel**, facing
> **commands that operate on one shared world value**, we decided for
> **commands addressed to a single robot, reading a read-only `Environment` and
> returning an `Outcome` of new robot state plus effects**, and neglected
> **commands over the whole `WorldState` (ADR 0001 §1)**, to achieve **robot
> and command logic that is the same whether robots run one after another or
> concurrently**, accepting **that a command's changes to the world are
> indirect, described as data and applied by someone else**.

### 2. Effects are a closed set of data, applied by the runner

`Effect` is a sealed interface; today its only case is `LeaveScent(position)`.
An outcome carries a list of effects, so a future command can produce several,
or none.

> In the context of **commands that change the world**, facing **the need for
> runners with different storage and concurrency to apply those changes**, we
> decided for **a sealed `Effect` hierarchy returned in each `Outcome`** and
> neglected **commands calling mutating methods on the environment**, to
> achieve **commands that stay pure and testable as input → output, and
> runners free to apply effects however their storage requires**, accepting
> **a new `Effect` case each time a command needs a new kind of change, and an
> exhaustive `when` in each environment that applies them**.

### 3. One robot's run is shared by every runner

`RobotProgram.run(environment, apply): RobotState` executes the commands in
order. It passes each effect to `apply` as soon as it occurs, and stops once
the robot is lost. It is the only code that steps a robot. This keeps ADR 0001
§3's rule (loss is checked in one place) and moves it from `WorldState` to
`RobotProgram`.

### 4. The runner owns the scent store

`Surface(safeSquares, scents: MutableSet<Position>)` implements `Environment`
and applies effects to its scent set. The sequential runner,
`MartianRobots.execute`, uses one `Surface` with a `HashSet` for the whole
mission. Each robot runs against it in turn, so each sees the scents left by
those before it.

A parallel runner would construct the same `Surface` over
`ConcurrentHashMap.newKeySet()`, the set view of a `ConcurrentHashMap`. It
would run `RobotProgram.run` from several threads. Commands, `RobotProgram` and
`Surface` would not change.

> In the context of **storing scent**, facing **sequential and parallel runners
> with different concurrency needs**, we decided for **a mutable scent set owned
> by the runner and chosen by it** and neglected **a persistent set threaded
> through every command (ADR 0001 §5)**, to achieve **O(1) scent reads and
> writes with no per-step copying, and a store that can be swapped for a
> concurrent one**, accepting **that scents are no longer part of any value a
> test can compare before and after a whole mission**.

## Consequences

**Positive**

- Commands are tested as `execute(robot, environment) shouldBe Outcome(...)`,
  and programs as robot before, robot after and effects produced.
- No `WorldState` is allocated per command. In
  [benchmark 0001](../benchmarks/0001-vast-scenario.md)'s scenario, the 1G
  field dropped from about 86 ms to 71 ms and allocation fell by about 10%. The
  1k field was within noise (94 ± 8 ms against 96 ± 2 ms).
- `kotlinx-collections-immutable` is no longer needed and has been removed.

**Negative / costs**

- **Parallel running changes the rules, not just the speed.** The brief
  processes robots strictly in order, so a robot sees exactly the scents of the
  robots before it. Robots run concurrently on a shared concurrent set see
  whatever scents happen to have been laid when they look, so results would
  depend on timing. A parallel runner that must keep the brief's rules needs
  reconciliation, such as benchmark 0001 recommendation 2: run every robot in
  parallel, then re-run in order only the robots that met a scent.
  `RobotProgram.run` supports that, but the runner itself is not built.
- An exhaustive `when` over `Effect` in each environment that applies effects.

**Deferred (intentional)**

- Any parallel runner.
- Effects other than `LeaveScent`.

## Alternatives considered

- **Keep `WorldState` and parallelise around it.** Every robot step would
  produce a new world, so concurrent robots would contend on one reference, or
  need merging. That is the coupling this ADR removes.
- **Let commands mutate the environment directly** (for example
  `environment.leaveScent(position)`). This is simpler for one runner, but
  commands stop being pure, and tests must inspect an environment's state
  rather than a returned value.
- **A single nullable `effect` rather than a list.** That fits today's one
  effect per command, but a command producing two changes would need another
  type change.
