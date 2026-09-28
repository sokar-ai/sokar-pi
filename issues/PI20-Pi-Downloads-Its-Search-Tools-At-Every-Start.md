# PI20 — Pi downloads its search tools at every start

**Priority:** 2
**Opened:** 2026-09-28
**Source:** measured while checking what a fresh task shows before Pi starts work

## What

Every start in a task prints `fd not found. Downloading...` and `ripgrep not found. Downloading...`,
then `Failed to download ... getaddrinfo ENOTFOUND github.com`. A task cannot resolve github.com, so
Pi runs without both tools, on 0.85.0. **Not measured:** what Pi does instead - a slower fallback, or
file search and grep tools that fail when the model calls them.

## What would close it

- Measured: what Pi's find and grep tools do without `fd` and `ripgrep`.
- Then either both binaries are in the image, pinned and in the bill like the rest of the tree, or a
  measurement says Pi works without them and the warning is recorded as harmless.
- Either way no start tries to reach github.com.

## Open question

Whether the binaries belong in the tree this package ships, or in the image's own packages.
