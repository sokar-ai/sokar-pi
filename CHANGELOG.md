# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Headings name this package's version; Pi's version is what the package ships and appears as an
entry rather than a heading. One sentence per change - `git log` has the detail.

## [Unreleased]

### Added

- The Pi adapter: definition, credential handling, headless commands, log formatting.
- `.deb` and `.rpm` packages, published to Artifactory from `main`.
- Pi 0.85.0 and a Node runtime, built into a tree from a lockfile and shipped inside the package.
- A CycloneDX bill of materials in every package, merging the Maven graph and the npm tree.
- An acceptance suite against the published packages on Ubuntu and Fedora, with a tier that authenticates for real.
- Weekly automated updates, verifying a new version on both distributions before anything is published.
- `buildtools/upstream-version.py`, `buildtools/update.py` and `buildtools/check-pin.py` for that pipeline.
- `buildtools/check-changelog.py`, failing a code change that does not say what changed.
- This changelog.

### Fixed

- The routing extension is written only when there is a token to put in it, not merely an endpoint.
- Pi starts with `--no-approve`, so a repository's `.pi` directory neither asks a trust question inside a task nor reconfigures the agent.
- The update job stops instead of rolling back when upstream offers an older version than is pinned, and compares versions as numbers rather than text.
- The changelog waiver answers for the commit it is written on, rather than for everything pushed with it.
- The changelog check asks whether an entry was added, not whether `CHANGELOG.md` was touched; a rewritten link line used to satisfy it.
- The changelog check reads the waiver after fetching the base commit, not before; in a shallow clone it missed `[no changelog]` and failed the build anyway.
- The changelog check no longer exempts documentation; a typo says `[no changelog]` like any other change that ships nothing observable.
- The acceptance suite refuses a run as root, rather than passing every check but the one that needs the broker.

[Unreleased]: https://github.com/sokar-ai/sokar-pi/commits/main
