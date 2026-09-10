#!/usr/bin/env python3
"""
Compares a freshly built bill of materials against the published one, and says whether
publishing may proceed without a person.

This is what the other bills were for. An automated update is only safe if it knows what it is
not allowed to decide, and two of those things are questions about third-party code:

  * the set of dependencies changed - new code entering a containment tool is exactly what
    pinning exists to make visible;
  * a licence changed - the package redistributes that code, so this is an obligation.

Neither is answerable without comparing two bills, which is why nothing here could be written
before they existed.

    compare-bills.py <built.json> <published-url> [--expect-moved <name>]

Exit codes:

    0   nothing that needs deciding: publish
    1   something changed that a person must look at
    2   the comparison could not be made, which is not the same as "nothing changed"

The third is deliberate. A fetch that fails must never read as "no published bill", because
that is indistinguishable from a first release - and treating a network error as a first
release publishes exactly the change this is meant to stop.
"""
from __future__ import annotations

import argparse
import json
import sys
import urllib.error
import urllib.request


def components(bom: dict) -> dict[str, dict]:
    """
    Flattens a bill to a mapping of identity to component.

    Keyed by purl, because a name is not an identity: cyclonedx-npm strips the scope, so
    '@earendil-works/pi-coding-agent' appears as 'pi-coding-agent' and would collide with
    anything else of that name. Components nest, so this walks rather than iterates.
    """
    found: dict[str, dict] = {}

    def walk(items):
        for component in items or []:
            key = component.get("purl") or f"{component.get('name')}@{component.get('version')}"
            found[key] = component
            walk(component.get("components"))

    walk(bom.get("components"))
    return found


def licences(component: dict) -> set[str]:
    """Every licence named on a component, however it is expressed."""
    named = set()
    for entry in component.get("licenses") or []:
        licence = entry.get("license") or {}
        name = licence.get("id") or licence.get("name") or entry.get("expression")
        if name:
            named.add(name)
    return named


def fetch(url: str) -> dict | None:
    """
    Reads the published bill, or None when there genuinely is not one yet.

    Redirects are followed: Artifactory serves a small bill directly and offloads a large one
    to cloud storage with a 302, so a fetcher that stops there gets an empty body for exactly
    the packages with the most to say.
    """
    try:
        with urllib.request.urlopen(url, timeout=60) as response:
            return json.load(response)
    except urllib.error.HTTPError as failure:
        if failure.code == 404:
            return None
        unreadable(f"{url}: HTTP {failure.code}")
    except Exception as failure:  # noqa: BLE001 - any failure here must stop the publish
        unreadable(f"{url}: {failure}")
    return None  # unreachable; unreadable() exits


def unreadable(reason: str):
    """
    Stops with the code that means "could not compare", which is not code 1.

    Distinct on purpose: 1 says a person must look at a real change, 2 says the question was
    never answered. A caller that treats them alike will eventually treat a network failure as
    a clean comparison.
    """
    print(f"could not read the published bill - {reason}", file=sys.stderr)
    print("This is not the same as 'nothing changed'.", file=sys.stderr)
    sys.exit(2)


def pair_moves(expected: list[str], built: dict[str, dict], published: dict[str, dict],
               added: list[str], removed: list[str]) -> list[tuple[str, str]]:
    """
    Takes the component that is deliberately being updated out of the added and removed sets.

    Without this the check cannot be used by the very job it was written for. The pinned CLI
    is recorded with its version in its purl, so moving from 2.1.236 to 2.1.267 reads as one
    component removed and another added - which is a stop condition, and it would fire on every
    single update. The thing being updated is not news; what travelled in with it is.

    Only an unambiguous pair moves: exactly one added and exactly one removed carrying that
    name. Anything else - two arrivals, a departure with no arrival - is left where it is and
    stops the publish, because it is no longer the change that was asked for.

    The pair's licences are NOT excused. A new version under a new licence is exactly the
    obligation the licence check exists for, so the caller still compares those.

    :param expected: Component names allowed to change version.
    :param built: The new bill, keyed as components() keys it.
    :param published: The bill being compared against.
    :param added: Keys only in the new bill; mutated.
    :param removed: Keys only in the published bill; mutated.
    :return: The (published key, built key) pairs that moved.
    """
    moved = []
    for name in expected:
        arrived = [key for key in added if built[key].get("name") == name]
        left = [key for key in removed if published[key].get("name") == name]
        if len(arrived) != 1 or len(left) != 1:
            continue
        added.remove(arrived[0])
        removed.remove(left[0])
        moved.append((left[0], arrived[0]))
    return moved


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Compares a built bill against the published one.",
        epilog="Exit 0 publish, 1 a person must look, 2 the comparison could not be made.")
    parser.add_argument("built")
    parser.add_argument("published_url")
    parser.add_argument("--expect-moved", action="append", default=[], metavar="NAME",
                        help="a component that is allowed to change version, named because it "
                             "is what this run is updating; its licence is still compared")
    args = parser.parse_args()

    with open(args.built, encoding="utf-8") as handle:
        built = components(json.load(handle))

    published_bom = fetch(args.published_url)
    if published_bom is None:
        print("nothing published yet, so there is nothing to compare - publishing")
        return 0
    published = components(published_bom)

    added = sorted(set(built) - set(published))
    removed = sorted(set(published) - set(built))
    moved = pair_moves(args.expect_moved, built, published, added, removed)

    relicensed = sorted(
        key for key in set(built) & set(published)
        if licences(built[key]) != licences(published[key]))
    # Reported by the key it arrived as, so the line names the version a reader would look at.
    relicensed += [new for old, new in moved if licences(built[new]) != licences(published[old])]

    for old, new in moved:
        print(f"  moved     {published[old].get('name')} "
              f"{published[old].get('version')} -> {built[new].get('version')}")
    for label, keys in (("added", added), ("removed", removed)):
        for key in keys:
            print(f"  {label:9} {key}")
    for key in relicensed:
        was = published.get(key) or published[next(old for old, new in moved if new == key)]
        before = ", ".join(sorted(licences(was))) or "none"
        after = ", ".join(sorted(licences(built[key]))) or "none"
        print(f"  relicensed {key}: {before} -> {after}")

    if not (added or removed or relicensed):
        print(f"the same {len(built)} components, unchanged licences - publishing")
        return 0

    print()
    print(f"STOP: {len(added)} added, {len(removed)} removed, {len(relicensed)} relicensed.")
    print("New third-party code or a changed licence is not something to decide automatically.")
    return 1


if __name__ == "__main__":
    sys.exit(main())
