#!/usr/bin/env python3
"""
Checks that everything naming the shipped Pi version says the same thing.

Nothing enforced this before. A bot that writes one place and not another would ship a package
whose definition advertises a version its own payload does not contain, and no test would
notice until an operator asked the installed agent what it runs.

This agent's pin is not a URL and a digest, the way a downloaded tool's is: the tree is built
here and shipped inside the package, so the question is whether the definition, the manifest
the tree is built from and the lockfile that resolves it all name the same version.

    check-pin.py [--definition target/classes/agent/pi.yaml]

Four questions:

    1  the version is a version, not an unsubstituted ${...} - filtering happened at all
    2  it is the version pom.xml pins
    3  package.json asks for that version
    4  package-lock.json resolves it, which is what npm ci actually installs

NEEDS NO NETWORK, unlike the other agents' copies of this script. Everything it compares is in
the repository, because everything this package installs is in the repository - which is the
same property that lets an image build here download nothing.

Exit codes:

    0   they agree
    1   they do not
    2   the question could not be answered
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

PACKAGE = "@earendil-works/pi-coding-agent"

ROOT = Path(__file__).resolve().parents[1]
POM = ROOT / "pom.xml"
MANIFEST = ROOT / "src" / "main" / "npm" / "package.json"
LOCKFILE = ROOT / "src" / "main" / "npm" / "package-lock.json"
FILTERED = ROOT / "target" / "classes" / "agent" / "pi.yaml"

FAILURES = 0


def ok(message: str) -> None:
    print(f"  \033[32mOK\033[0m    {message}")


def bad(message: str) -> None:
    global FAILURES
    print(f"  \033[31mFAIL\033[0m  {message}")
    FAILURES += 1


def main() -> int:
    parser = argparse.ArgumentParser(description="Checks that the shipped version agrees "
                                                 "everywhere it appears.")
    parser.add_argument("--definition", default=str(FILTERED),
                        help="the FILTERED definition, as built into target/classes")
    args = parser.parse_args()

    definition = Path(args.definition)
    if not definition.is_file():
        print(f"{definition} does not exist - build first, this reads what the build produced",
              file=sys.stderr)
        return 2

    found = re.search(r'^\s*version:\s*"?([^"\n]+)"?\s*$',
                      definition.read_text(encoding="utf-8"), re.M)
    if not found:
        bad(f"{definition} names no version")
        return 1
    version = found.group(1).strip()

    print(f"== the pin, as {definition} carries it ==")

    if "${" in version:
        bad(f"resource filtering did not run - the definition still reads {version!r}")
        return 1
    ok(f"the definition ships Pi {version}")

    pinned = re.search(r"<agent\.cli\.version>([^<]+)</agent\.cli\.version>",
                       POM.read_text(encoding="utf-8"))
    if not pinned:
        bad("pom.xml declares no agent.cli.version")
    elif pinned.group(1).strip() != version:
        bad(f"pom.xml pins {pinned.group(1).strip()}, the definition ships {version}")
    else:
        ok("pom.xml pins the same version")

    asked = (json.loads(MANIFEST.read_text(encoding="utf-8"))
             .get("dependencies", {}).get(PACKAGE))
    if asked != version:
        bad(f"package.json asks for {asked}, the definition ships {version}")
    else:
        ok("package.json asks for it")

    entry = (json.loads(LOCKFILE.read_text(encoding="utf-8"))
             .get("packages", {}).get(f"node_modules/{PACKAGE}") or {})
    if entry.get("version") != version:
        # This is the one that decides what an operator gets: npm ci installs the lockfile and
        # ignores what package.json asked for.
        bad(f"the lockfile resolves {entry.get('version')}, and npm ci installs the lockfile")
    else:
        ok("the lockfile resolves it, which is what npm ci installs")

    print()
    if FAILURES:
        print(f"STOP: {FAILURES} of the pinned facts disagree. The package would ship something "
              f"other than what it advertises.")
        return 1
    print("The pin agrees everywhere it is written.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
