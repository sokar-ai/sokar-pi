# Decisions

Settled reasoning that outlives the change that produced it. A decision is written here when
somebody would otherwise ask "why is it like this?" and find only code.

Accepted risks live here too. An accepted risk is not a forgotten one: it says what the exposure
is, why it is not being removed, and what would change the answer.

Newest first, and in the order they stand below. The date is when the decision was taken,
not when its row was written - the older ones were found with `git log -S` on the sentence
rather than guessed.

| Date | What was decided |
|---|---|
| 2026-09-27 | [The release tooling is Sokar's, configured from the pom](#the-release-tooling-is-sokars-configured-from-the-pom) - data beside the pin, the relock image kept equal to the builder's by a test |
| 2026-09-18 | [Pi does not check for a newer version in a task](#pi-does-not-check-for-a-newer-version-in-a-task) - the check could only fail, and never installed anything |
| 2026-09-13 | [The changelog check is removed, not replaced](#the-changelog-check-is-removed-not-replaced) - requiring an entry returns with Sokar B55, on logchange |
| 2026-09-13 | [`build-pi-tree.sh` stays a shell script](#build-pi-treesh-stays-a-shell-script) - it orchestrates podman and npm, and Java would be the same calls in more lines |
| 2026-09-12 | [The bill generator is installed from its own lockfile, not resolved at build time](#the-bill-generator-is-installed-from-its-own-lockfile-not-resolved-at-build-time) - and neither it nor npm's cache is shipped any more |
| 2026-09-12 | [Accepted risk: the Node runtime digest was first read from the service that serves it](#accepted-risk-the-node-runtime-digest-was-first-read-from-the-service-that-serves-it) - reviewed and pinned since, and the built runtime is asked its version |
| 2026-09-05 | [Three projects, two names](#three-projects-two-names) - the confusion that cost real work until 2026-09-05 |
| 2026-09-04 | [The endpoint is set by a file, not a variable](#the-endpoint-is-set-by-a-file-not-a-variable) - why an agent declares what shape of endpoint it can address |
| 2026-09-04 | [What was actually proven about brokering this agent](#what-was-actually-proven-about-brokering-this-agent) - which transport, which credential kind, against what |

## The release tooling is Sokar's, configured from the pom

**Decided 2026-09-27 with Agent Sokar**, when the shared `sokar-release` replaced the Python tools
that had been copied into all three agent repositories and had already drifted between them.

**What differs between agents is data**, and it lives in `pom.xml` as `sokar.release.*`, beside
`agent.cli.version`: the label, the source definition, the npm registry and dist-tag, no digest,
the package name and the Node image the lockfile is resolved in. Flags on each call would have put
the same facts on every workflow line, and the second copy is the one that goes stale.

**The relock image is the one fact written twice**, because the tool reads the pom uninterpolated
and `build-pi-tree.sh` pins `NODE_VERSION` for the tree build. `PinAgreementTest` fails when the two
name different images, so a lockfile is never resolved by one npm and installed by another. Moving
the pin into the pom and passing it to the script would remove the copy; it was not done here
because the script's own guards and their tests read its defaults.

**At package time the tool is a plugin dependency of the exec plugin**, not a dependency of the
project, so it never reaches the bill of materials or the native image's classpath.

**Measured before the Python was deleted**, against the same inputs: `merge-tree-bill` carried all
135 of the tree's components under the same subject, identical except one description whose
trailing space the tool trims; `upstream-version` answered `rollback` for a dist-tag older than the
pin; `compare-bills` stopped on a component nested in the tree that the update did not name. The
relock itself was not run - it needs podman, which the machine that did this has not got.

## Pi does not check for a newer version in a task

**Decided 2026-09-18 by the operator**, after Claude Code was found updating itself inside a task.

**Read in the pinned 0.85.0, not run:** at start, Pi asks `https://pi.dev/api/latest-version` and
at most shows *"Update Available"* with the command to run. It installs nothing by itself. `pi update`
would need pi.dev and the npm registry, and this definition allows no host but the provider's, so
in a task the check can only fail.

**How it is stopped:** the launcher `/usr/local/bin/pi` exports `PI_SKIP_VERSION_CHECK=1` before it
starts Pi. Pi has no setting for it, only the variable. Not `PI_OFFLINE`, which also switches off
other things Pi fetches.

**What is not covered:** `pi update` itself has no switch. What keeps it from changing the tree is
that the task reaches neither host.

**What would change it:** a Pi release that installs updates by itself, which the weekly update job
would have to catch before it is published.

## The changelog check is removed, not replaced

**Decided 2026-09-13 by the operator**, across all Sokar repositories.

`buildtools/check-changelog.py` failed a push whose code change did not touch `CHANGELOG.md`. It is
deleted, and nothing replaces it for now. Sokar is moving to logchange - one YAML file per change,
and a generated `CHANGELOG.md` - and a check for a hand-kept file would have to be rebuilt the moment
that reaches this repository. Requiring an entry returns as Sokar B55, proposed to logchange upstream
first, which keeps the three lessons the script carried: a waiver answers for its own commit only,
documentation is not exempt, and a range that cannot be compared fails.

**Until then** the changelog is still written by hand in the same commit; only the enforcement is gone.

**What would change it:** B55 landing, or logchange being adopted here.

## `build-pi-tree.sh` stays a shell script

**Decided 2026-09-13**, agreeing with Sokar B53's proposal for it.

The build is to be Java and Maven, and a file that is not says why. This one runs `npm ci` and the
bill generator inside a pinned builder container through podman, validates three overridable inputs,
and packs the tree. Every step is a process call; a Java version would make the same calls with more
lines and hide them behind a process API. What it must not lose is its input guards.

**Its regression matrix**, run by hand on 2026-09-12 and carried over from the retired issue about the Python tooling having no test harness.
Each of these must be refused with exit 2, executing nothing:

    NODE_VERSION='22.20.0; touch /tmp/pwned'
    NODE_VERSION='22.20.0$(id)'
    NODE_VERSION="'; rm -rf /out; '"
    NODE_SHA256=deadbeef
    PI_BUILDER_IMAGE='node:22; id'

Whoever changes the script runs the matrix and records the result in the commit.

**What would change the answer:** the tree build needing logic that is not a process call - parsing,
merging, deciding - which belongs in Java rather than grown into this file.

## The bill generator is installed from its own lockfile, not resolved at build time

**Decided:** 2026-09-12, from the security review in `.codex-review.md` (P-02).

`build-pi-tree.sh` used to fetch the CycloneDX generator with `npx --yes` while the tree that is
about to be shipped sat mounted writable beside it. The application dependencies were already
installed from a lockfile with integrity hashes; the tool that inspects them was not.

It now has its own `buildtools/sbom/package.json` and `package-lock.json`, is installed with
`npm ci --ignore-scripts`, and the directory is removed before anything is packaged. So every byte
of the generator is checked against a recorded hash, no install script runs next to the payload,
and the tool is a build input rather than part of the package.

**The cost, stated plainly:** a second lockfile to keep current. It is pinned deliberately - the
generator's version decides what the bill looks like, and that should change in a reviewed commit
rather than on the day the registry serves something newer.

## Accepted risk: the Node runtime digest was first read from the service that serves it

**Decided:** 2026-09-12.

The package ships a Node runtime downloaded from `nodejs.org` and checked against a SHA-256 that
lives in this repository. The pin is reviewed here, which is stronger than reading a digest from
the same response as the file - but the value was originally taken from that service, so the first
recording of it trusted that service.

**Why it is accepted:** the digest is now a reviewed constant in a file that changes only through a
commit, and since 2026-09-12 the pin check refuses an environment override and asks the runtime
that was actually built what version it is - a unit test since 2026-09-13, `PinAgreementTest`. An attacker would have to have compromised nodejs.org
at the moment the pin was first recorded, and the pin would then still be stable and auditable.

**What would change it:** verifying the Node release signature (the project publishes signed
`SHASUMS256.txt`) in the build or the pin check, which is worth doing when this is next touched.

## Three projects, two names

**Recorded 2026-09-12** when Sokar requirement A04 was retired into this repository, because the
confusion it documents cost real work and will recur.

`earendil-works/pi` is this agent: the Pi Agent Harness, published as
`@earendil-works/pi-coding-agent`, run as `pi`. **Oh My Pi is a fork of a different Pi** by a
different author, published as `@oh-my-pi/pi-coding-agent` and run as `omp`. Until 2026-09-05 the
requirements said otherwise, and work recorded as having been done against one had been done
against the other.

## The endpoint is set by a file, not a variable

Pi cannot be pointed at a broker with an environment variable: its endpoint comes from an extension
it auto-discovers. That is the measured reason an agent definition declares **what shape of
endpoint it can address** rather than Sokar assuming a variable exists - a distinction the contract
now makes for every agent because of this one.

Verified for the common API dialect against OpenRouter. Whether it holds for a provider that does
not speak that dialect is open, and is `issues/004`.

## What was actually proven about brokering this agent

**Recorded 2026-09-12** from Sokar requirement A04 before it was retired.

Brokering is verified **for the common API dialect, against OpenRouter, and for nothing else**. The
number of providers this agent supports was never verified here; a figure that once appeared in the
requirement belonged to no project in particular, which is why none is repeated here.
