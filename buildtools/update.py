#!/usr/bin/env python3
"""
Moves this module to a new Pi version, doing exactly what a person would.

    update.py <version> [--dry-run]

Four things change and all four are done here - this is the agent A02 was describing when it
said four edits in four files with nothing checking that they agree:

    src/main/npm/package.json        the dependency this tree is built from
    src/main/npm/package-lock.json   regenerated, which is where the integrity hashes come from
    pom.xml                          <agent.cli.version>, filtered into the definition
    CHANGELOG.md                     what moved, under Unreleased

THE NODE RUNTIME IS A DIFFERENT AXIS AND IS NOT TOUCHED. NODE_VERSION and NODE_SHA256 in
build-pi-tree.sh pin the runtime the tree is built against and shipped with; moving Pi is not a
reason to move Node, and doing both in one change would make a failure ambiguous.

The lockfile is regenerated INSIDE THE PINNED NODE CONTAINER that build-pi-tree.sh uses, not
with whatever npm the machine has. A lockfile resolved by one npm and installed by another is
the thing 'npm ci' exists to prevent, and the format itself has changed between majors.

Exit codes:

    0   the working tree now ships the requested version
    1   the request cannot be carried out - no such version, or nothing to write
    2   upstream or the container could not be reached, which is not the same as "no such version"
"""
from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import urllib.error
import urllib.request
from pathlib import Path

PACKAGE = "@earendil-works/pi-coding-agent"
REGISTRY = "https://registry.npmjs.org/@earendil-works%2Fpi-coding-agent"

VERSION = re.compile(r"^\d+\.\d+\.\d+$")

ROOT = Path(__file__).resolve().parents[1]
POM = ROOT / "pom.xml"
MANIFEST = ROOT / "src" / "main" / "npm" / "package.json"
LOCKFILE = ROOT / "src" / "main" / "npm" / "package-lock.json"
CHANGELOG = ROOT / "CHANGELOG.md"

ENTRY = re.compile(r"^- Pi pinned to (\S+?)\.?(?: \(was (\S+?)\.?\))?\.$", re.M)


def published(version: str) -> None:
    """
    Refuses a version the registry does not have, before anything is written.

    :param version: The version to look for.
    """
    try:
        with urllib.request.urlopen(REGISTRY, timeout=60) as response:
            versions = json.load(response).get("versions") or {}
    except urllib.error.HTTPError as failure:
        unreadable(f"{REGISTRY}: HTTP {failure.code}")
    except Exception as failure:  # noqa: BLE001
        unreadable(f"{REGISTRY}: {failure}")
    if version not in versions:
        print(f"{PACKAGE} has no version {version}", file=sys.stderr)
        sys.exit(1)


def unreadable(reason: str):
    """Stops with the code that means "could not ask", distinct from "the answer is no"."""
    print(f"could not read the registry - {reason}", file=sys.stderr)
    print("This is not the same as 'there is no such version'.", file=sys.stderr)
    sys.exit(2)


def node_image() -> str:
    """Reads the builder image out of build-pi-tree.sh, so one pin serves both."""
    script = (ROOT / "buildtools" / "build-pi-tree.sh").read_text(encoding="utf-8")
    version = re.search(r'^NODE_VERSION="\$\{NODE_VERSION:-([^}"]+)\}"', script, re.M)
    if not version:
        print("build-pi-tree.sh declares no NODE_VERSION", file=sys.stderr)
        sys.exit(1)
    return f"docker.io/library/node:{version.group(1)}-slim"


def relock(manifest: str) -> str:
    """
    Resolves a new lockfile from the rewritten manifest, in the pinned Node container.

    '--package-lock-only' resolves and writes the lockfile without installing anything, which is
    what a person bumping this by hand would want: the integrity hashes come from the registry
    rather than from a tree nobody kept.

    :param manifest: The rewritten package.json.
    :return: The new lockfile.
    """
    if not shutil.which("podman"):
        print("no podman, so the lockfile cannot be resolved by the pinned npm", file=sys.stderr)
        print("This is not the same as 'the lockfile is unchanged'.", file=sys.stderr)
        sys.exit(2)

    work = ROOT / "target" / "relock"
    shutil.rmtree(work, ignore_errors=True)
    work.mkdir(parents=True)
    (work / "package.json").write_text(manifest, encoding="utf-8")
    # The old lockfile goes in too, so unrelated dependencies keep the versions they had and the
    # diff is the change that was asked for rather than a whole-tree resolution.
    shutil.copy(LOCKFILE, work / "package-lock.json")

    result = subprocess.run(
        ["podman", "run", "--rm", "--userns=keep-id", "-v", f"{work}:/out:z", "-w", "/out",
         node_image(), "npm", "install", "--package-lock-only", "--omit=dev",
         "--no-audit", "--no-fund"],
        capture_output=True, text=True)
    if result.returncode != 0:
        print(f"npm could not resolve the lockfile:\n{result.stdout}\n{result.stderr}",
              file=sys.stderr)
        sys.exit(2)
    return (work / "package-lock.json").read_text(encoding="utf-8")


