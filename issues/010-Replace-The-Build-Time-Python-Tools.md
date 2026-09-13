# 010 — Replace the build-time Python tool with Sokar's shared tool

**Priority:** 1
**Opened:** 2026-09-13
**Source:** Sokar requirement B53, "The Build In One Language" - agent half handed to this repository on 2026-09-13; the operator made B53 first priority that day
**Depends on:** Sokar B53, which publishes the shared tool

## What

`merge-tree-bill.py` runs on every package build, from the `exec-maven-plugin` in `pom.xml`, after
`build-pi-tree.sh`. It is replaced by the tool Sokar publishes, with this agent's difference as
configuration rather than a copy.

The changelog check that stood beside it here is gone rather than replaced: removed on 2026-09-13 by
the operator's decision. Requiring an entry returns with Sokar B55, built on logchange - the
reasoning is in [`doc/decisions.md`](../doc/decisions.md).

## What would close it

- The call replaced; no `python3` left in `pom.xml`.
- The replaced step **proven against the failure it exists for**, reproduced before the Python file
  is deleted: the merged bill still carries the tree's 135 components.
- The Python file deleted.
