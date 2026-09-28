# PI19 — The tree follows where the agent is installed

**Priority:** 2
**Opened:** 2026-09-28
**Source:** Agent Coordinator's QS1, while the agents move to a copy per account on the shared VM

## What

`src/main/resources/agent/pi.yaml` names the tree it copies into the image by absolute path:
`packaged.source: /usr/share/sokar/agents/pi/pi-tree.tar.gz`. An account that installs its own copy
of the agent - into `~/.local/share/sokar/agents`, which Sokar reads before the machine's directory -
still gets **the machine's tarball**, so a test build of the adapter would run beside whatever tree
the machine-wide package last installed, and nothing would say so.

It is the only host path this agent reads at run time, measured 2026-09-28: every other absolute
path in `src/main` is a path inside the image. `sokar-claude-code` and `sokar-omp` read none - the
Claude adapter has run from `smith`'s own agents directory all day.

## What would close it

- The tarball is found beside wherever the agent binary was found, for the machine-wide install and
  for a copy in an account's own directory alike.
- Measured in an account with its own copy and a tree that differs from the machine's: the image gets
  the account's tree.

## Waits on

**Sokar `1b676e9` in the snapshot this repository builds against.** Answered by Agent Core on
2026-09-28: Sokar did not resolve a relative `source` at all - the agent API accepted only an
absolute path - and a declared tree that was missing did not stop the task; it built the image
without Pi and said so in one line. `1b676e9` reads a relative `source` beside the agent's binary,
refuses one containing `..`, and refuses a task whose declared tree is missing. It was built
without a requirement number, so this names the commit; until its snapshot is on Central a
relative `source` here would not load.

**Then, decided here:** the package puts the tarball beside the binary -
`/usr/libexec/sokar/agents/pi/pi-tree.tar.gz` - and `pi.yaml` names it as `pi/pi-tree.tar.gz`.
