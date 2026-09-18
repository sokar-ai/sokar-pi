# Rules for working in this repository

Short on purpose. A rule is here because somebody paid for learning it.

## Shared across the Sokar repositories

The same text in `sokar`, `sokar-frontend`, the three agent repositories and
`sokar-message-sluice`. Change it in the channel first, not in one copy.

- **The operator pushes. Agents commit and stop.** A push starts a build that costs metered minutes
  and can cancel one already running. Say what is ready and let him decide when.
- **Everyone stays in their own repository and asks for what they need from another.** Ruled by
  the operator on 2026-09-13: an agent neither reads nor writes another agent's repository - what
  it needs from there, it asks that repository's agent for in the channel, with the reason. The
  one exception is the backend agent, who coordinates and may **read** the other repositories.
  **Writing is always the job of the agent responsible for the repository**, with no exception.
- **The channel is append-only.** One heading per entry,
  `## <date -u> — <agent>`. Read everything written since your marker before you post, move your
  marker only past somebody else's entry, and never rewrite what is there. A question carries a
  prefix naming who is owed the answer, so a reader scanning the file can see it.
- **Re-read the channel immediately before appending to it.** An entry that landed between your
  read and your append makes what you are about to write answer a state that no longer exists —
  Agent Smith published advice for an experiment that had been settled four minutes earlier, and
  the read that would have caught it costs nothing. The marker says what to compare against.
- **Re-arm the watcher as the first thing after reading an entry**, before answering and before
  building. A watcher that reports one change and exits is unarmed from that moment, and twice
  entries sat unread for hours because reading went straight into work.
- **Compare against a marker of what was actually read**, never against a fresh baseline taken when
  you re-arm. A baseline adopts everything written between the read and the re-arm as already seen,
  silently. Keep the last heading you read and compare against that. Both sides had this defect on
  2026-09-07, fixed it the same afternoon, and this agent reintroduced it on 2026-09-12 by counting
  headings at re-arm time.
- **The file's order is the truth and the headings are a label.** An entry can sit behind ones
  stamped later, because a heading is written when an entry is composed and the append happens when
  it is finished - on 2026-09-12 a 17:21Z entry landed after a 17:31Z one. So take the timestamp at
  append time rather than at composition, **compare against the position of the last entry you read
  rather than against its time**, and never sort this file by heading to reconstruct what happened.
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
- **From "both are valid" it does not follow that both should exist.** Two indexes, two markers,
  two manifests, the same skills in two repositories - every expensive defect of 2026-09-12 had
  that shape, and not one of them was a wrong fact. They were correct facts with one inference too
  many on top, and the second copy was always the one that quietly went stale. When a thing is
  right in two forms, publish one and say why.
- **Measure before you claim.** "It works" means it was run. "It is not the cause" means the
  counter-test was run too. A finding without a measurement is a guess wearing a fact's clothes.
- **Two agents agreeing on an inference is not evidence** - it is one inference with two names on
  it. Agreement counts when each measured separately; when the second agent takes the first's
  observation and adds a reason, the reason has been reviewed by nobody. On 2026-09-12 two of us
  agreed that a catalogue field was missing, neither looked for the specification, and it was the
  registry behaving as documented. **Say which part you measured and which part you inferred**, so
  the other can agree with one and not the other.
- **An issue is one task.** If it needs two answers or two changes that could land separately, it
  is two issues. A dependency on an issue in another Sokar repository is named in the issue, with
  the repository and the number, so nobody discovers it by starting.
- **The documentation language is US English** - issues, decisions, changelog, comments, commit
  messages. The channel too.
- **Dot files and directories are not checked in.** `.gitignore` ignores `.*` and names only the
  exceptions a build needs. Anything true of one machine goes in `.AGENTS.md`, which that rule
  ignores by itself.

## Work in this repository

- **Every open thing is an issue**, in `issues/`, named `NNN-Short-Title.md`, numbered in order and
  carrying a priority. Not a TODO in the code, not a note in a commit message.
- **`issues/README.md` is the index**, and it is part of the change that adds or closes an issue.
  It opens with a table - number (linked), status, what blocks it, what it covers, how many
  questions are still open - ordered by priority rather than by number, so the top row is what
  to do next. **Blocked by** is a column, not prose inside the description, and names a Sokar
  requirement by its number, so the dependency reads the same from Sokar's **Blocks** column.
- **`doc/decisions.md` opens with its own index**: a table of date and one line saying what was
  decided, newest first, each row linking to the full text below it. Writing the row is part of
  taking the decision.
- **A finished issue is deleted, row and all.** Not kept with a status saying it is done, not moved
  to a section of what used to be here: the index holds what is still to do and nothing else.
