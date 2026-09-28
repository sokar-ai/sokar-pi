# Rules for working in this repository

Short on purpose. A rule is here because somebody paid for learning it.

## Shared across the Sokar repositories

The same text in every repository `project.yml` names. Change it in the channel first, not in
one copy.

- **The operator pushes. Agents commit and stop.** A push starts a build that costs metered minutes
  and can cancel one already running. Say what is ready and let the operator decide when.
- **A rewrite is cheap only while the commits are yours alone. Ask the remote first.**
  *The operator pushes. Agents commit and stop* - and stop includes stop amending, stop squashing,
  stop rebasing. A commit stops being yours the moment it is pushed, and nothing tells you when that
  happened except asking:

      git ls-remote origin refs/heads/main            the tip, and it cannot be stale
      git merge-base --is-ancestor <commit> <tip>     whether the commit is already in it

  `origin/main` and `@{u}` are caches and answer a question about your last fetch. An amend after a
  push leaves two commits with one parent and one subject on two sides, and the operator meets it as
  a merge conflict. **The repair is never a force push** - reset onto the remote's commit and
  re-apply as a new one, because the side that pushes is the side whose history is real. That same
  reset is also the only safe way to squash, which is why the cure and the correct method are one
  operation.
- **Everyone stays in their own repository and asks for what they need from another.** An agent
  neither reads nor writes another agent's repository - what it needs from there, it asks that
  repository's agent for in the channel, with the reason. The one exception is the coordinating
  agent, who may **read** the other repositories. Reading does not replace asking: a file shows what
  is the case, and only the agent who wrote it knows why. **Writing is always the job of the agent
  responsible for the repository**, with no exception.
- **The channel is append-only.** An entry begins with `## <UTC timestamp> — <agent>`. Headings
  inside an entry are free; scan for entries by the timestamp, never by `##` alone. Read
  everything written since your marker before you post, move your marker only past somebody
  else's entry, and never rewrite what is there. A question carries a prefix naming who is owed
  the answer, so a reader scanning the file can see it.
- **When quoting a document that has headings, indent it four spaces rather than fencing it.**
  A fence hides them from a renderer and not from a scanner, and the channel is append-only, so
  what a fence lets through cannot be taken out again.
- **Re-read the channel immediately before appending to it.** An entry that landed between your
  read and your append makes what you are about to write answer a state that no longer exists,
  and the read that would have caught it costs nothing. The marker says what to compare against.
- **Re-arm the watcher as the first thing after reading an entry**, before answering and before
  building. A watcher that reports one change and exits is unarmed from that moment, and whatever
  arrives while its reader is busy with work sits unread until somebody looks.
- **Compare against a marker of what was actually read**, never against a fresh baseline taken when
  you re-arm. A baseline adopts everything written between the read and the re-arm as already seen,
  silently. Keep the last heading you read and compare against that.
- **The file's order is the truth and the headings are a label.** An entry can sit behind ones
  stamped later, because a heading is written when an entry is composed and the append happens when
  it is finished. So take the timestamp at append time rather than at composition, **compare
  against the position of the last entry you read rather than against its time**, and never sort
  the channel by heading to reconstruct what happened.
- **A secret never appears in a command line, and reaches a process through its environment or its
  standard input.** Where one is stored, it is encrypted at rest and readable only by its owner -
  and in CI it is never written to a filesystem at all.
- **Every file fetched from Artifactory follows redirects** - `curl -L`, `jf rt curl -L`. A file
  large enough is answered with a `302` to its cloud storage, and a fetch without `-L` gets an
  empty body: the check passes for months and fails the day the file grows. Where "large enough"
  lies is not known; a Debian index is past it. The `/api/` endpoints answer directly. Let `curl`
  drop the credentials on that cross-host redirect - the storage URL is signed - and never pass
  `--location-trusted`.
- **The test machines are shared, and so is everything a run resolves from** - `~/.m2`,
  `~/.sokar/handover/` and what a VM has installed. Name what you remove rather than sweeping
  "what I do not recognise", **announce a restart before you trigger one**, and **announce a
  change to any of these before you make it** - an install, a deploy, a replaced handover -
  saying what replaces it and its hash, and wait while somebody's run is resolving from it. A
  reboot leaves no trace in the work it interrupts, and a swapped artifact leaves none in the run
  that used it: it passes, on something nobody meant to test.
- **Say what a run does to a shared machine before starting it - what it does, not what you believe
  it does.** Check first. A confident wrong answer costs somebody else an afternoon.
- **Link to a requirement by its number and to the index, never to its file.** A pointer is
  written for the day the thing it points at is gone, and it goes in more ways than one: a
  finished requirement is deleted, an issue closed unbuilt is deleted, and a design document
  recording an undecided question is deleted when the question is answered. A link to a file
  breaks on all three; a link to the index breaks on none. **Where a repository can enforce
  this with a test, it does** - without one, the defect is found by accident or not at all.
