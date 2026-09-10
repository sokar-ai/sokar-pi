#!/usr/bin/env python3
"""
Fails a change to code that does not say what changed.

A changelog nobody is obliged to write is a changelog that stops being written, and the
first person to notice is whoever is trying to work out what an installed package contains.
The rule this enforces is narrow: if a commit range touches anything that ships or builds,
CHANGELOG.md has to be in that range too.

    check-changelog.py <base> <head>

Editor settings and the licence are exempt, because nothing about them can be notable.
Documentation is NOT exempt. It was, by extension, and that was the wrong instrument: a typo in
a build note and a rewrite of the setup guide are both `*.md` and no diff can tell them apart -
which is the judgement this script already hands to a person. A documentation change that is
merely a typo says [no changelog] like any other change that ships nothing observable, so the
typo case is answered without a rule that guesses. It also does not survive being copied to a
repository whose documentation is the operator's surface rather than a build note.

Touching CHANGELOG.md is not the same as saying what changed, so the file has to gain an
entry - a bullet, or a version heading. The commit that moved these repositories to the
sokar-ai organisation satisfied the older check by rewriting a github.com URL in the
`[Unreleased]:` link line, while touching everything that ships.

A change that ships nothing observable - a comment, a rename, a workflow tidy - says
[no changelog] in a commit message and passes. Nothing can tell a comment from a behavior
change by looking at a diff, so the judgement is a person's; the marker makes it one somebody
made on purpose, in the log, rather than a rule quietly bent.

The marker answers for the commit it was written on and for nothing travelling with it. It used
to be looked for across the whole push, so a docstring-only tip saying [no changelog] excused
the two code commits under it - measured on this repository's own push of 2026-09-10. One entry
may still cover ten commits: this decides which commits ask the question, not how many entries
answer it, because a rule of one entry per commit produces the entries that say nothing.

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

import re
import subprocess
import sys
from fnmatch import fnmatch

CHANGELOG = "CHANGELOG.md"

# Changing one of these needs no entry. Everything else does - including the build files, the
# workflows and the documentation, which decide what an operator receives and what they are told
# just as much as the source does.
EXEMPT = (".gitignore", ".idea/*", "LICENSE")

EMPTY = "0" * 40

# What a person writes when a change ships nothing an operator could observe.
WAIVER = "[no changelog]"

# A version heading, which is what a release adds rather than a bullet.
HEADING = re.compile(r"^#+ +\[?\d")


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


def present(base: str, head: str) -> None:
    """
    Makes sure both commits are in this clone, before anything tries to read them.

    This has to happen before the waiver is looked for, not after. CI checks out with depth 1,
    so the commit a push came FROM is absent, and `git log base..head` then fails silently with
    empty output - which reads exactly like "no waiver in any message" and fails a build whose
    author wrote one. Measured on 2026-09-10: two agent repositories went red on a commit that
    carried the marker, while a third with the same marker passed, because its base happened to
    be present.

    :param base: What to compare from.
    :param head: What to compare to.
    """
    for commit in (base, head):
        if not here(commit):
            fetch(commit)


def message(commit: str) -> str:
    """
    Returns one commit's own message.

    :param commit: What to read.
    :return: The full message, empty when it cannot be read.
    """
    return subprocess.run(["git", "log", "-1", "--format=%B", commit],
                          capture_output=True, text=True).stdout


def attributable(base: str, head: str) -> list[str] | None:
    """
    Lists the commits of a push when each can be diffed against its own parent.

    Both conditions are checked rather than assumed: the range has to resolve, and every commit
    in it needs its parent here. In a shallow clone `git show` treats a boundary commit as a
    root and names every file in the tree, which would demand an entry for a push that changed
    one line - a check failing for its own reasons, which is worse than no check.

    :param base: What to compare from.
    :param head: What to compare to.
    :return: The commits, newest first, or {@code None} when nothing can be attributed.
    """
    out = subprocess.run(["git", "rev-list", f"{base}..{head}"], capture_output=True, text=True)
    if out.returncode != 0:
        return None
    commits = out.stdout.split()
    return commits if all(here(f"{commit}^") for commit in commits) else None


def deepened(base: str, head: str) -> list[str] | None:
    """
    Fetches enough history to attribute a push per commit, then lists it.

    Without this the rule would hold on a laptop and never in CI, where actions/checkout clones
    with depth 1 - the same shape of defect as looking for the waiver before fetching, which
    cost two publishes on 2026-09-10. Fifty is a bound rather than a measurement: a larger push
    degrades to judging the range together, and says so.

    :param base: What to compare from.
    :param head: What to compare to.
    :return: The commits, or {@code None} when the history is still too shallow.
    """
    commits = attributable(base, head)
    if commits is not None:
        return commits
    subprocess.run(["git", "fetch", "--no-tags", "--depth=50", "origin", head],
                   capture_output=True, text=True)
    return attributable(base, head)


def touched(commits: list[str]) -> list[str]:
    """
    Lists the files those commits change, each against its own parent.

    Two-commit diffs, never `git show`, for the reason attributable() states. A merge commit
    reports nothing, as it does to any name-only diff; these repositories push linear history,
    and a rule that guessed at merges would be a rule nobody had watched fail.

    :param commits: What to look at.
    :return: Repository-relative paths, without duplicates.
    """
    files: set[str] = set()
    for commit in commits:
        out = subprocess.run(["git", "diff", "--name-only", f"{commit}^", commit],
                             capture_output=True, text=True)
        files.update(line for line in out.stdout.splitlines() if line)
    return sorted(files)


def entry_added(base: str, head: str) -> bool:
    """
    Whether the change adds a line a reader of the changelog would see.

    An entry is a `- ` bullet, or a version heading when a release adds one. A link definition
    is neither, and it is the line that made the older check answer the wrong question: the
    commit moving these repositories to the sokar-ai organisation passed while touching
    everything that ships, because `[Unreleased]:` held a github.com URL and was rewritten with
    the rest. Every release, every link rewrite and every typo fix in this file opened the same
    hole for whatever rode along with it.

    THIS RESTS ON update.py WRITING A BULLET. An automated version bump passes because the line
    it writes starts with `- `; an edit there that changed the shape would take this gate with
    it silently, so the two belong in one thought.

    TWO THINGS IT DELIBERATELY DOES NOT CATCH. Editing an existing bullet - fixing a typo inside
    an entry - shows an added line and passes while saying nothing new. And a change that deletes
    an entry passes if it adds one anywhere else. Both are left because policing them needs a
    parser that understands a release moving entries under a version heading, and this is a
    reminder for the ordinary case rather than a wall - the same limit the note above states
    about a first push.

    :param base: What to compare from.
    :param head: What to compare to.
    :return: Whether an entry was added.
    """
    out = subprocess.run(["git", "diff", "--unified=0", base, head, "--", CHANGELOG],
                         capture_output=True, text=True)
    for line in out.stdout.splitlines():
        if not line.startswith("+") or line.startswith("+++"):
            continue
        body = line[1:].strip()
        if body.startswith("- ") or HEADING.match(body):
            return True
    return False


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

    present(base, head)

    commits = deepened(base, head)

    if commits is None:
        # Nothing can be attributed, so this degrades to the older, weaker rule - out loud,
        # because a fallback that changes what a gate means without saying so is not a gate.
        print("::warning::the commits between base and head are not in this clone, so the whole "
              "range is judged together and a waiver anywhere in it counts")
        if WAIVER in message(head):
            print(f"the tip says {WAIVER}, so no entry is required")
            return 0
        files = changed(base, head)
    else:
        asking = [commit for commit in commits if WAIVER not in message(commit)]
        if not asking:
            print(f"every commit here says {WAIVER}, so no entry is required")
            return 0
        files = touched(asking)

    if not files:
        print("nothing changed in this range")
        return 0

    needing = [f for f in files
               if f != CHANGELOG and not any(fnmatch(f, pattern) for pattern in EXEMPT)]
    if not needing:
        print(f"nothing that ships changed, so {CHANGELOG} is not required")
        return 0

    if CHANGELOG in files and entry_added(base, head):
        print(f"{CHANGELOG} gains an entry in this change")
        return 0

    why = ("changed without gaining an entry" if CHANGELOG in files else "was not updated")
    print(f"::error::{CHANGELOG} {why}, but this change touches what ships or what an"
          " operator is told:")
    for f in needing:
        print(f"  {f}")
    print()
    print(f"Add what changed under '## [Unreleased]' in {CHANGELOG}. A version bump applied by")
    print("buildtools/update.py writes its own entry and needs nothing by hand.")
    return 1


if __name__ == "__main__":
    sys.exit(main())
