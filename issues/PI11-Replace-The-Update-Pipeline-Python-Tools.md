# PI11 — Replace the update pipeline's Python tools with Sokar's shared tool

**Priority:** 1
**Opened:** 2026-09-13
**Source:** Sokar requirement B53, "The Build In One Language" - agent half handed to this repository on 2026-09-13; the operator made B53 first priority that day
**Depends on:** Sokar B53, which publishes the shared tool

## What

`update.yml` runs `upstream-version.py`, `update.py` and `compare-bills.py`. The first two have
already drifted between the three agent repositories; the bill comparison has not. They are replaced
by the tool Sokar publishes, with this agent's npm dist-tag and lockfile update as configuration.

Whatever PI03 settles about the update rules applies to the replacement unchanged.

## What would close it

- No `python3` left in `update.yml`, and all three files deleted.
- Each replaced check **proven against the failure it exists for**, reproduced first: an older
  upstream than the pinned one stops red instead of rolling back; a bill that changed in a component
  the update did not name fails the comparison.
