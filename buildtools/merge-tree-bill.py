#!/usr/bin/env python3
"""
Folds the shipped tree's bill into this package's bill.

Two documents describe one package otherwise: the Maven graph of the adapter, and the npm tree
that build-pi-tree.sh installs. A consumer would have to read both, and the change detection
that gates an automated update would have to diff both.

The tree's components are nested under the component that carries them - CycloneDX allows a
component tree, and cyclonedx-npm already produces one - so the bill says *what ships inside
what* rather than flattening 167 packages beside four Maven jars.

    merge-tree-bill.py <package-bom.json> <tree-bom.json>
"""
import json
import sys


def main() -> int:
    package_path, tree_path = sys.argv[1], sys.argv[2]

    with open(package_path, encoding="utf-8") as handle:
        package = json.load(handle)
    with open(tree_path, encoding="utf-8") as handle:
        tree = json.load(handle)

    # The tree's own subject becomes a component of this package: it is a thing this package
    # ships, not a second document about the same thing.
    subject = dict(tree["metadata"]["component"])
    subject.pop("bom-ref", None)
    subject["type"] = "application"
    subject.setdefault("properties", []).append(
        {"name": "sokar:delivery", "value": "shipped-in-package"})
    subject["components"] = tree.get("components") or []

    package.setdefault("components", []).append(subject)
    with open(package_path, "w", encoding="utf-8") as handle:
        json.dump(package, handle, indent=2)

    def count(items):
        total = 0
        for component in items or []:
            total += 1 + count(component.get("components"))
        return total

    print(f"merge-tree-bill: {count(package['components'])} components in "
          f"{package_path.rsplit('/', 1)[-1]}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
