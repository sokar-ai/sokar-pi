# 006 — Check what Pi asks at first run inside a task

**Priority:** 1
**Opened:** 2026-09-13
**Source:** Sokar requirement B24, handed to this repository on 2026-09-13

## What

Never checked. What is declared is `--no-approve`, which answers the one question measured on 0.85.0:
whether to trust a workspace carrying `.pi` or `.agents/skills`. Whether a fresh task shows anything
else before work - a first-run wizard, a login menu, a telemetry question - nobody has looked.

## Why it matters

A question nobody answers is a task that starts and then waits. Sokar's suite cannot notice it,
because its stub agent asks nothing.

## What would close it

- Measured at a real terminal in `/workspace`, on a machine where Pi never ran: with a credential,
  without one, and headless.
- Each dialog found either answered by a file this adapter declares, or recorded as not answerable
  with the reason.
- Checked on the way: whether a task **without** a credential still gets the status extension. It
  needs no token, but the adapter's files were first written on the assumption that nothing is
  useful without one.
- The result in `doc/decisions.md`.

## Open question

Whether Pi has any first-run dialog besides the trust question.
