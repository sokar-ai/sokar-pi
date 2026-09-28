# Decisions

Settled reasoning that outlives the change that produced it. A decision is written here when
somebody would otherwise ask "why is it like this?" and find only code.

Accepted risks live here too. An accepted risk is not a forgotten one: it says what the exposure
is, why it is not being removed, and what would change the answer.

Ordered by what each decision covers; when one was taken, `git log` answers.

| What it covers | Decision |
|---|---|
| The agent in a task | [The endpoint is set by a file, not a variable](#the-endpoint-is-set-by-a-file-not-a-variable) - why an agent declares what shape of endpoint it can address |
| The agent in a task | [What is proven about brokering this agent](#what-is-proven-about-brokering-this-agent) - which transport, which credential kind, against what |
| The agent in a task | [Pi does not check for a newer version in a task](#pi-does-not-check-for-a-newer-version-in-a-task) - the check could only fail, and never installs anything |
| The agent in a task | [Pi shows nothing before work in a task](#pi-shows-nothing-before-work-in-a-task) - no dialog with a credential, without one, or headless; the status extension loads without a token |
| Which project this is | [Three projects, two names](#three-projects-two-names) - Pi, and the different Pi that Oh My Pi forks |
| The build | [The release tooling is Sokar's, configured from the pom](#the-release-tooling-is-sokars-configured-from-the-pom) - data beside the pin, the relock image kept equal to the builder's by a test |
| The build | [`build-pi-tree.sh` stays a shell script](#build-pi-treesh-stays-a-shell-script) - it orchestrates podman and npm, and Java would be the same calls in more lines |
| The build | [The bill generator is installed from its own lockfile, not resolved at build time](#the-bill-generator-is-installed-from-its-own-lockfile-not-resolved-at-build-time) - and neither it nor npm's cache is shipped |
| The build | [NullAway is configured in this pom, not in the shared parent](#nullaway-is-configured-in-this-pom-not-in-the-shared-parent) - the parent is not this repository's to change |
| The build | [No check requires a changelog entry](#no-check-requires-a-changelog-entry) - requiring one returns with Sokar B55, on logchange |
| Accepted risk | [The Node runtime digest was first read from the service that serves it](#accepted-risk-the-node-runtime-digest-was-first-read-from-the-service-that-serves-it) - reviewed and pinned, and the built runtime is asked its version |

## The endpoint is set by a file, not a variable

Pi cannot be pointed at a broker with an environment variable: its endpoint comes from an extension
it auto-discovers. That is the measured reason an agent definition declares **what shape of
endpoint it can address** rather than Sokar assuming a variable exists - a distinction the contract
makes for every agent because of this one.

Verified for the common API dialect against OpenRouter. Whether it holds for a provider that does
not speak that dialect is open, and is PI04.

## What is proven about brokering this agent

Brokering is verified **for the common API dialect, against OpenRouter, and for nothing else**. How
many providers this agent supports is not verified here, which is why no figure is given.

## Pi does not check for a newer version in a task

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

## Pi shows nothing before work in a task

**A fresh task reaches Pi's prompt with no dialog**, measured on 0.85.0 at a terminal, a fresh task
per run:

- **With a credential:** Pi starts at its prompt, both extensions loaded (`sokar-route.ts`,
  `sokar-status.ts`). The one question Pi asks - whether to trust a workspace carrying `.pi` or
  `.agents/skills` - is answered by `--no-approve`.
- **Without one:** Sokar does not start the agent at all ("the agent would start without a credential
  and fail on its first request"). Started by hand in a task with a shell attached, Pi still asks
  nothing; it warns that no model is available, and **the status extension loads** - it needs no
  token, so a task without a credential still reports what Pi is doing. Only the routing extension
  is absent, as it should be with no endpoint to route to.
- **Headless:** a prompt run answers and exits; nothing waits.

**Pi tries to download `fd` and `ripgrep` at every start** and fails, because a task cannot resolve
github.com. It warns and carries on without them; that is PI20 in the issue index, not a dialog.

## Three projects, two names

`earendil-works/pi` is this agent: the Pi Agent Harness, published as
`@earendil-works/pi-coding-agent`, run as `pi`. **Oh My Pi is a fork of a different Pi** by a
different author, published as `@oh-my-pi/pi-coding-agent` and run as `omp`. The names invite
confusing them, and work done against one is not work done against the other - so a requirement,
an issue or a measurement names which one it means.

## The release tooling is Sokar's, configured from the pom

The update, the pin and the bill are Sokar's `sokar-release`, shared by all three agent
repositories. Copies of the same tools in each repository drift apart.

**What differs between agents is data**, and it lives in `pom.xml` as `sokar.release.*`, beside
`agent.cli.version`: the label, the source definition, the npm registry and dist-tag, no digest,
the package name and the Node image the lockfile is resolved in. Flags on each call would put the
same facts on every workflow line, and the second copy is the one that goes stale.

**The relock image is the one fact written twice**, because the tool reads the pom uninterpolated
and `build-pi-tree.sh` pins `NODE_VERSION` for the tree build. `PinAgreementTest` fails when the two
name different images, so a lockfile is never resolved by one npm and installed by another. Moving
the pin into the pom and passing it to the script would remove the copy; it is not done because the
script's own guards and their tests read its defaults.

**At package time the tool is a plugin dependency of the exec plugin**, not a dependency of the
project, so it never reaches the bill of materials or the native image's classpath.

**Measured against the same inputs as the tools it replaces:** `merge-tree-bill` carries all 135 of
the tree's components under the same subject, identical except one description whose trailing
space the tool trims; `upstream-version` answers `rollback` for a dist-tag older than the pin;
`compare-bills` stops on a component nested in the tree that the update did not name. The relock
itself is not measured this way - it needs podman.

## `build-pi-tree.sh` stays a shell script

The build is Java and Maven, and a file that is not says why. This one runs `npm ci` and the bill
generator inside a builder container pinned by digest through podman, validates five overridable
inputs, and packs the tree. Every step is a process call; a Java version would make the same calls
with more lines and hide them behind a process API. What it must not lose is its input guards.

**Its regression matrix.** Each of these must be refused with exit 2, executing nothing:

    NODE_VERSION='22.20.0; touch /tmp/pwned'
    NODE_VERSION='22.20.0$(id)'
    NODE_VERSION="'; rm -rf /out; '"
    NODE_SHA256=deadbeef
    PI_BUILDER_IMAGE='node:22; id'
    NODE_IMAGE_DIGEST=deadbeef
    JAVA_CMD='java; id'

Whoever changes the script runs the matrix and records the result in the commit.

**What would change the answer:** the tree build needing logic that is not a process call - parsing,
merging, deciding - which belongs in Java rather than grown into this file.

## The bill generator is installed from its own lockfile, not resolved at build time

The CycloneDX generator has its own `buildtools/sbom/package.json` and `package-lock.json`, is
installed with `npm ci --ignore-scripts`, and the directory is removed before anything is packaged.
So every byte of the generator is checked against a recorded hash, no install script runs next to
the tree that is about to be shipped, and the tool is a build input rather than part of the package.
Fetched with `npx --yes` instead, the tool that inspects the dependencies would be the one thing in
the build not pinned by a lockfile, running beside a writable payload.

**The cost, stated plainly:** a second lockfile to keep current. It is pinned deliberately - the
generator's version decides what the bill looks like, and that should change in a reviewed commit
rather than on the day the registry serves something newer.

## NullAway is configured in this pom, not in the shared parent

`org.fuin:pom` would make it true in every repository at once, but it is not this repository's to
change, and waiting for it would leave the `@NullMarked` promise unchecked. So the compiler
configuration, the two versions and `.mvn/jvm.config` are here, identical in `sokar-claude-code` and
`sokar-omp`. Three copies of one block is the shape `AGENTS.md` warns about; it is accepted because
the parent is the one place that removes it, and **moving it there is the answer the day the parent
takes it** - then all three copies go in the same change.

Only the `default-compile` execution runs it: tests pass `null` on purpose, and Error Prone never
sees them. `.mvn/jvm.config` exists because Error Prone runs inside javac in Maven's own JVM, and a
JDK from 16 on refuses it the compiler's internals - on JDK 25, an `IllegalAccessError` on
`com.sun.tools.javac.api` before a single file is checked.

## No check requires a changelog entry

The changelog is written by hand in the same commit, and nothing enforces it. Sokar is moving to
logchange - one YAML file per change, and a generated `CHANGELOG.md` - and a check for a hand-kept
file would have to be rebuilt the moment that reaches this repository. Requiring an entry returns as
Sokar B55, proposed to logchange upstream first, keeping three lessons: a waiver answers for its own
commit only, documentation is not exempt, and a range that cannot be compared fails.

**What would change it:** B55 landing, or logchange being adopted here.

## Accepted risk: the Node runtime digest was first read from the service that serves it

The package ships a Node runtime downloaded from `nodejs.org` and checked against a SHA-256 that
lives in this repository. The pin is reviewed here, which is stronger than reading a digest from
the same response as the file - but the value was taken from that service, so its first recording
trusted that service.

**Why it is accepted:** the digest is a reviewed constant in a file that changes only through a
commit, and `PinAgreementTest` refuses an environment override and asks the runtime that was
actually built what version it is. An attacker would have to have compromised nodejs.org at the
moment the pin was first recorded, and the pin would then still be stable and auditable.

**What would change it:** verifying the Node release signature (the project publishes signed
`SHASUMS256.txt`) in the build or the pin check, which is worth doing when this is next touched.
