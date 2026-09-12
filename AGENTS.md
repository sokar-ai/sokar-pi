# Rules for working in this repository

Short on purpose. A rule is here because somebody paid for learning it.

## Shared across the Sokar repositories

The same text in `sokar`, `sokar-frontend` and the three agent repositories. Change it in the
channel first, not in one copy.

- **The operator pushes. Agents commit and stop.** A push starts a build that costs metered minutes
  and can cancel one already running. Say what is ready and let him decide when.
- **Nobody edits another agent's repository.** Reading is fine and encouraged. For this repository
  there is no exception: nobody has standing permission here, and that includes me elsewhere.
- **The channel is `~/.sokar/agent-channel.md` and it is append-only.** One heading per entry,
  `## <date -u> — <agent>`. Read everything written since your marker before you post, move your
  marker only past somebody else's entry, and never rewrite what is there. A question carries a
  prefix naming who is owed the answer, so a reader scanning the file can see it.
- **A secret never appears in a command line, and reaches a process through its environment or its
  standard input.** Where one is stored, it is encrypted at rest and readable only by its owner -
  and in CI it is never written to a filesystem at all.
- **The test machines are shared.** Name what you remove rather than sweeping "what I do not
  recognise", and **announce a restart before you trigger one**. A reboot leaves no trace in the
  work it interrupts, so the person whose run it killed cannot find out what happened.
- **Say what a run does to a shared machine before starting it - what it does, not what you believe
  it does.** Check first. A confident wrong answer costs somebody else an afternoon.
- **Link to a requirement by its number and to the index, never to its file.** A finished
  requirement is deleted, so a link to the file breaks exactly when that requirement succeeds.
- **Measure before you claim.** "It works" means it was run. "It is not the cause" means the
  counter-test was run too. A finding without a measurement is a guess wearing a fact's clothes.
- **An issue is one task.** If it needs two answers or two changes that could land separately, it
  is two issues. A dependency on an issue in another Sokar repository is named in the issue, with
  the repository and the number, so nobody discovers it by starting.
- **The documentation language is US English** - issues, decisions, changelog, comments, commit
  messages. The channel too.
- **Each agent writes only in its own repository.** Everything else is a request in the channel to
  whoever owns it, with the reason.
- **Dot files and directories are not checked in.** `.gitignore` ignores `.*` and names only the
  exceptions a build needs. Anything true of one machine goes in `.AGENTS.md`, which that rule
  ignores by itself.

## Work in this repository

- **Every open thing is an issue**, in `issues/`, named `NNN-Short-Title.md`, numbered in order and
  carrying a priority. Not a TODO in the code, not a note in a commit message.
- **`issues/README.md` is the index**, and it is part of the change that adds or closes an issue.
  It opens with a table - number (linked), status, what it covers, how many questions are still
  open - ordered by priority rather than by number, so the top row is what to do next.
- **Settled reasoning goes in `doc/decisions.md`**, including accepted risks. An accepted risk says
  what the exposure is, why it is not being removed, and what would change the answer.
- **The exceptions to the dot-file rule here** are what a Java build needs: `.github`, `.mvn`,
  `.gitignore`, `.gitkeep`, plus `__pycache__/` for the Python tooling.
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
