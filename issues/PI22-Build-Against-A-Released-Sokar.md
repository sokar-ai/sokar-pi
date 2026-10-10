# PI22 — Build against a released Sokar

**Status:** now.

**What must be true.** A release of this adapter is built and accepted against a released Sokar, so
nothing it ships or runs with publishing, cloud or provider secrets comes from a mutable snapshot.
Releases stay rare; this is about what a release is built from, not when it is made.

## Why

Between releases everything is built on snapshots: `sokar.version` is `0.4.2-SNAPSHOT`, and the parent
`0.1.4-SNAPSHOT` brings the release tooling's `0.4.5-SNAPSHOT`. They are resolved from Central's
snapshot repository with `updatePolicy=always` and `-U`, and the build and acceptance jobs run
`sokar-release` and `sokar-machines` from it in jobs that also hold publishing, cloud and provider
secrets. A snapshot is neither immutable nor signed.

What holds already: on a tag, "Which channel" refuses a `sokar.version` that names a snapshot, and
`check-releases` refuses any snapshot in the effective pom, so no release is built from snapshot code.

## Acceptance

- On a tag, the snapshot repository and `-U` are gone from the release path; tools, parent and
  `sokar.version` resolve from releases only.
- Where a tool can run in a step without secrets, it does.
- A tag built while `sokar.version` names a snapshot is seen to be refused, in a test.
