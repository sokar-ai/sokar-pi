# Issues

What is open in this repository, ordered by what to do next rather than by number. The number is
identity, not sequence.

| # | Status | Blocked by | What it covers | Open questions |
|---|---|---|---|---|
| [PI20](PI20-Pi-Downloads-Its-Search-Tools-At-Every-Start.md) | open | — | Every start tries to download fd and ripgrep from github.com, which a task cannot reach. | 1 |
| [PI13](PI13-The-Built-Runtime-Is-Never-Asked-In-CI.md) | open | — | The built Node runtime's version is never asked in CI, because the tree does not exist yet when the check runs. | 1 |
| [PI01](PI01-Pin-GitHub-Actions-By-Sha.md) | handed on | — | Every third-party GitHub Action runs from a mutable tag, beside the tokens that publish packages and open pull requests. | 1 |
| [PI03](PI03-Automated-Agent-Updates.md) | open | — | The update pipeline is built; what it still decides by convention rather than by a stated rule, including a second pin with no detector. | 5 |
| [PI05](PI05-Declare-What-Waiting-Looks-Like.md) | blocked | Sokar B47 | Declaring what "waiting for a person" looks like in this agent's own output. | 1 |
| [PI07](PI07-Fail-Acceptance-When-A-Release-Adds-A-Dialog.md) | blocked | Sokar B52 | The acceptance run fails when a release adds a first-run dialog. | 1 |
| [PI08](PI08-Declare-Where-The-Session-Id-Is.md) | blocked | Sokar B46 | Declaring where this agent's session id is, so a task that comes back continues its conversation. | 2 |
| [PI04](PI04-Can-Pi-Reach-A-Forge-Subscription.md) | open | — | Whether this agent reaches a forge subscription at all, and whether brokering holds beyond the one dialect it was proven against. | 3 |
| [PI15](PI15-A-Test-That-Never-Runs-In-The-Pipeline.md) | open | — | One check is skipped in every run the pipeline makes: the tree it needs is built a phase after surefire. | 1 |

**Status** means: `open` - nobody is on it. `in progress` - somebody is. `handed on` - the work
belongs to another repository and this row tracks what has to change here afterwards. `blocked` -
waiting on something else, and **Blocked by** names it: an issue here by its number, a Sokar
requirement as `Sokar B<n>`.

## The one that is not ours to finish

**001** is tracked centrally as Sokar requirement **B49**, because the same mutable tags carry the
same risk in every repository the four agents maintain. Its open question is the one that decides
whether pinning helps at all: what keeps the pins current, since a pin without an update process
rots silently.

## Handed over from Sokar, 2026-09-12

Five agent requirements moved here when the operator ruled that an agent's work lives in its own
repository. **A02** became the update issue above (one per agent repository rather than one shared
file), **A11** the waiting one. The per-agent requirements were met: what outlived them is in
[`doc/decisions.md`](../doc/decisions.md), and the files themselves are gone.

`A01` and the candidate agents without a repository stay in sokar, where `issues/agents/` is now
the place an agent lives before it has one.

## Where the rest of the open work lives

Not everything open is an issue. Accepted risks - exposures that are known, deliberate and not
being removed - are in [`doc/decisions.md`](../doc/decisions.md) with what would change the
answer. A finding that has been answered is neither: it is in the code, with its reasoning.
