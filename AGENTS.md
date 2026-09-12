# Rules for working in this repository

Short on purpose. A rule is here because somebody paid for learning it.

## Shared across the Sokar repositories

These are the same text in `sokar`, `sokar-frontend` and the three agent repositories. Change them
in the channel first, not in one copy.

- **The operator pushes. Agents commit and stop.** A push starts a build that costs metered minutes
  and can cancel one already running. Say what is ready and let him decide when.
- **Nobody edits another agent's repository.** Reading is fine and encouraged. If something of
  yours needs a change over there, ask in the channel and say why.
- **The channel is `~/.sokar/agent-channel.md` and it is append-only.** Read the entries written
  since your marker before you post, move your marker only past somebody else's entry, and never
  rewrite what is already there.
- **A secret never appears in a command line.** Not in `argv`, not in a container's command, not in
  a log. Environment variables and files with owner-only permissions, and standard input when the
  value must cross a machine boundary.
- **The test machines are shared.** Name what you remove rather than sweeping "what I do not
  recognise", and **announce a restart before you trigger one**. A reboot leaves no trace in the
  work it interrupts, so the person whose run it killed cannot find out what happened.
- **Say what a run does to a shared machine before starting it - what it does, not what you believe
  it does.** Check first. A confident wrong answer costs somebody else an afternoon.
- **Measure before you claim.** "It works" means it was run. "It is not the cause" means the
  counter-test was run too. A finding without a measurement is a guess wearing a fact's clothes.

## Work in this repository

- **Every open thing is an issue**, in `issues/`, named `NNN-Short-Title.md`, numbered in order and
  carrying a priority. Not a TODO in the code, not a note in a commit message.
- **Settled reasoning goes in `doc/decisions.md`**, including accepted risks. An accepted risk says
  what the exposure is, why it is not being removed, and what would change the answer.
- **Dot files and directories are not checked in.** `.gitignore` ignores `.*` and names the few
  exceptions a Java build needs - `.github`, `.mvn`, `.gitignore`, `.gitkeep`. A local-only note
  goes in `.AGENTS.md`, which is ignored by that rule rather than by being named.
- **The changelog is part of the change.** `buildtools/check-changelog.py` fails a code change that
  says nothing; `[no changelog]` in the commit message is for changes that genuinely alter nothing
  an operator would notice.
- **Comments say why, not what.** One-liners in config; in code, the reason a reader would
  otherwise have to reconstruct. No exhaustive prose inlined in source.
- **A test that passes both with and without the fix proves nothing.** Run it against the broken
  version before trusting it.

## What this repository is

The Pi adapter: an agent definition, the Java that shapes what Pi cannot express as data, the
TypeScript extensions placed inside a task container, and the packaging that ships Pi with its own
Node runtime. It depends on the published Sokar agent API and wire artifacts, never on Sokar's
implementation.

- `./mvnw -o -B -s settings.xml test` is the fast gate; `verify` adds the acceptance module.
- `buildtools/build-pi-tree.sh` builds what the package carries - Pi from a lockfile and a pinned
  Node runtime - inside a container, so no Node toolchain has to exist on the build machine.
  It needs podman and skips itself with a message when there is none.
- **Everything that build script may take from the environment is validated first.** Those values
  end up in commands running beside the tree that is about to be shipped.
- `buildtools/sbom/` is the bill generator with its own lockfile. It is a build tool: installed
  with `npm ci --ignore-scripts` and removed before anything is packaged. Nothing a build fetches
  belongs in the package.
- The agent's own facts live in `src/main/resources/agent/pi.yaml`. Nothing outside `agents/` in
  Sokar knows this agent's wording, and nothing here hardcodes Sokar's.