- **From "both are valid" it does not follow that both should exist.** Two indexes, two markers,
  two manifests, the same skills in two repositories - each is a correct fact with one inference
  too many on top, and the second copy is always the one that quietly goes stale. When a thing is
  right in two forms, publish one and say why.
- **Measure before you claim.** "It works" means it was run. "It is not the cause" means the
  counter-test was run too. A finding without a measurement is a guess wearing a fact's clothes.
- **"I could not get X" is a claim about a method, not about the world**, and it is worth saying
  out loud only once a second method has failed too. A page `curl` returns empty can be one whose
  body is loaded afterwards, and a fetch that renders it answers in one call what the first method
  called undeterminable.
- **Two agents agreeing on an inference is not evidence** - it is one inference with two names on
  it. Agreement counts when each measured separately; when the second agent takes the first's
  observation and adds a reason, the reason has been reviewed by nobody. **Say which part you
  measured and which part you inferred**, so the other can agree with one and not the other.
- **An issue is one task.** If it needs two answers or two changes that could land separately, it
  is two issues. A dependency on an issue in another Sokar repository is named in the issue, with
  the repository and the number, so nobody discovers it by starting.
- **The documentation language is US English** - issues, decisions, changelog, comments, commit
  messages. The channel too.
- **Do not refer to feature numbers in commit messages.** Just state what the feature is. A commit
  says *"Start work in a chosen repository"*, not *"B67"* - the number means nothing to somebody
  reading the history without the index beside it, and the index outlives the requirement by being
  deleted when it is finished.
- **Dot files and directories are not checked in.** `.gitignore` ignores `.*` and names only the
  exceptions a build needs. Anything true of one machine goes in `.AGENTS.md`, which that rule
  ignores by itself.
- **A Java repository builds in one language.** Its build, checks, update job and tests run
  through Java and Maven, and no build or workflow needs `python3`. A file that stays in another
  language is named in that repository's `AGENTS.md`, with the reason it cannot be Java there or
  in the file's own header - `mvnw` is the worked example: it is how a pinned Maven arrives
  before any Java can run. And no repository carries a copy of a helper another one carries:
  copies of one helper drift apart, and one grows a step the others lack. The drift is the
  argument, not the tidiness.
- **Java code is null-checked when it compiles.** Every package holding main code is
  `@NullMarked` (JSpecify) from its first commit, and NullAway runs in the main compile as an
  error, scoped by `OnlyNullMarked`. An unmarked package is skipped in silence, so a repository
  keeps a test that fails on one.
- **Documentation and rules say what is true now.** A README, `build.md`, everything under
  `doc/` and `AGENTS.md` state what holds today - what the product does, how it is built, what
  was measured, what an agent must do and why - with no dates, no "until", "since" or "used
  to", no incident told as a story, and nobody named as the one who did, found, decided or
  approved something, an agent no more than the operator. Where a rule gives somebody a duty,
  it names the role: "the operator pushes", "the repository's agent", "the coordinating
  agent". A rule keeps its reason, stated so that it stays true. How a thing came to be is in
  the git history; the changelog and the issues record events on purpose and are not covered.
  A dated sentence is stale the day after it is written, and nobody rereads it to find out.

## Work in this repository

- **Every open thing is an issue**, in `issues/`, named `PInn-Short-Title.md`, numbered in
  order and carrying a priority. Not a TODO in the code, not a note in a commit message.
- **The letter says which set a number belongs to**, so a bare `PInn` is unambiguous in
  every repository at once and an ordinary number can never look like a citation. `B`, `A`, `F`
  and `P` are Sokar's own sets, `PJ` the project,
  `SL` the sluice, `MX` the Matrix transport, and `CC`, `PI` and `OM` the three agents. The
  shape is `[A-Z]{1,2}\d{2}`, and `IssueCitationTest` keys on it.
- **`issues/README.md` is the index**, and it is part of the change that adds or closes an issue.
  It opens with a table - number (linked), status, what blocks it, what it covers, how many
  questions are still open - ordered by priority rather than by number, so the top row is what
  to do next. **Blocked by** is a column, not prose inside the description, and names a Sokar
  requirement by its number, so the dependency reads the same from Sokar's **Blocks** column.
- **`doc/decisions.md` opens with its own index**: a table of what each decision covers and one
  line saying what was decided, ordered by subject, each row linking to the full text below it. No
  dates - when a decision was taken, `git log` answers. Writing the row is part of taking the
  decision.
- **A finished issue is deleted, row and all.** Not kept with a status saying it is done, not moved
  to a section of what used to be here: the index holds what is still to do and nothing else.
- **A retired number leaves the documents with it.** In the same change that deletes an issue, the
  numbers naming it go too, and what the sentence needed is written instead: *the retired issue
  about the Python tooling having no test harness*, rather than its bare number. That is what makes
  a citation here always a pointer, so `IssueCitationTest` can check that every number named is an
  issue that exists; a repository that keeps numbers as history cannot run that check, and an open
  issue then names one retired long ago without anybody noticing.
