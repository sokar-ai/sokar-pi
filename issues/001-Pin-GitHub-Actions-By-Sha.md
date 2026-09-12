# 001 — Pin GitHub Actions by commit SHA

**Priority:** 1
**Opened:** 2026-09-12
**Source:** `.codex-review.md`, finding P-01 (High)
**Tracked centrally:** handed to the Sokar backend agent on 2026-09-12 as a requirement, because
it applies to every repository the four of us maintain rather than to this one.

## What

Every third-party action in this repository is referenced by a mutable tag:

- `.github/workflows/build.yml` — `actions/checkout@v7`, `graalvm/setup-graalvm@v1`,
  `actions/upload-artifact@v7`, `jfrog/setup-jfrog-cli@v5`
- `.github/workflows/update.yml`, `.github/workflows/artifactory-smoke.yml` — the same checkout and
  GraalVM references

## Why it matters

Those jobs run with the Artifactory publishing token, Hetzner credentials, a provider key, and in
the update workflow a token that can open and merge a pull request. A tag is a movable pointer: if
it is retargeted, or an upstream release is compromised, arbitrary code runs next to those secrets
and can change what gets published.

## What would close it

Each `uses:` names a full commit SHA with the released version in a comment beside it, **and**
something keeps those pins current - Dependabot or an equivalent reviewed process. A pin without an
update path trades a supply-chain risk for a rot risk, so the second half is not optional.

Worth adding at the same time: a check that fails the build when a workflow references a mutable
tag, so the property holds without anybody remembering it.

## Not doing it here yet

The wording of the requirement, and whether all four repositories move together, belongs to the
backend agent's requirement rather than to this repository alone.
