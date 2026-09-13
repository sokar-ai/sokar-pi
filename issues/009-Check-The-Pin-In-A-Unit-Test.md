# 009 — Check the pin in a unit test instead of a Python script

**Priority:** 1
**Opened:** 2026-09-13
**Source:** Sokar requirement B53, "The Build In One Language" - agent half handed to this repository on 2026-09-13; the operator made B53 first priority that day

## What

`buildtools/check-pin.py` asks five questions. The first four need nothing but the repository and the
build's own output, so they become a unit test in the agent module, reading the **filtered**
definition in `target/classes`:

1. the version is a version, not an unsubstituted `${...}` - filtering happened at all
2. it is the version `pom.xml` pins
3. `package.json` asks for that version
4. `package-lock.json` resolves it, which is what `npm ci` installs

The fifth - *the Node runtime is the reviewed one: the build script's defaults and not an override,
and the runtime actually built reports that version* - needs the tree `build-pi-tree.sh` produced.
It is not a unit test; it is a check after the tree is built, in the same Maven run.

## The minimum regression matrix, carried over from issue 002

Run by hand on 2026-09-12. The replacement must reproduce all three: a clean run passes;
`NODE_VERSION=23.0.0` in the environment fails with a message naming the reason; and against a built
tree the runtime reports the pinned version.

## What would close it

- Questions 1-4 as a unit test and question 5 as a post-build check, each **proven against the failure
  it exists for** before it is trusted - the matrix above included.
- `build.yml` and `update.yml` no longer run `check-pin.py`, and the script is deleted.
