# PI15 — A test that never runs in the pipeline

**Priority:** 2
**Opened:** 2026-09-27
**Source:** measured 2026-09-27 in answer to Sokar B51 ([index](https://github.com/sokar-ai/sokar/blob/main/issues/base/README.md)), which asks whether a test that runs nowhere says so

## What

`PinAgreementTest.theBuiltRuntimeReportsThePinnedVersion` asks the built Node runtime for its version
and compares it with the pinned default. **It is skipped in every run this pipeline makes**, and its
own comment says why without saying that it is always so: *"Only where a tree has been built. The
unit phase runs before the tree is built, so on a clean checkout this is skipped."*

The tree is built by `build-pi-tree.sh` at **`prepare-package`**, and surefire runs at **`test`** -
one phase earlier, every time. So the assumption it is gated on, `target/tree/node/bin/node` being
executable, is false for the whole of the unit phase and true only in a second invocation over a
dirty `target/`.

**Measured, both sides:**

    locally, ./mvnw -o -B test      Tests run: 10, Skipped: 1   PinAgreementTest
    CI, the update run of 09-21     Tests run: 10, Skipped: 1   PinAgreementTest

**What it costs:** the one check that the runtime a package ships actually reports the pinned version
has never run anywhere. It reads as covered - the class is green, the suite is green, and the skip is
a `[WARNING]` in the middle of a passing log.

**Sibling repositories have none of this:** `sokar-claude-code` and `sokar-omp` report `Skipped: 0`,
so the class exists here and only here, because only this repository builds a tree.

## What would close it

- The check runs where the tree exists - failsafe at `integration-test`, after `prepare-package`, in
  the profile that builds the tree.
- It fails rather than skips when the tree is missing in a run that was supposed to build one.
- Proven by running it against a tree whose Node reports another version, which
  `refusesABuiltRuntimeThatReportsAnotherVersion` already does for the comparison itself.

## Open question

Whether the packaging profile is the right home, given that the Python tools it sits beside are on
their way out with PI10 and PI11. If the tree build moves, this check moves with it.
