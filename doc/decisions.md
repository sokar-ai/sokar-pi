# Decisions

Settled reasoning that outlives the change that produced it. A decision is written here when
somebody would otherwise ask "why is it like this?" and find only code.

Accepted risks live here too. An accepted risk is not a forgotten one: it says what the exposure
is, why it is not being removed, and what would change the answer.

Ordered by what each decision covers; when one was taken, `git log` answers.

| What it covers | Decision |
|---|---|
| The agent in a task | [The endpoint is set by a file, not a variable](#the-endpoint-is-set-by-a-file-not-a-variable) - why an agent declares what shape of endpoint it can address; the one file the container gets |
| The agent in a task | [The broker is reached through a relay in the task's namespace](#the-broker-is-reached-through-a-relay-in-the-tasks-namespace) - a host listener is unreachable or open to all; the broker stays on the host |
| The agent in a task | [What is proven about brokering this agent](#what-is-proven-about-brokering-this-agent) - OpenRouter and GitHub Copilot, measured; a Copilot model must be one Pi lists |
| The agent in a task | [Pi does not check for a newer version in a task](#pi-does-not-check-for-a-newer-version-in-a-task) - the check could only fail, and never installs anything; telemetry off, pi.dev refused |
| The agent in a task | [Pi shows nothing before work in a task](#pi-shows-nothing-before-work-in-a-task) - no dialog with a credential, without one, or headless; the status extension loads without a token |
| The agent in a task | [Reaching work is checked by what the screen shows, not by the dialogs known](#reaching-work-is-checked-by-what-the-screen-shows-not-by-the-dialogs-known) - a declared marker, waited for attended; Pi draws it only after a question |
| The agent in a task | [Waiting for a person is declared as unknown, because it is](#waiting-for-a-person-is-declared-as-unknown-because-it-is) - Pi's screen does not tell a question from idle, so Sokar says "cannot say" |
| The agent in a task | [A task that comes back continues its conversation](#a-task-that-comes-back-continues-its-conversation) - the session id read from Pi's first record, unattended or attached |
| Which project this is | [Three projects, two names](#three-projects-two-names) - Pi, and the different Pi that Oh My Pi forks |
| The build | [The release tooling is Sokar's, configured from the pom](#the-release-tooling-is-sokars-configured-from-the-pom) - data beside the pin, the relock image kept equal to the builder's by a test |
| The build | [A release is built from releases only](#a-release-is-built-from-releases-only) - on a tag nothing from Central's snapshots, and a tag on a Sokar snapshot refused by the step itself |
| The build | [`build-pi-tree.sh` stays a shell script](#build-pi-treesh-stays-a-shell-script) - it orchestrates podman and npm, and Java would be the same calls in more lines |
| The build | [`follow-upstream.sh` is a shell script for the same reason](#follow-upstreamsh-is-a-shell-script-for-the-same-reason) - process calls and `git`, with no decision of its own |
| The build | [Actions run from a commit, and Dependabot moves them](#actions-run-from-a-commit-and-dependabot-moves-them) - every `uses:` by commit, GraalVM by Sokar's pin, checked by `check-actions` |
| The build | [What an update takes, and when](#what-an-update-takes-and-when) - three days old and still the newest, one pull request per run, a real model call |
| The build | [The bill generator is installed from its own lockfile, not resolved at build time](#the-bill-generator-is-installed-from-its-own-lockfile-not-resolved-at-build-time) - and neither it nor npm's cache is shipped |
| The build | [NullAway comes from `sokar-parent`](#nullaway-comes-from-sokar-parent) - one compiler configuration for every repository; `.mvn/jvm.config` stays here |
| The build | [No check requires a changelog entry](#no-check-requires-a-changelog-entry) - requiring one belongs to Sokar's changelog check, on logchange |
| Accepted risk | [The Node runtime digest was first read from the service that serves it](#accepted-risk-the-node-runtime-digest-was-first-read-from-the-service-that-serves-it) - reviewed and pinned, and the built runtime is asked its version |

## The endpoint is set by a file, not a variable

Pi cannot be pointed at a broker with an environment variable: its endpoint comes from an extension
it auto-discovers. That is the measured reason an agent definition declares **what shape of
endpoint it can address** rather than Sokar assuming a variable exists - a distinction the contract
makes for every agent because of this one.

Verified against OpenRouter and against GitHub Copilot, whose models use three APIs under one base
(below).

**What the container gets is one file**, `/home/agent/.pi/agent/extensions/sokar-route.ts`, written
by `PiRoutingExtension`: a `pi.registerProvider(<provider>, { baseUrl, apiKey })` call whose key is
the task's token, not the credential. No environment variable carries either. Three things about it
are load-bearing:

- **It is a file, not a variable.** Only Azure has a base-URL variable in Pi; every other provider's
  endpoint is fixed unless an extension overrides it. Extensions are auto-discovered from
  `~/.pi/agent/extensions/*.ts`, so this is something to place rather than something to run. It is
  also written after the container exists, by which time its environment is already fixed.
- **The dialect's path belongs to the provider, not to Pi.** OpenRouter serves the OpenAI dialect
  under `/api/v1`; a base URL without it answers 404, and one ending in `/v1` answers "model not
  found" to a client that appends its own version segment. The path arrives on the endpoint Sokar
  hands over, from the provider's definition.
- **The address is literal, not `localhost`.** Node resolves `localhost` to `::1` first and the relay
  binds IPv4, so `localhost` is a connection refused.

## The broker is reached through a relay in the task's namespace

Pi can only address a URL. A host-side listener is either unreachable from the container or bound to
every interface - measured on one machine:

```
host itself                                : 200
container -> host loopback via 169.254.1.2 : refused
container -> its own 127.0.0.1             : refused
```

So the **listening end** moves into the task's network namespace: `sokar vault relay` binds
`127.0.0.1` there and forwards to the broker's unix socket on the host. It resolves nothing and
connects to nothing but a local file. When `vault.log` shows no request at all, `relay.log` beside it
says whether Pi reached the relay.

**The broker itself stays on the host.** Put in the namespace, it keeps the host's mount namespace,
so it reads the host's `/etc/resolv.conf`, finds a resolver that does not exist there, and every
request fails as "could not reach the provider". Its egress would also be governed by the task's own
firewall rather than the host's.

## What is proven about brokering this agent

Brokering is verified **against OpenRouter and against GitHub Copilot**, and for nothing else. How
many providers this agent supports is not verified here, which is why no figure is given.

**GitHub Copilot, measured on 0.85.0** with a real Copilot account, through Sokar on a VM:
`--provider github-copilot --model gpt-5-mini` answered, through the broker's `/responses`, and the
container held only the task's token. So Pi carries a subscription reached through a browser sign-in
too:

- **The sign-in stays on the host.** The GitHub token comes from GitHub's device flow, granted once by
  a person with `sokar vault authorize github-copilot`; Pi's own `/login` is never run, so its sign-in
  flows never refresh a token inside the container. Pi reads the task's token from
  `COPILOT_GITHUB_TOKEN`, the variable Sokar's provider names.
- **Redirection holds beyond the common dialect.** Copilot's models use three APIs in Pi's catalogue -
  `anthropic-messages`, `openai-completions`, `openai-responses` - all under one base, which the
  broker's endpoint replaces.
- **The model has to be one Pi lists for Copilot.** A bare id picks the provider the task is
  authenticated for, so `gpt-5-mini` alone works. `gpt-4.1` is not in 0.85.0's Copilot catalogue: Pi
  then guesses `/responses` for it, and Copilot answers 400.

## Pi does not check for a newer version in a task

**Read in the pinned 0.85.0, not run:** at start, Pi asks `https://pi.dev/api/latest-version` and
at most shows *"Update Available"* with the command to run. It installs nothing by itself. `pi update`
would need pi.dev and the npm registry, and this definition allows no host but the provider's, so
in a task the check can only fail.

**How it is stopped:** the launcher `/usr/local/bin/pi` exports `PI_SKIP_VERSION_CHECK=1` before it
starts Pi. Pi has no setting for it, only the variable. Not `PI_OFFLINE`, which also switches off
other things Pi fetches.

**Telemetry is off the same way:** the launcher exports `PI_TELEMETRY=0`, under which Pi skips its
install ping to `pi.dev` and the attribution headers it would add to OpenRouter requests. And the
definition lists `pi.dev` under `refused_domains`, so the host is refused by policy rather than
merely absent from what is allowed.

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
within its bound. Both pass on the acceptance VM. **Why the check can fail:** with a workspace carrying a
skill and `--no-approve` left off, Pi's trust question is up, and the bytes it has written by then -
read while the question was on screen - hold the question and not the header: Pi draws its prompt
only after the question is answered. The kit's own failing step was not run for this, because its
fixture project carries no skill.

## Waiting for a person is declared as unknown, because it is

Sokar tells a person an agent is waiting for them from a declaration in the agent's own YAML: literal
lines on the attached screen that only a question draws. Pi has none. Measured on 0.85.0 at a terminal:
asked to put a question and wait, it writes the question as plain text, and the rest of the
screen is its idle screen byte for byte; the window title stays `π - <directory>` in every state.

**So `pi.yaml` declares nothing, and Sokar says "cannot say".** A rule written anyway would match
nothing and make Sokar say "not waiting" - telling a person no question is there when one may be. The
acceptance scenario `waiting.feature` proves the answer Sokar gives is "cannot say", on the VM against
Sokar 196; `WaitingDeclarationTest` fails the day a declaration appears, so it cannot appear without
the measurement and the scenario that drives Pi to a question.

**Pi's own extension does see its dialogs** - `PiStatusExtension` writes a state file around every
prompt - but Sokar reads the screen, not files an agent package places, and a model's plain-text
question is not a dialog either.

**What would change the answer:** a Pi release that draws something only a question shows, or Sokar
reading a signal an agent package provides.

## Three projects, two names

`earendil-works/pi` is this agent: the Pi Agent Harness, published as
`@earendil-works/pi-coding-agent`, run as `pi`. **Oh My Pi is a fork of a different Pi** by a
different author, published as `@oh-my-pi/pi-coding-agent` and run as `omp`. The names invite
confusing them, and work done against one is not work done against the other - so a requirement,
an issue or a measurement names which one it means.

## A task that comes back continues its conversation

Sokar records the session a task's agent ran and passes it back with `--session` when the task starts
again. Where the id is, `pi.yaml` declares under `session.session_id`. Measured on 0.85.0: an unattended
run's first record, `{"type":"session"}`, carries it as `id`, and `--session <id>` continues it. An
attached session is kept as `~/.pi/agent/sessions/--workspace--/<timestamp>_<id>.jsonl`. Its name is not
the id, and `--session` does not find the session by it; its first record is the same `{"type":"session"}`,
and Sokar reads the id from inside the newest session file.

**Proven at the version it pins.** `SessionIdDeclarationTest` reads the declaration against the measured
records. `session.feature` drives it through Sokar on a VM: unattended, a word to remember in one run and
given back in the next although the second prompt never names it; attended, a message, the task stopped
and started again, and the earlier message back on its screen.

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


## A release is built from releases only

Between releases everything is built on snapshots: `sokar.version`, the parent, and with the parent the release
tooling. A tag's build takes none of them.

- **Nothing from Central's snapshots on a tag.** The snapshot repository is a profile in `settings.xml` that
  switches itself off when `sokar.release` is set, and `build.yml` sets `MAVEN_ARGS` to `-Dsokar.release` on a tag
  and to `-U` elsewhere. Measured: with it, a build from an empty local repository cannot resolve the snapshot
  parent; without it, it does. Not `-P!standard`: Maven then lists the profile as inactive and still takes the
  parent from its repository, also measured.
- **Nothing asked for again on a tag.** `-U` is in no command; `MAVEN_ARGS` carries it where it belongs, also into
  the `pinned-jdk` action.
- **A tag on a snapshot is refused.** "Which channel" refuses a `sokar.version` that names a snapshot, in the build
  job and in the release job. `ReleaseChannelTest` runs both steps as a tag's run would, sees the refusal, and sees
  a released Sokar let through; it was seen to fail with the refusal taken out. `check-releases` then refuses any
  snapshot left in the effective pom.
- **Secrets only where a tool needs them.** The tooling is resolved and the channel decided in steps that hold no
  secret. The steps that hold one run only what needs it: the acceptance legs and their clean-up the cloud's key and
  the provider's, publishing the repository's token.
## Actions run from a commit, and Dependabot moves them

Every `uses:` names a full commit with its release beside it, `@<commit> # vX.Y.Z`. A tag is a name its
owner may point anywhere, and these jobs hold the publishing token, the machine credentials and a
token that merges pull requests. GitHub's own actions are held to the same rule: the argument does not
depend on who publishes the action.

**Dependabot keeps the pins current**, in every repository. A
pin nobody moves rots, and a stale action with a known flaw is not safer than the current tag. Each
release arrives as a pull request with the new commit and its version, and nothing merges it
automatically: the review is what the pin buys.

**`sokar-release check-actions` fails the build on a step that names a tag, a branch, a bare hash or a
line of releases like `# v7`**, and says how to pin it, so a new workflow cannot bring a tag back. It is
Sokar's one check for this rule, run in the build job, and this repository keeps no second test for it.
A step of this repository and an image by digest pass. Measured against this repository's workflows,
with a tag, a `# v7`, a `setup-graalvm` at a commit and a Dependabot without `/.github/actions/*` each
put in: every one refused.

**Dependabot watches the local actions too, and waits three days.** `dependabot.yml` lists
`/.github/actions/*` beside `/`, because the pinned-JDK action pins its cache, and a Dependabot that
watches only the workflows never moves that pin. A release is taken after three days, as every other
pin here, and the week's moves come as one grouped pull request. `check-actions` fails when the local
actions are not watched.

**`mvnw` checks the Maven it downloads**: `distributionSha256Sum` in `.mvn/wrapper/maven-wrapper.properties`,
for 3.9.15 the digest Apache's own SHA-512 confirms. A wrong one stops the wrapper before Maven runs.

**Nothing else in these workflows is fetched by a name**: the only `curl` is `jf rt curl` reading this
product's own Artifactory. The setup actions download their tools themselves, and there the two differ:

- **The JFrog CLI is fixed by the action's commit.** `setup-jfrog-cli` 5.2.0 defaults to `jf` 2.124.0
  and asks for the newest only when told to, so a Dependabot bump moves both together, under review. It
  checks no digest, which is the same publisher's trust as the action.
- **GraalVM is the one Sokar pins, checked against its digest.** `setup-graalvm` with `'25'` resolves
  the newest 25.x at run time and checks nothing for community builds - and that JDK compiles the
  native binary this repository publishes. So every job uses `./.github/actions/pinned-jdk`: the
  runner's own Java 25 runs `sokar-machines jdk --github` once, which installs the GraalVM
  `sokar-machines` pins (`machines.graalvm.*`, moved by Sokar's Machines workflow under the same
  three-day rule), checks its digest before unpacking, and sets `JAVA_HOME`. The build and the
  acceptance machines use the same pin, by construction. `check-actions` refuses `setup-graalvm` and
  `setup-java` even at a pinned commit, because the commit fixes the action, not the JDK it fetches.
  Measured on a VM as a stand-in runner - an empty home, Temurin 25 in place of
  the runner's Java, Sokar's artifacts at `0bce03a`: GraalVM 25.0.2 installed and checked, and this
  repository's native build and packages made with it.

## What an update takes, and when

Sokar's release tool applies these rules; `UpdateRulesTest`
fails when this repository stops asking for them.

**A release is taken once it is three days old, and still the newest**: `sokar.release.min-age` is
`3d`. A release withdrawn or patched within days never becomes a pull request, and the people who
install on the first day get the days to report what breaks. A younger release is not skipped for the
one before it: the job waits and says until when. A release whose date cannot be read is never old
enough.

**A pin move bumps the package's patch version**, `0.4.1-SNAPSHOT` to `0.4.2-SNAPSHOT`, so a package
version names what it installs and `apt` sees an upgrade.

**The verifying tier makes a real model call**, with its key in the workflow's secrets - intended, not
temporary. Published metadata proves the download is intact; only a real request proves the new
version still starts without a question, reads its credential variable, routes through the broker and
gets an answer.

**The CI machines are not kept current here.** GraalVM and the pre-pulled base images are Sokar's
machine tooling, under the same rule, once for every repository.

**Node, `fd` and `ripgrep` are followed by the same job, under the same rule.** Each is declared to the tool as `sokar.release.pin.<name>.*`: where a release is read - Node on its LTS
line 22, the two tools from their newest GitHub release - where its digest is read, and the properties
a move writes. A Node move also rewrites `sokar.release.npm.image`, which spells out its version and
image digest, so the relock image cannot part from the builder. `UpdateRulesTest` fails when a tree pin
is not declared to the tool, not followed by the job, or not offered when the job is dispatched.

**One run, one pull request, one commit per move.** A request per item, each verified on its own,
would build the tree and rent machines once per item, one after another, and the requests would
conflict on the module version and the changelog. So `buildtools/follow-upstream.sh` asks about all four, commits each move separately, and the job
verifies the result once. The cost is that one questionable move holds up the others; the commits are
what answer it - a person drops one and lets the build run again. The request lives on one branch,
`update/pi`, so a run while one is still open replaces it rather than opening a second. **The CLI
moves last**, because moving it regenerates the lockfile in the Node image the pom names, and that has
to be the one this run moved Node to.

**Measured against the tool with named pins:** Node 22.20.0 to 22.23.3 wrote the
digests Node's `SHASUMS256.txt` and Docker Hub give; `fd` and `ripgrep`, moved one release back and
forward again, came back to exactly the pinned digests; an asset pattern matching two files was refused.

**What would change the answer:** a release that cannot wait three days. Dispatching the job with the
version named takes it at once - that is the way round the rule, not a change to it.

## `build-pi-tree.sh` stays a shell script

The build is Java and Maven, and a file that is not says why. This one runs `npm ci` and the bill
generator inside a builder container pinned by digest through podman, validates the pins Maven passes
in and its own overrides, and packs the tree. Every step is a process call; a Java version would make the same calls
with more lines and hide them behind a process API. What matters in it is its input guards.

**Its regression matrix.** Each of these is refused with exit 2, executing nothing:

    NODE_VERSION='22.20.0; touch /tmp/pwned'
    NODE_VERSION='22.20.0$(id)'
    NODE_VERSION="'; rm -rf /out; '"
    NODE_SHA256=deadbeef
    PI_BUILDER_IMAGE='node:22; id'
    PI_BUILDER_IMAGE=node:22-slim       (a tag alone, no @sha256: digest)
    NODE_IMAGE_DIGEST=deadbeef
    NODE_IMAGE_DIGEST=<the 64 hex digits without sha256:>
    JAVA_CMD='java; id'
    FD_SHA256=deadbeef
    RG_VERSION='15; id'
    any pin unset (the script keeps no default)

**What would change the answer:** the tree build needing logic that is not a process call - parsing,
merging, deciding - which belongs in Java rather than grown into this file.

## `follow-upstream.sh` is a shell script for the same reason

It calls Sokar's release tool once per item, commits, and writes a table: process calls and `git`,
with no decision of its own - what is asked, how old a release must be and what a move writes are the
tool's and the pom's. It is a script rather than workflow steps so it runs on a workstation too, where
it was measured against the published tool: all four followed (Node and Pi moved, in that order, the
Pi lockfile then resolved in the new Node image), one of them with a version by hand, and, through a
stand-in tool, an upstream that could not be asked, one older than the pin, and a move that failed
halfway - undone completely, the others still moved.

**Its regression matrix.** Each of these is refused with exit 2, executing nothing:

    WHAT=bogus
    WHAT=all VERSION=1.2.3          (one version for all of them)
    VERSION='1.2.3; id'
    CHANNEL='latest;id'
    SOKAR_RELEASE=relative/path
    a working tree with changes     (every move is committed)

## The bill generator is installed from its own lockfile, not resolved at build time

The CycloneDX generator has its own `buildtools/sbom/package.json` and `package-lock.json`, is
installed with `npm ci --ignore-scripts`, and the directory is removed before anything is packaged.
So every byte of the generator is checked against a recorded hash, no install script runs next to
the tree that is about to be shipped, and the tool is a build input rather than part of the package.
Fetched with `npx --yes` instead, the tool that inspects the dependencies would be the one thing in
the build not pinned by a lockfile, running beside a writable payload.

**The tree's own install takes `--ignore-scripts` too.** A lifecycle script runs after the integrity
check, with the shipped tree writable, and Pi needs none: of the three packages that declare one,
`@google/genai`'s preinstall is a no-op, `protobufjs`'s postinstall prints a warning, and `esbuild`'s
postinstall copies a binary its JS API finds without it. `PinAgreementTest` fails on an `npm ci` in
`build-pi-tree.sh` without the flag.

**The cost, stated plainly:** a second lockfile to keep current. It is pinned deliberately - the
generator's version decides what the bill looks like, and that should change in a reviewed commit
rather than on the day the registry serves something newer.

## NullAway comes from `sokar-parent`

The compiler configuration that runs NullAway - Error Prone with only NullAway, `OnlyNullMarked`, the
two processor paths - is `sokar-parent`'s, managed for every Sokar repository, so this pom declares no
compiler plugin. Only the `default-compile` execution runs it: tests pass `null` on purpose, and Error
Prone never sees them.

What stays here is `.mvn/jvm.config`: Error Prone runs inside javac in Maven's own JVM, and from JDK 16
on that JVM refuses it the compiler's internals - measured on JDK 25, an `IllegalAccessError` on
`com.sun.tools.javac.api` before a single file is checked.

## No check requires a changelog entry

The changelog is written by hand in the same commit, and nothing enforces it. Sokar is moving to
logchange - one YAML file per change, and a generated `CHANGELOG.md` - and a check for a hand-kept
file would have to be rebuilt the moment that reaches this repository. Requiring an entry returns as
Sokar's changelog check, proposed to logchange upstream first, keeping three lessons: a waiver answers for its own
commit only, documentation is not exempt, and a range that cannot be compared fails.

**What would change it:** Sokar's changelog check landing, or logchange being adopted here.

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
`SHASUMS256.txt`) in the build or the pin check.
