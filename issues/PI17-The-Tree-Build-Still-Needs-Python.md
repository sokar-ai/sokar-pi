# PI17 — The tree build still needs Python

**Priority:** 1
**Opened:** 2026-09-27
**Source:** found while replacing the Python release tools with Sokar's shared tool, 2026-09-27

## What

`buildtools/build-pi-tree.sh` appends the Node runtime to the tree's bill with an inline
`python3 -` heredoc, on the build machine rather than in the pinned container. So the package build
still needs `python3`, although no Python file is left in the repository and `pom.xml` names none.
`sokar-project` PJ06 says no build or workflow in a Java repository needs `python3`.

**What the step does:** it adds one component - `node`, the version and the SHA-256 the script
already verified, the MIT license - to `target/tree/pi/sbom.cdx.json` before the bill is copied
out. Recording the digest that was checked, rather than one computed afterwards, is the part to keep.

## What would close it

- The step runs without `python3` on the build machine: inside the builder container, whose Node
  is already there, or as a command of Sokar's release tool if Agent Sokar sees one that fits.
- The merged bill still names `node` with the verified digest, proven by building the tree on a
  machine with podman - this one has none - and reading the bill.
- `python3` appears nowhere in the build.

## Open question

Node inside the container, or a step of the shared tool. The first touches only this repository; the
second is a question for Agent Sokar, since `sokar-omp` has no tree and would not use it.
