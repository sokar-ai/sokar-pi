# PI12 — Turn the acceptance script into scenarios on acceptance-kit steps

**Priority:** 2
**Opened:** 2026-09-13
**Source:** Sokar requirement B53, "The Build In One Language" - agent half handed to this repository on 2026-09-13; the operator made B53 first priority that day
**Depends on:** Sokar B53 for the steps; Sokar B52 is the first of them

## What

`buildtools/acceptance.sh` - 317 lines, diverged slightly from its two siblings - checks the
published package on a rented machine. The Cucumber half of the same run already uses the kit. The
shell half becomes scenarios on kit steps, so one check is written once for three agents.

PI07 (a release that adds a first-run dialog) is the first scenario of this kind.

## What would close it

- `acceptance.sh` deleted, and `build.yml` / `update.yml` run the scenarios instead.
- The checks added on 2026-09-12 survive as steps, each proven against its failure: the test
  credential never appears on a command line, and a credential split across a newline in a log is
  still found.

## Open question

Which of the script's checks the kit can express at all - answered by B53's steps, not guessed here.