- **Before deleting it, move what outlives it.** The test is whether the knowledge is about more
  than that one issue - a measurement, a distinction the contract makes, something that was built
  wrong once. It goes to `doc/` if a person using the product needs it, to `AGENTS.md` if it is
  internal and true wherever we work, to `.AGENTS.md` if it is only true on this machine. An
  issue's own acceptance criteria outlive nothing and are dropped with it.
- **Settled reasoning goes in `doc/decisions.md`**, including accepted risks. An accepted risk says
  what the exposure is, why it is not being removed, and what would change the answer.
- **The exceptions to the dot-file rule here** are what a Java build needs: `.github`, `.mvn`,
  `.gitignore`, `.gitkeep`, plus `__pycache__/` for the Python tooling.
- **The skills this repository expects**, at the operator's instruction: Oracle's GraalVM skill
  (`oracle/skills`, the `graal` directory) because the build produces a native image, and
  `decebals/claude-code-java` because it is a Java repository. They come from
  **<https://fuinorg.jfrog.io/artifactory/agent-skills/>** rather than from GitHub: a tag upstream
  is a name its owner may repoint, and what is republished there is what was reviewed and can be
  rolled back.
- **The changelog is part of the change**, written by hand in the same commit. Nothing enforces it
  for now: the check was removed on 2026-09-13 by the operator's decision, and requiring an entry
  returns with Sokar B55. An entry goes into the existing section of its kind under `[Unreleased]`,
  never under a new heading of the same kind - the Claude Code adapter's changelog grew three
  `Changed` that way.
- **Comments say why, not what.** One-liners in config; in code, the reason a reader would
  otherwise have to reconstruct. No exhaustive prose inlined in source.
- **A test that passes both with and without the fix proves nothing.** Run it against the broken
  version before trusting it.
- **The build is Java and Maven, and a file that is not says why it stays** (Sokar B53, 2026-09-13).
  What stays: `mvnw`, the Maven wrapper, which is how a pinned Maven arrives before any Java tooling
  can run; and `buildtools/build-pi-tree.sh`, which orchestrates podman and npm in a pinned container
  - the reason and its guard matrix are in `doc/decisions.md`. The Python tools and `acceptance.sh`
  are on their way out - issues 010 to 012.

### How to get them

**The repository is readable without credentials and without any tool**, because a skill is a
directory holding `SKILL.md` and installing one is unpacking an archive. Commands rather than a
verb: there is no instruction every agent understands, so this section gives you something to run.

    BASE=https://fuinorg.jfrog.io/artifactory/agent-skills
    curl -fsSL $BASE/.skills/skills.json             # every slug with its latest version
    curl -fsSL $BASE/.skills/<slug>/versions.json    # the versions of one skill
    curl -fsSL -o /tmp/s.zip $BASE/<slug>/<version>/<slug>-<version>.zip
    unzip -q -d <your skills directory>/<slug> /tmp/s.zip

**Where `<your skills directory>` is depends on the harness, and only you know yours.** Claude Code
reads `~/.claude/skills/<slug>/` and a project's `.claude/skills/<slug>/`; another harness has its
own place, and a skill put where nothing reads it fails silently. Verify by asking the harness what
it loaded, not by looking at the directory.

**Check what you downloaded.** The artifact's SHA-256 is stated under
`/artifactory/api/storage/agent-skills/<path>`; an interrupted transfer otherwise installs a
truncated skill, which reads as a short one rather than as an error.

**If the JFrog CLI happens to be installed**, `jf agent skills install <slug> --repo agent-skills`
does the same with resolution and an install record. Do not install it for this — the four commands
above are the whole requirement.

**The version is `YYYY.MMDD.P`** — the upstream commit's date, then the packaging revision, so a
newer upstream always sorts higher and a repackaging of the same upstream never reuses a number.
`2026.911.3` is the eleventh of September, packaged the third time. (Not `2026.09.11`: SemVer
forbids a leading zero in a numeric identifier.)

### Measuring what an agent asks at first run

What a fresh task shows a person can only be measured at a real terminal, on a machine where the
agent has never run. Four traps each answered the wrong question on 2026-09-10:

- `--version` never reaches the first-run flow, so it proves nothing about dialogs.
- Start in `/workspace`. From `/home/agent` an agent asks whether to trust that directory instead
  of showing the dialog being looked for.
- The task image has neither `python3` nor `node`: a probe that changes an agent's own JSON writes
  the whole file rather than patching it.
- A binary that exists is not a binary that is current. Check its timestamp or version; a day-old
  build once answered as if it were the one just made.

Sokar's own acceptance suite runs a stub agent that has no dialogs, so a green Sokar build says
nothing about what a real agent asks. That check belongs in this repository.

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
