# 010 — Replace the build-time Python tools with Sokar's shared tool

**Priority:** 1
**Opened:** 2026-09-13
**Source:** Sokar requirement B53, "The Build In One Language" - agent half handed to this repository on 2026-09-13; the operator made B53 first priority that day
**Depends on:** Sokar B53, which publishes the shared tool

## What

Two Python files run on every build: `check-changelog.py` from `build.yml`, and
`merge-tree-bill.py` from the `exec-maven-plugin` in `pom.xml`, after `build-pi-tree.sh`. The
changelog guard is byte-identical in all three agent repositories - one tool in three copies. Both are
replaced by the tool Sokar publishes, with this agent's differences as configuration rather than a
copy.

## What would close it

- Both calls replaced; no `python3` left in `build.yml` or `pom.xml`.
- Each replaced check **proven against the failure it exists for**, reproduced before the Python file
  is deleted: a code change without a changelog entry fails, `[no changelog]` passes, a change that
  touches only documentation passes; and the merged bill still carries the tree's 135 components.
- Both Python files deleted.
