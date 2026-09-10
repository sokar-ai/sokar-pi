# Building

Nothing here needs a checkout of [Sokar](https://github.com/fuinorg/sokar). This
repository compiles against the **published agent contract** - `sokar-agent-api`
and `sokar-wire` - resolved from Maven, and that is the property worth keeping: if
a build here ever needs the Sokar sources, the split has been undone without
anyone deciding to undo it.

```
./mvnw -s settings.xml verify                     # unit tests
./mvnw -s settings.xml -Pnative,dist verify       # + the binary, the .deb and the .rpm
```

`-s settings.xml` is not optional while the contract is a snapshot: it declares the
repository the snapshot comes from. The native build needs GraalVM as `JAVA_HOME`.

What the second command leaves in `target/`:

```
sokar-agent-pi                              the adapter, run by Sokar on the host
pi-tree.tar.gz                              Pi and a Node runtime, ~73 MB
sokar-agent-pi_1.0.0~SNAPSHOT_amd64.deb
sokar-agent-pi-1.0.0~SNAPSHOT-1.x86_64.rpm
```

**`-Pdist` needs podman**, because it builds `pi-tree.tar.gz` inside a pinned Node
container rather than on the build machine - so no Node toolchain has to be installed
here, and the toolchain is a stated version rather than whatever the machine has.
Without podman the script says so and skips, and the package is then incomplete.

That tree is why this build takes minutes where a single-binary agent takes seconds,
and why the package is about 79 MB. It is shipped rather than fetched so a task image
builds with no network access at all, and so the same package cannot install different
bytes on different days: `npm ci` against a lockfile that pins every dependency by
integrity hash, verified once here rather than on every image build.

**The package version is this agent's own**, not Sokar's. An agent released against
an unchanged CLI is still an upgrade, and a Sokar release does not move it. `~`
rather than `-` before `SNAPSHOT` because dpkg and rpm sort `~` below everything;
left as `-SNAPSHOT` it would sort *above* the release and apt would refuse the
upgrade.

The Pi version the image installs is carried in the package description
instead, so `dpkg -s sokar-agent-pi` and `rpm -qi` still answer it.

## Bumping the Pi version

Three places, and the lockfile is the one that is easy to forget:

1. `agent.cli.version` in `pom.xml` — filtered into `pi.yaml` and carried in the package
   description, so `dpkg -s` and `rpm -qi` answer which Pi an image runs.
2. The version in `src/main/npm/package.json`.
3. **`src/main/npm/package-lock.json`, regenerated** — that is what actually decides the
   bytes. Every dependency in it is pinned by integrity hash, and `npm ci` refuses to
   install anything that does not match, so the verification happens once here rather
   than on every image build.

```
cd src/main/npm && npm install --package-lock-only
```

Nothing is fetched at image-build time: the tree is inside the package, so a task image
builds with no network access at all. The Node runtime is pinned separately, by version
and SHA-256, at the top of `buildtools/build-pi-tree.sh`.

`sokar agents --supply-chain` reports what is pinned, so "which version ran" is
answerable from the installed adapter rather than from a build log.

## Publishing

A push to `main` uploads the two packages to Artifactory, into the **same repositories
Sokar itself publishes to** - `sokar-dist-deb` and `sokar-dist-rpm`. They belong
together: this package declares `Depends: sokar`, so split across repositories an
operator would have to configure both for the dependency to resolve.

A pull request builds and packages but publishes nothing.

Two repository settings are needed, the same ones Sokar uses: the variable `JF_URL`
(the platform url, **without** `/artifactory`) and the secret `JF_ACCESS_TOKEN`. The
token needs Read, Deploy/Cache, **Annotate** and **Delete** on both repositories -
Annotate because the Debian index is driven by properties, Delete because the snapshot
file name is stable and every build overwrites it.

`.github/workflows/artifactory-smoke.yml` checks all of that in about twenty seconds,
without building anything. Run it after rotating the token, or before wondering why a
publish failed.

## Acceptance

`buildtools/acceptance.sh` is the last step, and the only one that installs what an
operator installs. Everything before it proves the code is right; this proves the
**package** is. It runs on a stock Hetzner image that has never seen this project -
never a prepared snapshot - so it exercises the package repository itself: the
signature, the index, and `Depends: sokar` resolving from the same place.

`buildtools/ci/remote-acceptance.py` provisions the machine, installs from Artifactory
the way [the README](README.md#install) says, creates an unprivileged user (a task runs
rootless, so running the suite as root would prove less), runs the suite and destroys the
server in a `finally`. A `cpx12` is enough - one core and 2 GB, because this installs
packages and runs a single prompt.

Both distributions, because they differ in ways that have already caused bugs: the `.deb`
path and AppArmor on Ubuntu against the `.rpm` path and SELinux enforcing on Fedora. Ubuntu
26.04, not 24.04 - Sokar needs podman 5, and 24.04 ships 4.9.3 for the whole of its life.

Two halves:

- **install** - the packages install, `sokar agents` lists an agent it was never linked
  against, `sokar setup` registers the hooks. No credential needed.
- **tier 2** - a task authenticates against OpenRouter and completes a prompt, and the
  real credential is then searched for in the container's environment and in every log
  the run produced. Needs `SOKAR_E2E_OPENROUTER_API_KEY`; without it this half is skipped
  and the script still exits 0, so a fork or a revoked key loses coverage rather than
  turning the build red with no information.

**The model is set in the workflow**, `SOKAR_E2E_MODEL` in `.github/workflows/build.yml`,
rather than in a repository setting - so it is visible in the diff, reviewable, and changes
with a commit rather than silently. Each agent picks its own: what is measured is that the
credential was accepted, so the model only has to be cheap and able to follow one instruction.
The same variable overrides a local run.

It is `z-ai/glm-5.3-flash`, about $0.00002 a run, chosen by measurement:
a `:free` variant returned `rate_limit_exceeded` on three consecutive attempts. Note that
this model id reaches OpenRouter's
OpenAI-compatible dialect, which is the one Pi reaches - it speaks whatever its provider
speaks rather than a fixed format. Watch out for reasoning
models with a small token budget: at `max_tokens=64` the GLM models return empty text with
`stop_reason: max_tokens`, having spent the budget before saying anything.

Those last two checks are worth having even when authentication fails. A credential
scheme that authenticates by handing the real key to the agent has not failed loudly -
it has failed quietly, and only a real credential makes the leak searchable.

Run it by hand with `workflow_dispatch`, or locally:

```
REMOTE_BUILD=... SSH="$(cat key)" SOKAR_E2E_OPENROUTER_API_KEY=... \
  python3 buildtools/ci/remote-acceptance.py --os fedora
```

## What this repository still cannot check

That a real Pi CLI reaches only the hosts its definition declares. That is the
domain-coverage check, it needs the resolver log from inside a task, and it lives in the
Sokar repository as `buildtools/e2e-tier1.sh`.

## Acceptance, as a person at a terminal

`src/acceptance` holds Cucumber scenarios that drive a real machine over ssh - a pty for what a
person sees, no pty for what a script gets - through Sokar's published acceptance kit. They are
off unless a host is named, so an ordinary build neither resolves the kit nor compiles them:

```
./mvnw -s settings.xml verify \
    -Dsokar.acceptance.host=<machine with sokar and this agent's package installed> \
    -Dsokar.acceptance.user=acceptance \
    -Dsokar.acceptance.key=$HOME/.ssh/id_ed25519
```

The report lands in `target/acceptance.html`. The scenarios tagged `@credential` need
`SOKAR_E2E_OPENROUTER_API_KEY` and `SOKAR_E2E_MODEL` in the environment of the machine running the
suite - typed into the vault at a terminal, never on a command line - and are **skipped**, not
passed, without them. There is no glue class here: every step is the kit's, which is what keeps
this repository free of test code that knows about ssh.

In CI the same suite runs from the runner against the rented machine on every push to `main`,
beside `buildtools/acceptance.sh` until it has been green there for real; see the comment in
`.github/workflows/build.yml`. A run that produces no scenarios fails rather than passing quietly.
