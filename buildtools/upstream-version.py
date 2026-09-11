#!/usr/bin/env python3
"""
Says whether Pi has moved on from the version this module ships.

The first step of the update pipeline, and the one that differs most between agents: what
"the current version" is depends on how the vendor publishes. Pi is an npm package, so this
reads the registry's 'latest' dist-tag; another agent reads a release list or a pointer file.
What every agent's copy of this script shares is the OUTPUT, so the job that calls it does not
have to know which:

    pinned=0.85.0        what this module ships today
    upstream=0.85.1      what the registry calls latest
    update=yes           'yes' when newer, 'no' when the same, 'rollback' when older
    source=dist-tag latest  which pointer 'upstream' was read from
    major=same           'moved' when the major version changed, which nothing may decide alone

The same lines are appended to $GITHUB_OUTPUT when it is set.

Exit codes:

    0   the question was answered, whatever the answer is
    2   it could not be answered

'update=yes' is deliberately NOT a non-zero exit. A scheduled job that goes red every time
there is something to do teaches whoever watches it to ignore red.

THE REGISTRY ALSO PUBLISHES 'legacy-node20', which is a different line of the same package and
not a channel to follow. Only 'latest' is read, and a tag named on the command line is the way
to pin something else deliberately.
"""
from __future__ import annotations

import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path

PACKAGE = "@earendil-works/pi-coding-agent"
REGISTRY = "https://registry.npmjs.org/@earendil-works%2Fpi-coding-agent"

VERSION = re.compile(r"^\d+\.\d+\.\d+$")

ROOT = Path(__file__).resolve().parents[1]
POM = ROOT / "pom.xml"


def pinned() -> str:
    """Reads the version this module ships, from the one place it is written by hand."""
    found = re.search(r"<agent\.cli\.version>([^<]+)</agent\.cli\.version>",
                      POM.read_text(encoding="utf-8"))
    if not found:
        unanswerable(f"{POM} declares no agent.cli.version")
    version = found.group(1).strip()
    if not VERSION.match(version):
        unanswerable(f"{POM} pins {version!r}, which is not a version")
    return version


def upstream(tag: str) -> str:
    """
    Reads the version a dist-tag points at.

    :param tag: The dist-tag, 'latest' unless something else is asked for.
    :return: A bare version.
    """
    try:
        with urllib.request.urlopen(REGISTRY, timeout=60) as response:
            tags = json.load(response).get("dist-tags") or {}
    except urllib.error.HTTPError as failure:
        unanswerable(f"{REGISTRY}: HTTP {failure.code}")
    except Exception as failure:  # noqa: BLE001 - anything at all here means "do not know"
        unanswerable(f"{REGISTRY}: {failure}")
    if tag not in tags:
        unanswerable(f"{PACKAGE} has no dist-tag {tag!r}; it has {sorted(tags)}")
    if not VERSION.match(tags[tag]):
        unanswerable(f"dist-tag {tag!r} is {tags[tag]!r}, which is not a version")
    return tags[tag]


def unanswerable(reason: str):
    """
    Stops with the code that means "could not tell", which is not the code for "up to date".

    An update job that reads a failed fetch as "nothing new" is quietly switched off, and looks
    exactly like one that is working.
    """
    print(f"could not tell whether there is a new version - {reason}", file=sys.stderr)
    sys.exit(2)


def order(version: str) -> tuple[int, ...]:
    """
    Returns a version as something that sorts the way versions do.

    As text, 2.1.9 sorts after 2.1.10, and a gate that calls a legitimate update a rollback gets
    switched off. VERSION has already refused anything but three numbers, so this is the whole
    comparison rather than a parser for one.

    :param version: A version that matched VERSION.
    :return: Its numbers, in order.
    """
    return tuple(int(part) for part in version.split("."))


def verdict(have: str, there: str, named: bool) -> str:
    """
    Decides what the job does about the two versions.

    Older than the pin is a rollback, which automation may not choose: a withdrawn release and
    two pointers that disagree look the same from here. Only a version a person named may take
    the pin backwards.

    :param have: The pinned version.
    :param there: The version upstream offers.
    :param named: Whether a person named that version rather than a pointer.
    :return: 'yes', 'no' or 'rollback'.
    """
    if order(there) > order(have):
        return "yes"
    if order(there) == order(have):
        return "no"
    return "yes" if named else "rollback"


def main() -> int:
    parser = argparse.ArgumentParser(description="Compares the shipped Pi version against the "
                                                 "one the npm registry calls latest.")
    parser.add_argument("--channel", default="latest",
                        help="the dist-tag to follow; 'latest' by default")
    parser.add_argument("--upstream", default=None, metavar="VERSION",
                        help="answer as if the tag pointed here, and ask nothing")
    args = parser.parse_args()

    have = pinned()
    there = args.upstream or upstream(args.channel)
    if args.upstream and not VERSION.match(there):
        print(f"{there!r} is not a version", file=sys.stderr)
        return 1

    answer = {
        "pinned": have,
        "upstream": there,
        "source": "a version named by hand" if args.upstream else f"dist-tag {args.channel}",
        "update": verdict(have, there, bool(args.upstream)),
        # Answered here rather than by the caller, so every agent's copy answers it the same way.
        "major": "moved" if there.split(".")[0] != have.split(".")[0] else "same",
    }
    for key, value in answer.items():
        print(f"{key}={value}")

    output = os.environ.get("GITHUB_OUTPUT")
    if output:
        with open(output, "a", encoding="utf-8") as handle:
            for key, value in answer.items():
                handle.write(f"{key}={value}\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
