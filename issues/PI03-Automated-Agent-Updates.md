# PI03 — Automated agent updates: what is still undecided

**Priority:** 2
**Opened:** 2026-09-12
**Source:** handed over from Sokar requirement **A02**, 2026-09-12. The requirement covered all
three agent repositories; this is this repository's share of it.
**Depends on:** the same issue in `sokar-claude-code` and `sokar-omp` - any rule agreed here should be the same rule there,
or the divergence should be deliberate.

## What is already built here

Detect, apply, verify, publish all exist, as commands of Sokar's release tool: `upstream-version`
reads the npm dist-tag, `update` moves the pin and regenerates the lockfile, the unit test `PinAgreementTest`
refuses a disagreement between the four places that name a version - and also refuses a Node
runtime that is not the reviewed one - and `compare-bills` stops a release that
changes what third-party code ships.

So this issue is not "build the pipeline". It is the set of questions the pipeline still answers by
convention rather than by a stated rule.

## What is still open

- **Can the verifying tier's credential live in CI?** Without it the automation checks less than a
  person does by hand, which is a worse gate wearing the appearance of a better one.
- **What version does the agent package take when only the tool it installs moved?** The two were
  separated deliberately. A bot needs a stated rule rather than a guess.
- **Is the pointer this repository follows the right one?** It follows the npm dist-tag `latest`, because there is no `stable`. Nothing states how old a release must be before it is picked up.
- **How do the CI snapshots get refreshed when GraalVM or the base image moves?** The machines pin
  GraalVM by digest and pre-pull base images, so following an upstream release there means
  rebuilding an image rather than editing a version.
- **The Node runtime and the Pi CLI move independently.** The runtime's version and digest are now
  checked, but nothing decides *when* to follow a Node release. That is a second pin with no
  detector.

## What would close it

Each question answered in `doc/decisions.md` with its reasoning, and where the answer is a rule the
pipeline must keep, a check that fails when it is broken. An answer that lives only in a person's
head is what this issue exists to remove.
