# 002 — The Python build tooling has no test harness

**Priority:** 3
**Opened:** 2026-09-12
**Source:** work on `.codex-review.md` (the negative tests the review asked for)

## What

`buildtools/` holds several Python tools that decide what gets shipped - the update script, the pin
check, the changelog guard, the bill comparison. None of them has a test, and nothing in CI runs
one: there is no pytest, no unittest, no runner.

## Why it matters

The review asked for negative tests around metadata parsing, which is the right ask: these scripts
are the last thing between an upstream mistake and a published package. Writing one test today
means introducing a framework, a dependency and a CI step - a decision about how this repository is
built, not a fix.

Until then, guards in these scripts are verified by hand, which is not repeatable and leaves no
record.

## What would close it

- Decide on a runner (pytest is the obvious one) and where its dependency is pinned.
- A CI step that runs it, failing the build like the Java tests do.
- Tests for the guards that already exist, starting with the update script's metadata rules.

Where a guard is worth testing today, the logic is kept in a function that takes its input as an
argument rather than fetching it - so a test needs no network when the harness arrives.
