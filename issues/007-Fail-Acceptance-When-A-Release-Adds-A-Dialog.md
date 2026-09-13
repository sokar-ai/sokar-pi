# 007 — Fail the acceptance run when a release adds a first-run dialog

**Priority:** 4
**Opened:** 2026-09-13
**Source:** Sokar requirement B24, handed to this repository on 2026-09-13
**Depends on:** Sokar B52, the acceptance-kit step "the agent reached work without a prompt"

## What

The acceptance suite checks what it knows about. Nothing fails when a new Pi release adds a
question before work starts, and Sokar's own suite cannot catch it either, because its stub agent
has no dialogs. The check has to be *"reached work without a prompt"*, not *"the known dialogs are
absent"*.

## Why it needs Sokar

Every step these scenarios use comes from Sokar's acceptance kit, and the same check is needed in
all three agent repositories. One step in the kit used three times, not three copies.

## What would close it

- A scenario that starts a fresh task attended and one that runs headless, failing when anything
  waits for input before the agent's own prompt or before the headless run completes.
- Proven by making it fail once against a dialog put back on purpose, before trusting it.

## Open question

What "reached work" looks like for this agent in a form the kit step can be told - a prompt marker
declared per agent, most likely.
