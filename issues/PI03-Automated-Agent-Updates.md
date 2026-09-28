# PI03 — Automated agent updates: what is still undecided

**Priority:** 2
**Opened:** 2026-09-12
**Source:** handed over from Sokar requirement **A02**, 2026-09-12. The requirement covered all
three agent repositories; this is this repository's share of it.
**Depends on:** the same issue in `sokar-claude-code` and `sokar-omp` - any rule agreed here should be the same rule there,
or the divergence should be deliberate.

## What is already built here

Detect, apply, verify, publish all exist, as commands of Sokar's release tool: `upstream-version`
reads the npm dist-tag, `update` moves the pin and regenerates the lockfile, the unit test `PinAgreementTest`
refuses a disagreement between the four places that name a version - and also refuses a Node
runtime that is not the reviewed one - and `compare-bills` stops a release that
changes what third-party code ships.

So this issue is not "build the pipeline". It is the set of questions the pipeline still answers by
convention rather than by a stated rule.

## Decided

- **The verifying tier makes a real model call, and its key lives in the workflow's secrets** - intended,
  not temporary. Published metadata proves the download is intact; only a real request proves the new
  version still starts without a question, reads its credential variable, routes through the broker
  and gets an answer. The ready and dialog checks run beside it.
- **When only the pinned CLI moves, the package's patch version is bumped**, so a package version names
  the CLI it installs.
- **A release is taken once it is three days old** and still the newest.
- **The Node runtime, `fd` and `ripgrep` are watched by the same job, under the same rule** - Node on
  its LTS line - one pull request per moved pin.

## Waits on

- **Sokar B81** ([index](https://github.com/sokar-ai/sokar/blob/main/issues/base/README.md)) - Sokar's
  release tool taking a release once it has aged, with a date for every upstream kind, bumping
  `x.y.z-SNAPSHOT` to `x.y.(z+1)-SNAPSHOT` when it moves the pin, and moving named pins.
- **Sokar B82** (same index) - `sokar-machines` keeping the CI machines current: GraalVM and the
  pre-pulled base images, under the same rule, once for every repository.

The tree's pins are already pom properties (`pin.node.*`, `pin.fd.*`, `pin.rg.*`), which B81's named
pins move; `build-pi-tree.sh` keeps no default of its own.

Once B81 is built, this repository's update job passes the three-day bound and, where it has them,
asks for each named pin; the rest is Sokar's.

## What would close it

Each question answered in `doc/decisions.md` with its reasoning, and where the answer is a rule the
pipeline must keep, a check that fails when it is broken. An answer that lives only in a person's
head is what this issue exists to remove.
