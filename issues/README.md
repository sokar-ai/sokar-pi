# Issues

What is open in this repository, ordered by what to do next rather than by number. The number is
identity, not sequence.

| # | Status | What it covers | Open questions |
|---|---|---|---|
| [001](001-Pin-GitHub-Actions-By-Sha.md) | handed on | Every third-party GitHub Action runs from a mutable tag, beside the tokens that publish packages and open pull requests. | 1 |
| [002](002-No-Test-Harness-For-Python-Tooling.md) | open | The Python tools that decide what gets shipped have no tests, and nothing in CI could run one. | 1 |

**Status** means: `open` - nobody is on it. `in progress` - somebody is. `handed on` - the work
belongs to another repository and this row tracks what has to change here afterwards. `blocked` -
waiting on an answer, and the row says whose.

## The one that is not ours to finish

**001** is tracked centrally as Sokar requirement **B49**, because the same mutable tags carry the
same risk in every repository the four agents maintain. Its open question is the one that decides
whether pinning helps at all: what keeps the pins current, since a pin without an update process
rots silently.

## Where the rest of the open work lives

Not everything open is an issue. Accepted risks - exposures that are known, deliberate and not
being removed - are in [`doc/decisions.md`](../doc/decisions.md) with what would change the
answer. A finding that has been answered is neither: it is in the code, with its reasoning.
