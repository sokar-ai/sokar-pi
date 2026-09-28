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
| The agent in a task | [Reaching work is checked by what the screen shows, not by the dialogs known](#reaching-work-is-checked-by-what-the-screen-shows-not-by-the-dialogs-known) - a declared marker, waited for attended; Pi draws it only after a question |
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

**Pi looks for `fd` and `ripgrep` at every start** and downloads them from GitHub when they are
missing, which a task cannot reach. Interactive input waits for that attempt, and without `fd` the
`@`-file autocomplete offers nothing. So both ship in the tree, pinned by digest and in the bill,
linked onto `PATH`: measured, Pi then starts without the attempt and `@READ` offers `README.md`;
with them hidden from `PATH`, the attempt and its warnings return and the autocomplete is empty.

## Reaching work is checked by what the screen shows, not by the dialogs known

**The acceptance run fails when anything comes before Pi's prompt**, known or not. The definition
declares `Pi can explain its own features`, a line of the header Pi draws at its prompt; the kit's
step waits for it attended, typing nothing, and a second scenario requires an unattended run to end
within its bound. Both pass against `smith`. **Why the check can fail:** with a workspace carrying a
skill and `--no-approve` left off, Pi's trust question is up, and the bytes it has written by then -
read while the question was on screen - hold the question and not the header: Pi draws its prompt
only after the question is answered. The kit's own failing step was not run for this, because its
fixture project carries no skill.

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

**The relock image is the one fact written twice**, because the tool reads the pom uninterpolated:
`sokar.release.npm.image` spells out the builder that `pin.node.version` and `pin.node.image.digest`
name. `PinAgreementTest` fails when the two name different images, so a lockfile is never resolved by
one npm and installed by another; a Node move rewrites both in one write.

**At package time the tool is a plugin dependency of the exec plugin**, not a dependency of the
project, so it never reaches the bill of materials or the native image's classpath.

**Measured against the same inputs as the tools it replaces:** `merge-tree-bill` carries all 135 of
the tree's components under the same subject, identical except one description whose trailing
space the tool trims; `upstream-version` answers `rollback` for a dist-tag older than the pin;
`compare-bills` stops on a component nested in the tree that the update did not name. The relock
itself is not measured this way - it needs podman.

## Actions run from a commit, and Dependabot moves them

Every `uses:` names a full commit with its release beside it, `@<commit> # vX.Y.Z`. A tag is a name its
owner may point anywhere, and these jobs hold the publishing token, the machine credentials and a
token that merges pull requests. GitHub's own actions are held to the same rule: the argument does not
depend on who publishes the action.

**Dependabot keeps the pins current** - the operator's choice on 2026-09-28, for every repository. A
pin nobody moves rots, and a stale action with a known flaw is not safer than the current tag. Each
release arrives as a pull request with the new commit and its version, and nothing merges it
automatically: the review is what the pin buys.

**`WorkflowPinTest` fails the build on a step that names a tag, a branch or a bare hash**, and says how
to pin it, so a new workflow cannot bring a tag back. A step of this repository and an image by digest
pass.

**Nothing else in these workflows is fetched by a name**: the only `curl` is `jf rt curl` reading this
product's own Artifactory. The setup actions download their tools themselves, and there the two differ:

- **The JFrog CLI is fixed by the action's commit.** `setup-jfrog-cli` 5.2.0 defaults to `jf` 2.124.0
  and asks for the newest only when told to, so a Dependabot bump moves both together, under review. It
  checks no digest, which is the same publisher's trust as the action.
- **Accepted risk: GraalVM is not.** `java-version: '25'` with `graalvm-community` resolves the newest
  25.x from GitHub's release list at run time, and `setup-graalvm` 1.6.6 checks a checksum only for
  Oracle's enterprise builds. That JDK compiles the native binary this repository publishes. An exact
  version alone would not close it - a release asset can be replaced - and Dependabot does not move a
  `with:` value. **What would change it:** an exact version with a digest this workflow verifies, and a
  named process that moves both. That is one decision for every repository's JDK, not this one's.

## What an update takes, and when

The operator decided these on 2026-09-28. Sokar's release tool applies them; `UpdateRulesTest`
fails when this repository stops asking for them.

**A release is taken once it is three days old, and still the newest**: `sokar.release.min-age` is
`3d`. A release withdrawn or patched within days never becomes a pull request, and the people who
install on the first day get the days to report what breaks. A younger release is not skipped for the
one before it: the job waits and says until when. A release whose date cannot be read is never old
enough.

**A pin move bumps the package's patch version**, `1.0.0-SNAPSHOT` to `1.0.1-SNAPSHOT`, so a package
version names what it installs and `apt` sees an upgrade.

**The verifying tier makes a real model call**, with its key in the workflow's secrets - intended, not
temporary. Published metadata proves the download is intact; only a real request proves the new
version still starts without a question, reads its credential variable, routes through the broker and
gets an answer.

**The CI machines are not kept current here.** GraalVM and the pre-pulled base images are Sokar's
machine tooling, under the same rule, once for every repository.

**Node, `fd` and `ripgrep` are followed by the same job, under the same rule, one pull request each.**
Each is declared to the tool as `sokar.release.pin.<name>.*`: where a release is read - Node on its LTS
line 22, the two tools from their newest GitHub release - where its digest is read, and the properties
a move writes. A Node move also rewrites `sokar.release.npm.image`, which spells out its version and
image digest, so the relock image cannot part from the builder. `UpdateRulesTest` fails when a tree pin
is not declared to the tool, or not followed by the job.

**Measured against the tool at the commit that added named pins:** Node 22.20.0 to 22.23.3 wrote the
digests Node's `SHASUMS256.txt` and Docker Hub give; `fd` and `ripgrep`, moved one release back and
forward again, came back to exactly the pinned digests; an asset pattern matching two files was refused.

**What would change the answer:** a release that cannot wait three days. Dispatching the job with the
version named takes it at once - that is the way round the rule, not a change to it.

## `build-pi-tree.sh` stays a shell script

The build is Java and Maven, and a file that is not says why. This one runs `npm ci` and the bill
generator inside a builder container pinned by digest through podman, validates the pins Maven passes
in and its own overrides, and packs the tree. Every step is a process call; a Java version would make the same calls
with more lines and hide them behind a process API. What it must not lose is its input guards.

**Its regression matrix.** Each of these must be refused with exit 2, executing nothing:

    NODE_VERSION='22.20.0; touch /tmp/pwned'
    NODE_VERSION='22.20.0$(id)'
    NODE_VERSION="'; rm -rf /out; '"
    NODE_SHA256=deadbeef
    PI_BUILDER_IMAGE='node:22; id'
    NODE_IMAGE_DIGEST=deadbeef
    NODE_IMAGE_DIGEST=<the 64 hex digits without sha256:>
    JAVA_CMD='java; id'
    FD_SHA256=deadbeef
    RG_VERSION='15; id'
    any pin unset (the script keeps no default)

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
commit, `PinAgreementTest` refuses an environment override, and `BuiltTreeCheck` asks the runtime
that was actually built what version it is, in every package build. An attacker would have to have compromised nodejs.org at the
moment the pin was first recorded, and the pin would then still be stable and auditable.

**What would change it:** verifying the Node release signature (the project publishes signed
`SHASUMS256.txt`) in the build or the pin check, which is worth doing when this is next touched.
