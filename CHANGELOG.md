# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Headings name this package's version; Pi's version is what the package ships and appears as an
entry rather than a heading. One sentence per change - `git log` has the detail.

## [Unreleased]

### Removed

- The Python release tools and `acceptance.sh`, which had drifted between the three agent repositories.
- The changelog check in CI; requiring an entry returns later, built on logchange.

### Changed

- Node pinned to 22.23.3 (was 22.20.0).
- The vault scenario creates its vault before unlocking it, since Sokar now refuses to unlock one that does not exist.
- Every GitHub Action runs from a pinned commit with its release beside it, Dependabot proposes the moves, and the build refuses a step named by tag.
- The update job takes a release once it is three days old, and follows Node, `fd` and `ripgrep` beside Pi, each in its own pull request.
- The Node builder image's digest is written with its `sha256:`, as the release tool reads and writes it.
- The Pi tree's pins - the Node runtime, its builder image, `fd` and `ripgrep` - are pom properties, and `build-pi-tree.sh` keeps no default of its own.
- The Pi tree installs beside the agent binary and is named relative to it, so a copy of the agent in one account's own directory ships that account's tree.
- The tree build records the Node runtime in its bill with Sokar's release tool, so no build step needs `python3`.
- The build and the update job call Sokar's release tool instead of Python: merging the tree's bill, the upstream lookup, the pin move with its relock, and the bill comparison.
- The acceptance run is the Cucumber scenarios alone; they refuse to run as root, bring a vault of their own, and check that the adapter ships what its bill names and that a live task's broker saw the request.
- The pull request carries the third-party comparison itself, with each component's licence, rather than leaving it in the run log.
- Issues carry their repository's letter: `PInn` rather than `0nn`, so a number says which set it belongs to.
- The acceptance suite makes its project by following a local repository and names it on every task start; its cleanup asks Sokar to remove the project instead of deleting Sokar's directories.
- Every task the acceptance suite starts names its repository, which Sokar now requires.
- The pin check is a unit test instead of a Python script: the definition, the pom, `package.json` and the lockfile must name one version, and the Node runtime must not be overridden.

### Security

- The tree is built in a Node image pinned by digest, not by a tag its owner could repoint; npm ci and the Node checksum run inside that image.
- Pi no longer asks for a newer version at start inside a task; the check could only fail there.
- The acceptance suite no longer puts the test credential on a command line; the pattern reaches `grep` on a file descriptor.
- The check that no log holds the credential also searches with line breaks removed, so a value split across a newline is found rather than reported as absent.
- The acceptance run passes the model name to the container as data rather than inside a shell command.
- The build script validates every value it accepts from the environment and passes them as data, so a version or image name carrying shell syntax is refused rather than executed.
- The bill generator is installed from its own lockfile with scripts disabled, and neither it nor npm's cache is shipped any more - 46 MB less in the package.
- The pin check also answers for the Node runtime: the reviewed defaults, no environment override, and the runtime that was actually built.
- The CI no longer installs the unpinned Hetzner Python client; nothing had used it since the Java machine tooling replaced it.

### Added

- The acceptance run fails when anything comes before Pi's prompt, attended or unattended, not only the dialogs already known.
- The package carries `fd` and `ripgrep`, pinned by digest and in the bill, so Pi no longer tries to download them from GitHub at every start and its `@`-file autocomplete works.
- The build refuses a main package that is not null-marked, so NullAway cannot skip one in silence.
- The compile checks the package's nullness contract with NullAway, so returning null where a type promises a value fails the build.
- The build refuses an issue number that names no issue, so a citation in prose cannot outlive the issue it points at.
- The build refuses a link to an issue file from anywhere but the index, so a pointer cannot outlive the issue it names.
- The packages provide `sokar-agent`, so the setup script and the daemon list this agent as one a person can choose.
- An extension that records whether Pi is working, idle or waiting for a person, written into the container and read by the host.
- The Pi adapter: definition, credential handling, headless commands. Its log is shown as Pi writes it; there is no formatter for it yet.
- `.deb` and `.rpm` packages, published to Artifactory from `main`.
- Pi 0.85.0 and a Node runtime, built into a tree from a lockfile and shipped inside the package.
- A CycloneDX bill of materials in every package, merging the Maven graph and the npm tree.
- An acceptance suite against the published packages on Ubuntu and Fedora, with a tier that authenticates for real.
- Weekly automated updates, verifying a new version on both distributions before anything is published.
- `buildtools/upstream-version.py`, `buildtools/update.py` and `buildtools/check-pin.py` for that pipeline.
- `buildtools/check-changelog.py`, failing a code change that does not say what changed.
- This changelog.

### Fixed

- The check that the shipped Node runtime reports its pinned version runs in every package build instead of skipping, and asks the shipped `fd` and `ripgrep` theirs too.
- With one prompt open over another, the status file names the prompt still waiting once the inner one closes, rather than the one that closed.
- The native binary starts on any x86-64 CPU; it needed AVX2, so on a pre-Haswell host or a VM with a conservative CPU model the package installed and then would not start.
- The packages declare the glibc (2.34) and zlib the native binary links against, and the build fails when the binary needs more.
- The build's index check follows Artifactory's redirect to cloud storage; it had read every package as not indexed.
- The agent is registered in `META-INF/services` like the other two, which was an empty directory; only in-process discovery read it, so no shipped package was affected.
- The routing extension is written only when there is a token to put in it, not merely an endpoint.
- Pi starts with `--no-approve`, so a repository's `.pi` directory neither asks a trust question inside a task nor reconfigures the agent.
- The update job stops instead of rolling back when upstream offers an older version than is pinned, and compares versions as numbers rather than text.
- The changelog waiver answers for the commit it is written on, rather than for everything pushed with it.
- The changelog check asks whether an entry was added, not whether `CHANGELOG.md` was touched; a rewritten link line used to satisfy it.
- The changelog check reads the waiver after fetching the base commit, not before; in a shallow clone it missed `[no changelog]` and failed the build anyway.
- The changelog check no longer exempts documentation; a typo says `[no changelog]` like any other change that ships nothing observable.
- The acceptance suite refuses a run as root, rather than passing every check but the one that needs the broker.

[Unreleased]: https://github.com/sokar-ai/sokar-pi/commits/main
