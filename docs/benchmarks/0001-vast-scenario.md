# Benchmark 0001: vast field with scattered holes

- **Date:** 2026-11-02
- **Code measured:** the version recorded in [ADR 0001](../adrs/0001-overall-design.md)
  (immutable `WorldState`, commands as functions, scents in a persistent set)
- **Machine:** Apple M3 (8 cores), macOS 14.5
- **JVM:** Temurin 25.0.4, default GC (G1), default heap
- **Harness:** JMH 1.37, 1 fork, 3 × 2 s warmup, 5 × 2 s measurement, average time

## Purpose

The brief's inputs are tiny: a grid of at most 50 × 50 and a handful of robots.
This benchmark asks how the design behaves well beyond that. The scale is much
larger, the terrain is irregular, and many more scents are left.

The benchmark should show where the time goes. It should also show which
changes would pay off if the rules or the size of the problem changed.

## Scenario

[`VastScenario`](../../src/test/kotlin/martians/VastScenario.kt) builds a
deterministic world from a seed:

- **Field:** a square `size` points across.
- **Holes:** a point is a hole when a SplitMix64-style mix of its coordinates,
  modulo 16, is 0. That makes about one point in 16 a hole, scattered rather
  than in lines. Holes are expressed purely as a `SafeSquares` predicate, so no
  production code knows about them.

  > **Later change:** the hole rule has since been simplified. A point is now a
  > hole when the low four bits of `Position.hashCode()` (`31 * x + y`) are
  > zero, which puts holes on regular diagonals at the same one-in-16 density.
  > Re-measured with streaming execution, the old rule gives 88.6 / 82.7 ms and
  > the new rule 95.9 / 85.9 ms (1k / 1G field), with about 7% more allocation.
  > The likely cause is that robots survive longer between diagonal holes, so
  > more commands run; this was not counted. Streaming made no measurable
  > difference. All figures below use the original rule, and the relative
  > results and recommendations are unaffected.
- **Robots:** `robotCount` robots. Each starts on a random safe point facing a
  random direction, with 1–99 instructions drawn uniformly from `L`, `R`, `F`.

[`VastScenarioTest`](../../src/test/kotlin/martians/VastScenarioTest.kt) checks
the generator and the runner together on a smaller instance (1,000 × 1,000,
10,000 robots):

- about 1/16 of points are holes;
- every robot ends on a safe square;
- no two robots are lost from the same point;
- the same seed gives the same outcome.

[`VastScenarioBenchmark`](../../src/jmh/kotlin/martians/VastScenarioBenchmark.kt)
times `MartianRobots.execute` over 100,000 robots at two field sizes:

| Field size | Commands generated | Commands executed | Robots lost | Moves ignored because of scent |
|---|---|---|---|---|
| 1,000 | 5.01M | 3.26M | 46,955 | 13,066 |
| 1,000,000,000 | 5.00M | 3.09M | 50,725 | 0 |

At 1,000 points across, robots often run into earlier robots' scents. At a
billion points across they never do. Fewer commands are executed than generated
because a robot stops at its first loss. About one forward move in 16 lands on
a hole, so roughly half the robots are lost.

## How to run

```sh
./gradlew jmhJar
java -jar build/libs/martian-robots-jmh.jar VastScenarioBenchmark -prof gc
```

