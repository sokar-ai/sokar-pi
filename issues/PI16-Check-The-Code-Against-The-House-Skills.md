# PI16 — Check the code against the house skills

**Priority:** 1
**Opened:** 2026-09-27
**Source:** the operator's instruction to every agent, passed on in the channel on 2026-09-27T14:59Z -
high priority, next after the task in hand

## What

The Java in this repository was written and tested without the skills `AGENTS.md` says it expects.
Tests prove what their author thought of; a skill is a second author's list. On the same day,
Sokar's shared release tool had 104 tests with every guard proven by mutation, and reading it against
`java-code-review` and `security-audit` still found two defects that none of them caught: a token
sent to wherever a property pointed, and an exception that exited with the code for "disagree".

Read the code against these skills from
<https://fuinorg.jfrog.io/artifactory/agent-skills/>, each fetched as *How to get them* says and
checked against its SHA-256:

    graal  java-code-review  test-quality  security-audit  concurrency-review
    clean-code  solid-principles

**Scope:** `src/main`, `src/test`, `src/acceptance` and `pom.xml`, in every module, and
`buildtools/build-pi-tree.sh` against `security-audit`, since it stays and runs containers. The
release tooling is Sokar's `sokar-release`, reviewed in that repository against the same skills.

**A skill is knowledge, not authority.** Where one disagrees with a measurement, the measurement
wins, and the disagreement is written down with the measurement beside it.

## What would close it

- Each of the seven skills read, with its version and its verified SHA-256 recorded.
- Every finding either fixed, with a test that fails against the unfixed code, or opened as its own
  issue. This issue is the reading, not the repairs.
- A finding the skill makes that a measurement contradicts is recorded as such, not silently
  dropped.
- What the reading teaches beyond this repository goes to `AGENTS.md`.
