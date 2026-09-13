# 013 — The built Node runtime's version is never asked in CI

**Priority:** 2
**Opened:** 2026-09-13
**Source:** found while turning the pin check into a unit test for Sokar requirement B53

## What

The pin check asks the Node runtime inside the built tree what version it is, and compares that with
the reviewed default in `build-pi-tree.sh`. It can only ask where a tree exists - and **in CI there is
none at the moment it asks.** `build.yml` and `update.yml` run the unit tests in a plain `verify`; the
tree is built later, by the `dist` profile's `prepare-package`, in a step that passes `-DskipTests`.

Measured from the workflow order, not inferred: this was true of `check-pin.py` before it became
`PinAgreementTest`, which printed *"no built tree, so the runtime itself was not asked"* and passed.
The test now reports the case as **skipped** rather than passed, which makes the gap visible without
closing it.

## Why it matters

The runtime digest is verified when the tree is built, so a wrong download cannot slip through. What
nothing checks in CI is that the runtime which ends up in the package reports the version the build
script pins - the last of the reviewed-runtime guarantees recorded in `doc/decisions.md`.

## What would close it

- A check that runs **after** the tree is built, in the same run that builds the packages, and fails
  that run when the built runtime reports another version.
- Proven against the failure: a tree whose runtime reports another version fails the run.

## Open question

Whether that check belongs in the `dist` build as a Java post-build step, or in the acceptance run,
which installs the package and could ask the installed runtime instead.