def replace_once(text: str, pattern: re.Pattern, replacement, what: str) -> str:
    """Substitutes exactly one occurrence, refusing zero and refusing several."""
    rewritten, count = pattern.subn(replacement, text)
    if count != 1:
        print(f"expected exactly one {what}, found {count} - refusing to guess", file=sys.stderr)
        sys.exit(1)
    return rewritten


def note_the_change(text: str, version: str, was: str) -> str:
    """
    Records the new version under Unreleased, replacing this script's own earlier entry.

    One line survives a run of weekly bumps, and it keeps the version of the LAST RELEASE as its
    "was" - the reader wants the net move since something shipped, not the last hop.
    """
    existing = ENTRY.search(text)
    since = existing.group(2) if existing and existing.group(2) else was
    line = f"- Pi pinned to {version} (was {since})."

    if existing:
        return text[:existing.start()] + line + text[existing.end():]

    lines = text.split("\n")
    try:
        at = next(i for i, one in enumerate(lines) if one.rstrip() == "## [Unreleased]")
    except StopIteration:
        print("CHANGELOG.md has no '## [Unreleased]' heading to write under", file=sys.stderr)
        sys.exit(1)

    # Only this release's own block: a Changed heading under an older release is not ours.
    ends = next((i for i in range(at + 1, len(lines)) if lines[i].startswith("## ")), len(lines))
    heading = next((i for i in range(at, ends) if lines[i].rstrip() == "### Changed"), None)
    if heading is None:
        lines[at + 1:at + 1] = ["", "### Changed", "", line]
    else:
        first = next((i for i in range(heading + 1, ends) if lines[i].startswith("- ")), None)
        lines.insert(first if first is not None else heading + 2, line)
    return "\n".join(lines)


def installed(lockfile: str) -> str | None:
    """The version of the agent package the lockfile resolves, for reporting and checking."""
    entry = json.loads(lockfile).get("packages", {}).get(f"node_modules/{PACKAGE}")
    return entry.get("version") if entry else None


def main() -> int:
    parser = argparse.ArgumentParser(description="Ships a new Pi version.")
    parser.add_argument("version", help="the version to ship, e.g. 0.85.1")
    parser.add_argument("--dry-run", action="store_true",
                        help="say what would change and write nothing")
    args = parser.parse_args()

    if not VERSION.match(args.version):
        print(f"{args.version!r} is not a version", file=sys.stderr)
        return 1

    pom = POM.read_text(encoding="utf-8")
    was = re.search(r"<agent\.cli\.version>([^<]+)</agent\.cli\.version>", pom)
    if not was:
        print(f"{POM} declares no agent.cli.version", file=sys.stderr)
        return 1
    if was.group(1).strip() == args.version:
        print(f"already shipping {args.version} - nothing to do")
        return 0

    published(args.version)

    pom = replace_once(pom, re.compile(r"<agent\.cli\.version>[^<]+</agent\.cli\.version>"),
                       f"<agent.cli.version>{args.version}</agent.cli.version>",
                       "agent.cli.version in pom.xml")

    manifest = replace_once(
        MANIFEST.read_text(encoding="utf-8"),
        re.compile(rf'("{re.escape(PACKAGE)}"\s*:\s*")[^"]+(")'),
        lambda m: f"{m.group(1)}{args.version}{m.group(2)}",
        f"{PACKAGE} in package.json")

    lockfile = relock(manifest)
    resolved = installed(lockfile)
    if resolved != args.version:
        print(f"the lockfile resolved {resolved}, not {args.version}", file=sys.stderr)
        return 1

    module = re.search(r"<artifactId>sokar-agent-pi</artifactId>\s*<version>([^<]+)</version>", pom)
    bumped = None
    if module and not module.group(1).endswith("-SNAPSHOT"):
        major, minor, patch = module.group(1).split(".")
        bumped = f"{major}.{minor}.{int(patch) + 1}"
        pom = replace_once(
            pom,
            re.compile(r"(<artifactId>sokar-agent-pi</artifactId>\s*<version>)[^<]+(</version>)"),
            lambda m: f"{m.group(1)}{bumped}{m.group(2)}", "the module's own version")

    changelog = note_the_change(CHANGELOG.read_text(encoding="utf-8"),
                                args.version, was.group(1).strip())

    before = len(json.loads(LOCKFILE.read_text(encoding="utf-8")).get("packages", {}))
    after = len(json.loads(lockfile).get("packages", {}))

    print(f"  pi            {was.group(1).strip()} -> {args.version}")
    print(f"  lockfile      {before} -> {after} packages, resolved by the pinned npm")
    if bumped:
        print(f"  this module   {module.group(1)} -> {bumped}")
    elif module:
        print(f"  this module   {module.group(1)}, unchanged - the CI run number already orders "
              f"snapshot packages")
    print(f"  changelog     {ENTRY.search(changelog).group(0)}")

    if args.dry_run:
        print("\n--dry-run: nothing written")
        return 0

    POM.write_text(pom, encoding="utf-8")
    MANIFEST.write_text(manifest, encoding="utf-8")
    LOCKFILE.write_text(lockfile, encoding="utf-8")
    CHANGELOG.write_text(changelog, encoding="utf-8")
    print(f"\nwritten. Review the diff, then: Pin Pi {args.version}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
