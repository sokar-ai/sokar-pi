#!/usr/bin/env python3
"""
Fails a change to code that does not say what changed.

A changelog nobody is obliged to write is a changelog that stops being written, and the
first person to notice is whoever is trying to work out what an installed package contains.
The rule this enforces is narrow: if a commit range touches anything that ships or builds,
CHANGELOG.md has to be in that range too.

    check-changelog.py <base> <head>

Documentation and editor settings are exempt, because a typo fix is not a notable change and
requiring an entry for one teaches people to write entries that say nothing.

A change that ships nothing observable - a comment, a rename, a workflow tidy - says
[no changelog] in a commit message and passes. Nothing can tell a comment from a behavior
change by looking at a diff, so the judgement is a person's; the marker makes it one somebody
made on purpose, in the log, rather than a rule quietly bent.

Exit codes:

    0   the changelog was updated, or nothing needed it
    1   code changed and the changelog did not
    2   the range could not be read

A NOTE ON WHAT THIS CANNOT SEE. It compares two commits. On the first push of a new branch
there is no base to compare against, and GitHub reports the previous commit as all zeros -
so the check says so and passes rather than failing every new branch. That is a real hole,
and it is stated here rather than left for someone to discover: the check is a reminder for
the ordinary case, not a wall.
"""
from __future__ import annotations

import subprocess
import sys
from fnmatch import fnmatch

CHANGELOG = "CHANGELOG.md"

# Changing one of these needs no entry. Everything else does - including the build files and
# the workflows, which decide what an operator receives just as much as the source does.
EXEMPT = ("*.md", ".gitignore", ".idea/*", "LICENSE")

EMPTY = "0" * 40

# What a person writes when a change ships nothing an operator could observe.
WAIVER = "[no changelog]"


def here(commit: str) -> bool:
    """Whether the object is in this clone."""
    return subprocess.run(["git", "cat-file", "-e", f"{commit}^{{commit}}"],
                          capture_output=True).returncode == 0


def fetch(commit: str) -> None:
    """
    Fetches one commit into a shallow clone, which is what CI hands this script.

    actions/checkout clones with depth 1, so the commit a push came FROM is not present and
    the comparison cannot be made at all. This cost two publishes: the guard exited 2 on every
    run and never once looked at a changelog. Deliberately here rather than as fetch-depth on
    the checkout step, so the check keeps working whatever a later edit does to that step.
    """
    subprocess.run(["git", "fetch", "--no-tags", "--depth=1", "origin", commit],
                   capture_output=True, text=True)


def changed(base: str, head: str) -> list[str]:
    """
    Lists the files that differ between two commits.

    Two arguments rather than 'base..head': a range has to be resolvable, while a two-commit
    diff only needs both objects to exist - which matters when their history is not connected.

    :param base: What to compare from.
    :param head: What to compare to.
    :return: Repository-relative paths.
    """
    for commit in (base, head):
        if not here(commit):
            fetch(commit)
    try:
        out = subprocess.run(["git", "diff", "--name-only", base, head],
                             capture_output=True, text=True, check=True)
    except subprocess.CalledProcessError as failure:
        print(f"could not compare {base} with {head}: {failure.stderr.strip()}", file=sys.stderr)
        print("Both commits have to be in this clone; a shallow checkout holds neither.",
              file=sys.stderr)
        sys.exit(2)
    return [line for line in out.stdout.splitlines() if line]


def main() -> int:
    base, head = sys.argv[1], sys.argv[2]

    if not base or base.startswith(EMPTY[:8]):
        print(f"::warning::no commit to compare against ({base!r}), so the changelog was not "
              f"checked. This is not the same as 'it was updated'.")
        return 0

    waived = subprocess.run(["git", "log", "--format=%B", f"{base}..{head}"],
                            capture_output=True, text=True)
    if WAIVER in waived.stdout:
        print(f"a commit message says {WAIVER}, so no entry is required")
        return 0

    files = changed(base, head)
    if not files:
        print("nothing changed in this range")
        return 0

    if CHANGELOG in files:
        print(f"{CHANGELOG} is in this change")
        return 0

    needing = [f for f in files if not any(fnmatch(f, pattern) for pattern in EXEMPT)]
    if not needing:
        print(f"only documentation and settings changed, so {CHANGELOG} is not required")
        return 0

    print(f"::error::{CHANGELOG} was not updated, but this change touches what ships:")
    for f in needing:
        print(f"  {f}")
    print()
    print(f"Add what changed under '## [Unreleased]' in {CHANGELOG}. A version bump applied by")
    print("buildtools/update.py writes its own entry and needs nothing by hand.")
    return 1


if __name__ == "__main__":
    sys.exit(main())
