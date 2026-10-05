# PI22 — Build against a released Sokar

**Status:** now; blocked by sokar-project PJ18.

**What must be true.** A release of this adapter is built and accepted against a released Sokar, so
nothing it ships or runs with publishing, cloud or provider secrets comes from a mutable snapshot.

## Why

`sokar.version` is `0.4.0-SNAPSHOT`, resolved with `updatePolicy=always` and `-U`, and the build and
acceptance jobs run `sokar-release` and `sokar-machines` from it in jobs that also hold publishing,
cloud and provider secrets. A snapshot is neither immutable nor signed. "Which channel" already
refuses a tag while `sokar.version` is a snapshot, so no release is built from snapshot code.

## Acceptance

- `sokar.version` names Sokar's release, and the snapshot repository and `-U` are gone from the
  release path; a tag built while it names a snapshot is seen to be refused.
- Where a tool can run in a step without secrets, it does.
