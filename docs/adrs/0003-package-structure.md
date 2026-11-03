# ADR 0003 — Package structure and dependency rules

- **Status:** Accepted
- **Date:** 2026-11-03
- **Scope:** How production code is divided into packages, and which packages
  may depend on which. Builds on [ADR 0002](0002-robot-addressed-commands.md)
  §3, which isolated the domain.

## Context

After ADR 0002 the domain was isolated, but the root `martians` package still
mixed five concerns:

- the command rules;
- the scent store;
- a runner that ran robots one after another;
- the code that put the application together;
- the command-line entry point.

The code that steps one robot sat in `martians.running` under the name
`SequentialProgramRunner`. The runner that really was sequential, running
robots one after another, sat in the root package.

Production code also carried APIs used only by tests:
`InputParser.parse(String): Mission`, `Mission` itself, and
`MartianRobots.run(String): String`.

## Decision

### 1. One package per concern

| Package | Holds | May depend on |
|---|---|---|
| `martians.domain` | positions, orientation, terrain, robot state and programs, `Command`, `Outcome`, `Effect`, `Environment` | nothing in the project |
| `martians.commands` | `TurnLeft`, `TurnRight`, `Forward`, `standardCommands` | domain |
| `martians.parsing` | `InputParser`, `LineParser`, `InputException` | domain |
| `martians.running` | `RobotStepper`, `SequentialMissionRunner`, `Surface` | domain |
| `martians` | `MartianRobots` (the composition root), `Main` (the CLI) | everything |

`RobotStepper` steps one robot and is used by any mission runner.
`SequentialMissionRunner` runs robots strictly in order, as the brief
requires. A parallel or reconciling runner would be its sibling. `Surface`, the
runner-owned scent store, lives with the runners.

`MartianRobots` is the one place where the brief's choices are put together:
the text input format, the standard commands, the sequential runner and the
one-line report. No other code supplies defaults for those choices.

`ArchitectureTest` checks the "May depend on" column with ArchUnit; each
package may also depend on the Kotlin and Java runtimes.

> In the context of **a growing codebase with several independent extension
> points**, facing **a root package that mixed rules, storage, running,
> assembly and the CLI**, we decided for **one package per concern, with
> `parsing`, `running` and `commands` depending only on `domain` and the root
> putting them together, enforced by ArchUnit**, and neglected **keeping
> everything outside the domain in the root package**, to achieve **packages
> that change for one reason each, extension points that are easy to find, and
> boundaries that a test keeps honest**, accepting **more packages and imports
> than a program this size strictly needs**.

### 2. No production API exists only for tests

`Mission`, `InputParser.parse(String)` and `MartianRobots.run(String)` are
removed. Tests split a string into lines with a test helper,
`String.inputLines()`, and call the streaming entry points that production
uses.

> In the context of **convenience APIs for tests**, facing **production code
> that exists only to serve tests**, we decided for **test-side helpers over
> the real streaming API** and neglected **keeping whole-input variants in
> production**, to achieve **one parsing path and one running path, both
> exercised by every test**, accepting **a line or two of collecting code in
> tests that want a whole result**.

## Consequences

**Positive**

- Each extension point has an obvious home. A new command goes in `commands`
  and a new runner in `running`. A new input format would be a sibling of
  `parsing`, and the root would choose between them.
- The dependency rules fail the build if, say, `running` starts depending on
  `parsing`.

**Negative / costs**

- Test helpers live in the root test package and depend on `running`
  (`Surface`), so tests in every package import from there.

**Deferred (intentional)**

- Moving output formatting (`RobotState.report()`) out of the domain to sit
  alongside parsing. It was deliberately placed on `RobotState`; it would
  follow if a second output format appeared.
