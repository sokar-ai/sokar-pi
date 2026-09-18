# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Headings name this package's version; Pi's version is what the package ships and appears as an
entry rather than a heading. One sentence per change - `git log` has the detail.

## [Unreleased]

### Removed

- The changelog check in CI; requiring an entry returns later, built on logchange.

### Changed

- The pin check is a unit test instead of a Python script: the definition, the pom, `package.json` and the lockfile must name one version, and the Node runtime must not be overridden.

### Security

- Pi no longer asks for a newer version at start inside a task; the check could only fail there.
- The acceptance suite no longer puts the test credential on a command line; the pattern reaches `grep` on a file descriptor.
- The check that no log holds the credential also searches with line breaks removed, so a value split across a newline is found rather than reported as absent.
- The acceptance run passes the model name to the container as data rather than inside a shell command.
- The build script validates every value it accepts from the environment and passes them as data, so a version or image name carrying shell syntax is refused rather than executed.
- The bill generator is installed from its own lockfile with scripts disabled, and neither it nor npm's cache is shipped any more - 46 MB less in the package.
- The pin check also answers for the Node runtime: the reviewed defaults, no environment override, and the runtime that was actually built.
- The CI no longer installs the unpinned Hetzner Python client; nothing had used it since the Java machine tooling replaced it.

### Added

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
