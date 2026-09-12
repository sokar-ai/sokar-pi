# Decisions

Settled reasoning that outlives the change that produced it. A decision is written here when
somebody would otherwise ask "why is it like this?" and find only code.

Accepted risks live here too. An accepted risk is not a forgotten one: it says what the exposure
is, why it is not being removed, and what would change the answer.

## The bill generator is installed from its own lockfile, not resolved at build time

**Decided:** 2026-09-12, from the security review in `.codex-review.md` (P-02).

`build-pi-tree.sh` used to fetch the CycloneDX generator with `npx --yes` while the tree that is
about to be shipped sat mounted writable beside it. The application dependencies were already
installed from a lockfile with integrity hashes; the tool that inspects them was not.

It now has its own `buildtools/sbom/package.json` and `package-lock.json`, is installed with
`npm ci --ignore-scripts`, and the directory is removed before anything is packaged. So every byte
of the generator is checked against a recorded hash, no install script runs next to the payload,
and the tool is a build input rather than part of the package.

**The cost, stated plainly:** a second lockfile to keep current. It is pinned deliberately - the
generator's version decides what the bill looks like, and that should change in a reviewed commit
rather than on the day the registry serves something newer.

## Accepted risk: the Node runtime digest was first read from the service that serves it

**Decided:** 2026-09-12.

The package ships a Node runtime downloaded from `nodejs.org` and checked against a SHA-256 that
lives in this repository. The pin is reviewed here, which is stronger than reading a digest from
the same response as the file - but the value was originally taken from that service, so the first
recording of it trusted that service.

**Why it is accepted:** the digest is now a reviewed constant in a file that changes only through a
commit, and since 2026-09-12 `check-pin.py` refuses an environment override and asks the runtime
that was actually built what version it is. An attacker would have to have compromised nodejs.org
at the moment the pin was first recorded, and the pin would then still be stable and auditable.

**What would change it:** verifying the Node release signature (the project publishes signed
`SHASUMS256.txt`) in the build or the pin check, which is worth doing when this is next touched.
