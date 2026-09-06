#!/usr/bin/env bash
#
# Builds what the sokar-agent-pi package ships: the Pi coding agent with its dependencies, and
# the Node runtime it needs. Both end up inside the package, so a task image is built with no
# network access at all - see the 'packaged' section of pi.yaml.
#
# Everything runs in a pinned container rather than on the build machine, so no Node toolchain
# has to be installed to build Sokar, and the toolchain itself is a stated version rather than
# whatever the machine happens to have.
#
# Skipped, with a message, when there is no container runtime: the rest of Sokar still builds.
#
# Every container maps the build user in, so what it writes belongs to the build user. Without
# that, the tree is written as root and the next build cannot even delete it.
set -euo pipefail

MODULE="$(cd "$(dirname "$0")/.." && pwd)"
TARGET="$MODULE/target/tree"
NODE_VERSION="${NODE_VERSION:-22.20.0}"
NODE_SHA256="${NODE_SHA256:-eeaccb0378b79406f2208e8b37a62479c70595e20be6b659125eb77dd1ab2a29}"
BUILDER="${PI_BUILDER_IMAGE:-docker.io/library/node:${NODE_VERSION}-slim}"
CYCLONEDX_NPM_VERSION="${CYCLONEDX_NPM_VERSION:-6.0.1}"

if ! command -v podman >/dev/null 2>&1; then
    echo "build-pi-tree: no podman, so the Pi tree is not built; sokar-agent-pi will be incomplete"
    exit 0
fi

rm -rf "$TARGET"
mkdir -p "$TARGET/pi" "$TARGET/node"
cp "$MODULE/src/main/npm/package.json" "$MODULE/src/main/npm/package-lock.json" "$TARGET/pi/"

# npm ci, not npm install: it installs exactly the lockfile, refuses to update it, and checks
# every package against the integrity hash recorded there. That is what makes this reproducible
# rather than "whatever the registry served today".
podman run --rm --userns=keep-id -v "$TARGET:/out:z" -w /out/pi "$BUILDER" sh -c "
    set -eu
    npm ci --omit=dev --no-audit --no-fund

    # The bill, generated here and nowhere else: this is the only moment the lockfile and the
    # installed tree exist together, and the next line deletes the lockfile. Scanning the
    # result later is not equivalent - measured, a scan of the unpacked tree finds 141 of the
    # 167 packages and no integrity hashes at all.
    npx --yes @cyclonedx/cyclonedx-npm@${CYCLONEDX_NPM_VERSION} \
        --omit dev --spec-version 1.6 --output-format JSON \
        --output-file /out/pi/sbom.cdx.json

    rm -f package.json package-lock.json
"

# The published runtime, checked against the digest nodejs.org publishes beside it. Fetched with
# node's own fetch rather than curl: the container runs as the build user, so it cannot install
# packages, and the builder image already has the one tool needed.
podman run --rm --userns=keep-id -v "$TARGET:/out:z" "$BUILDER" sh -c "
    set -eu
    node -e \"
        const { writeFileSync } = require('fs');
        fetch('https://nodejs.org/dist/v${NODE_VERSION}/node-v${NODE_VERSION}-linux-x64.tar.gz')
            .then(r => { if (!r.ok) { throw new Error('HTTP ' + r.status); } return r.arrayBuffer(); })
            .then(b => writeFileSync('/tmp/node.tar.gz', Buffer.from(b)));
    \"
    echo '${NODE_SHA256}  /tmp/node.tar.gz' | sha256sum -c -
    tar -xzf /tmp/node.tar.gz -C /out/node --strip-components=1
    # Only the runtime is used: Pi is started as 'node <cli.js>'. npm, npx and the headers are
    # build-time tools that would otherwise ride along in every installation.
    rm -rf /out/node/include /out/node/share /out/node/lib/node_modules
    rm -f /out/node/bin/npm /out/node/bin/npx /out/node/bin/corepack
"

# Prebuilt binaries for platforms a Linux container will never run. Dead weight in a package
# that is already large, and confusing to anyone auditing what is shipped.
podman run --rm --userns=keep-id -v "$TARGET:/out:z" "$BUILDER" sh -c '
    set -eu
    find /out/pi -type d \( -name darwin -o -name win32 -o -name "darwin-*" -o -name "win32-*" \) \
        -prune -exec rm -rf {} + 2>/dev/null || true
    find /out/pi -type d -name examples -prune -exec rm -rf {} + 2>/dev/null || true
'

# Node is in the tree and is not an npm package, so nothing above can have listed it. Added
# with the digest this script already verifies, rather than a digest computed after the fact -
# what is recorded is what was checked.
python3 - "$TARGET/pi/sbom.cdx.json" "$NODE_VERSION" "$NODE_SHA256" <<'PYTHON'
import json, sys
path, version, digest = sys.argv[1], sys.argv[2], sys.argv[3]
with open(path) as handle:
    bom = json.load(handle)
bom.setdefault("components", []).append({
    "type": "application",
    "name": "node",
    "version": version,
    "purl": f"pkg:generic/node@{version}?download_url=https://nodejs.org/dist/v{version}/"
            f"node-v{version}-linux-x64.tar.gz",
    "licenses": [{"license": {"id": "MIT"}}],
    "hashes": [{"alg": "SHA-256", "content": digest}],
})
with open(path, "w") as handle:
    json.dump(bom, handle, indent=2)
PYTHON

cp "$TARGET/pi/sbom.cdx.json" "$MODULE/target/pi-tree-sbom.cdx.json"

# One tarball rather than two directories: one file for the packagers to carry, one file for an
# operator to check, and one unpack instead of copying tens of thousands of files per task.
tar -czf "$MODULE/target/pi-tree.tar.gz" -C "$TARGET" pi node

echo "build-pi-tree: pi $(du -sh "$TARGET/pi" | cut -f1), node $(du -sh "$TARGET/node" | cut -f1),"\
     "packaged $(du -h "$MODULE/target/pi-tree.tar.gz" | cut -f1)"
