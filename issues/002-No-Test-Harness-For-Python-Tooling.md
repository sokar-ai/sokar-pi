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

## The minimum regression matrix, until there is a harness

Run by hand on 2026-09-12 and the cases a change to these guards has to reproduce. Written down
here rather than left in the review answer, because a matrix nobody can find is repeated from
memory and shrinks each time.

`build-pi-tree.sh` must refuse each of these with exit 2, executing nothing:

    NODE_VERSION='22.20.0; touch /tmp/pwned'
    NODE_VERSION='22.20.0$(id)'
    NODE_VERSION="'; rm -rf /out; '"
    NODE_SHA256=deadbeef
    PI_BUILDER_IMAGE='node:22; id'

`check-pin.py` must answer in all three states: a clean run passes; `NODE_VERSION=23.0.0` in the
environment fails with a message naming the reason; and against a built tree the runtime reports
the pinned version.

Whoever changes these guards either automates the matrix or runs it and records the result in the
commit. "It still works" is not one of the two.

## What would close it

- Decide on a runner (pytest is the obvious one) and where its dependency is pinned.
- A CI step that runs it, failing the build like the Java tests do.
- Tests for the guards that already exist, starting with the update script's metadata rules.

Where a guard is worth testing today, the logic is kept in a function that takes its input as an
argument rather than fetching it - so a test needs no network when the harness arrives.
