# 005 — Declare what waiting looks like for this agent

**Priority:** 2
**Opened:** 2026-09-12
**Source:** handed over from Sokar requirement **A11**, 2026-09-12, which I wrote and measured.
The requirement covered all three agents; this is this repository's share.
**Depends on:** Sokar requirement **B47** (the daemon side: the manifest field, the matcher, the
contract). Nothing here can be read by anything until that exists. It also depends on the same
issue in `sokar-claude-code` and `sokar-omp`, because the declaration's shape should be one shape.

## What this agent has to declare

A working rule, and honestly nothing for waiting. Measured at 0.85.0: a separator line
`── ⠏ Working ───…` while it works, and **no marker at all** when it waits - its question is plain
text and the chrome around it is byte-for-byte the idle screen.

## What is already known, measured 2026-09-12

- **Headless: nothing to declare.** `agent_settled` is the last line before exit, not a wait.
- **Attached: the waiting state is indistinguishable from idle.** This is the *cannot say* case in
  the flesh, and the contract must report it as that rather than as *not waiting*.
- **The window title never changes**: `π - <directory>` in every state.
- This repository already installs an extension into the container (`PiStatusExtension`), which
  since 2026-09-12 writes a state file from the events pi emits around every prompt. That is a
  better signal than anything on the screen - but it is this repository's own mechanism, not a
  declaration the daemon reads, and B47 has to decide whether it wants to read a file an agent
  package placed.

## What would close it

- A declaration in `src/main/resources/agent/pi.yaml`, written from a measurement rather than from
  upstream's documentation.
- A test in this repository that drives the agent to a waiting point and asserts the declaration
  still matches - so a version bump that changes the wording fails this build instead of going
  quiet in the field.
- Or, where there is nothing to declare, a test asserting the contract reports *cannot say* rather
  than *not waiting*.

## Why it waits on B47

The manifest field does not exist yet, and the reader for it does not exist yet. A declaration
written now would be text nothing parses. What can be done before B47 lands is the measurement,
and that is done.