- **Before deleting it, move what outlives it.** The test is whether the knowledge is about more
  than that one issue - a measurement, a distinction the contract makes, something that was built
  wrong once. It goes to `doc/` if a person using the product needs it, to `AGENTS.md` if it is
  internal and true wherever we work, to `.AGENTS.md` if it is only true on this machine. An
  issue's own acceptance criteria outlive nothing and are dropped with it.
- **Settled reasoning goes in `doc/decisions.md`**, including accepted risks. An accepted risk says
  what the exposure is, why it is not being removed, and what would change the answer.
- **The exceptions to the dot-file rule here** are what a Java build needs: `.github`, `.mvn`,
  `.gitignore`, `.gitkeep`.
- **The skills this repository expects**: Oracle's GraalVM skill
  (`oracle/skills`, the `graal` directory) because the build produces a native image, and six of
  `decebals/claude-code-java` because it is a Java repository. That set is eighteen skills, and
  `spring-boot-patterns` or `jpa-patterns` say nothing about this code, so these are the ones meant:

      graal  java-code-review  test-quality  security-audit  concurrency-review
      clean-code  solid-principles

  They come from
  **<https://fuinorg.jfrog.io/artifactory/agent-skills/>** rather than from GitHub: a tag upstream
  is a name its owner may repoint, and what is republished there is what was reviewed and can be
  rolled back.
- **The changelog is part of the change**, written by hand in the same commit. Nothing enforces it;
  requiring an entry returns with Sokar B55. An entry goes into the existing section of its kind
  under `[Unreleased]`, never under a new heading of the same kind - inserting at the top grows a
  second `Changed` beside the first.
- **Comments say why, not what.** One-liners in config; in code, the reason a reader would
  otherwise have to reconstruct. No exhaustive prose inlined in source.
- **A suite result is reported with its skips named, never as a count.** `Tests run: 41, Skipped: 1`
  read as *41 tests green* hides a check that never runs at all, and the person most likely to read
  past it is whoever knows why it skips.
- **A test that passes both with and without the fix proves nothing.** Run it against the broken
  version before trusting it.
- **What stays in another language, and why** - the list the shared "one language" rule asks for:
  `mvnw`, the Maven wrapper, which is how a pinned Maven arrives before any Java can run; and
  `buildtools/build-pi-tree.sh`, which orchestrates podman and npm in a pinned container - the reason and its guard matrix are in `doc/decisions.md`. It records Node in the tree's bill
  through Sokar's `sokar-release`, whose classpath the pom passes as its argument. The release
  tooling is that tool, called from the pom and the workflows, and the acceptance run is the
  scenarios in `src/acceptance`.

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

**If the harness cannot install a skill, read it.** Unpack it outside the repository, in a scratch
directory, and read its `SKILL.md`. A harness that cannot write its skills directory has not left
you without the skills: reading them still finds defects a passing test suite missed.

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
agent has never run. Four traps each answer the wrong question:

- `--version` never reaches the first-run flow, so it proves nothing about dialogs.
- Start in `/workspace`. From `/home/agent` an agent asks whether to trust that directory instead
  of showing the dialog being looked for.
- The task image has neither `python3` nor `node`: a probe that changes an agent's own JSON writes
  the whole file rather than patching it.
- A binary that exists is not a binary that is current. Check its timestamp or version; an older
  build answers as if it were the one just made.

Sokar's own acceptance suite runs a stub agent that has no dialogs, so a green Sokar build says
nothing about what a real agent asks. That check belongs in this repository.

## What this repository is

The Pi adapter: an agent definition, the Java that shapes what Pi cannot express as data, the
TypeScript extensions placed inside a task container, and the packaging that ships Pi with its own
Node runtime. It depends on the published Sokar agent API and wire artifacts, never on Sokar's
implementation.

- `./mvnw -o -B -s settings.xml test` is the fast gate; `verify` adds the acceptance module.
- **No test may name the pinned version.** The update job bumps `agent.cli.version`, filtering
  carries it into the definition, and a literal in a test fails every bump -
  `expected 18.1.13 but was 18.2.7` is what it says the first time the pin moves. Which version is pinned is
  `PinAgreementTest`'s question, against the pom.
- **A gate that stops can only reach a person through the pull request**, and `gh pr create` with
  `${{ github.token }}` is refused unless the organization allows Actions to create pull requests -
  `GraphQL: GitHub Actions is not permitted to create or approve pull requests` - and a gate that
  fired correctly then loses its reason in the runner. Ways out:
  the setting, or the request step prefers `secrets.SOKAR_UPDATE_TOKEN` as the merge step does.
- **The release tooling is `sokar-release`**, resolved like `sokar-machines` through `-Pci-tools`;
  what this agent differs in is the `sokar.release.*` properties in `pom.xml`. At package time it
  is a plugin dependency of the exec plugin, so it never becomes a dependency of the package.
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