Run the jar with a JDK 25 `java` (Gradle's toolchain JDK will do). For a
profile, add `-prof "jfr:dir=build/jfr"` and inspect the recording with
`jfr view hot-methods <file>` and `jfr view allocation-by-class <file>`.

## Baseline

| Field size | Time per run | Allocated per run |
|---|---|---|
| 1,000 | 96.0 ± 1.7 ms | 253 MB |
| 1,000,000,000 | 88.2 ± 8.3 ms | 240 MB |

That is about **29 ns and 78 bytes per command executed**, and an allocation
rate of about 2.5 GB/s. GC took about 2% of the measured time.

## Profile

JFR execution sampling (1,000 field):

- **58% of time in `WorldState.execute`.** The JIT compiles the commands, the
  `copy` calls and the fold into this one method, so it absorbs their cost.
- **About 15% in the persistent set.** This is spread across `TrieNode.put`,
  `containsKey` and related methods. The `TrieNode` comes from the *map*
  implementation: `persistentSetOf()` returns an insertion-ordered set, built
  on a persistent map plus `Links` that record the order.
- **Small shares elsewhere:** `Position.equals`, `Position.moved`, the
  `RobotState` constructor and `copy`, `GridBounds.isSafe` and
  `EnumEntriesList.getSize` (from `Orientation.left()` and `right()`).

JFR allocation sampling:

- **`WorldState` 21%, `RobotState` 19%, `Position` 7%.** These are one of each
  per command.
- **About 12% from the persistent set:** `TrieNode`, `Links`, `Object[]`.
- **`ConcurrentHashMap$ValueIterator` 37%.** This is a sampling artefact from
  JMH's own `BenchmarkHandler`, not from the code under test.

## Experiments

Each change was tried on its own, benchmarked, and reverted. Every row from B
onwards includes A.

| | Change | 1k field | 1G field | Allocated (1k) | Verdict |
|---|---|---|---|---|---|
| — | Baseline | 96.0 ms | 88.2 ms | 253 MB | |
| A | `persistentHashSetOf` instead of `persistentSetOf` | 88.0 ms | 83.0 ms | 213 MB (−16%) | **Adopt** |
| B | `when`-based `left()`/`right()` instead of indexing `entries` | 91.1 ms | 83.7 ms | 213 MB | No effect |
| C | `Position` as a `@JvmInline value class` packed into a `Long` | 92.1 ms | 79.3 ms | 223 MB | Worse allocation |
| D | Check for `Forward`/`TurnLeft`/`TurnRight` before calling `Command.execute` | 88.2 ms | 79.8 ms | 213 MB | Within noise |
| E | Mutable local loop inside one robot (throwaway lower bound) | 61.6 ms | 61.9 ms | 61 MB (−76%) | Lower bound only |

Notes:

- **A:** the ordered set's insertion-order bookkeeping is never used. A hash set
  allocates less per scent and does less work per lookup.
- **C:** `RobotState` stores the packed `Long` unboxed. But each `Position` is
  boxed again when passed through the `SafeSquares` interface and into the
  scent set, which cancels out the saving.
- **D:** dispatch to the three commands is not the bottleneck.
- **E:** the cost of keeping each world an immutable value is about 30% of the
  time and three quarters of the allocation. The rest of the time is spent
  picking which command each instruction is. Uniformly random letters make that
  branch unpredictable for the CPU, so this is partly a property of the
  generated input.

## Recommendations

> **Status:** recommendation 1 was applied in commit `7ab4340`. Recommendation 3
> was applied afterwards: input is now streamed (ADR 0001 §7). The
> recommendations below are unchanged, as written at the time of measurement.

1. **Use `persistentHashSetOf` for scents (A).** It is a two-line change with no
   design impact.
2. **Run robots in parallel and re-run only those that hit a scent.** A robot's
   outcome depends on scents only at the points where it tries to step onto an
   unsafe square.
   - First, run every robot in parallel with no scents, recording where each
     first tries to step off.
   - Then go through the robots in input order, keeping track of the scents
     left so far. A robot whose first attempt to step off was from a point an
     earlier robot had already scented is re-run against those scents. Every
     other robot's result is already correct, and adds its scent if it was
     lost.

   Here that means re-running 0 of 100,000 robots on the 1G field and about
   13,000 on the 1k field. Close to linear speed-up on large, sparse fields
   should follow. Only `MartianRobots.execute` would change.
3. **Read input as a stream** if the number of robots becomes very large. The
   whole `Mission` is currently held in memory, at about 4 bytes per instruction
   plus a list per robot. Reading and running one robot at a time keeps memory
   constant and suits batched parallel execution.
4. **Make the robot mutable within its own run, and only if the 30% matters.**
   Scents would still be persistent between robots. This reverses the main
   reason for ADR 0001 §1 (pure commands, before/after tests), so it would need
   its own ADR.

Rejected on measurement: B, C, D.

## Limits of this benchmark

- Instructions are uniformly random. Real instruction strings are likely more
  regular, so command dispatch would be easier for the CPU to predict.
- Parsing is not measured; `execute` runs on programs prepared in `@Setup`.
- These are single-machine, single-fork numbers. Error bars on the 1G-field rows
  are wider (±5–8 ms), so differences under about 5 ms between rows are not
  significant.
