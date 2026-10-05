# Issues

What is open in this repository, grouped by the MVP (`sokar-project` PJ17): **Now**
serves one person, one machine, one agent, from install to a reviewed push; **Soon** follows right
after it; **Later** is the rest. Within a group, ordered by what to do next rather than by number. The
number is identity, not sequence.

## Now

| # | Status | Blocked by | What it covers | Open questions |
|---|---|---|---|---|
| [PI22](PI22-Build-Against-A-Released-Sokar.md) | open | PJ18 | no Sokar snapshot in a release build | 0 |

## Soon

Nothing open.

## Later

| # | Status | Blocked by | What it covers | Open questions |
|---|---|---|---|---|
| [PI21](PI21-A-Login-Inside-A-Brokered-Task-Fails.md) | open |  | a person cannot put a provider key into a task | 1 |

## What lives here and what in Sokar

An agent's work lives in its own repository, so the requirements for Pi are issues here. The
reasoning that outlives them is in [`doc/decisions.md`](../doc/decisions.md).

The candidate agents without a repository wait in `sokar-project` until a repository builds them.

## Where the rest of the open work lives

Not everything open is an issue. Accepted risks - exposures that are known, deliberate and not
being removed - are in [`doc/decisions.md`](../doc/decisions.md) with what would change the
answer. A finding that has been answered is neither: it is in the code, with its reasoning.
