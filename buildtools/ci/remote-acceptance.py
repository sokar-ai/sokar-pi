#!/usr/bin/env python3
"""
Runs the acceptance suite on a clean rented machine, against the PUBLISHED packages.

A stock image, never a prepared snapshot: the point is to install the way an operator does,
on a machine that has never seen this project. That also makes it the only check that
exercises the package repository itself - the signature, the index, and the dependency on
sokar resolving from the same place.

The server is destroyed in a finally, whatever happens.

    REMOTE_BUILD   Hetzner API token
    SSH            private key that reaches the server it creates
    SOKAR_E2E_OPENROUTER_API_KEY   optional; without it the tier 2 half is skipped
"""
from __future__ import annotations

import argparse
import os
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import hetzner  # noqa: E402

USER = "acceptance"

# Stock images, and the versions matter: the deb must install on the oldest release Sokar
# supports, and the rpm on a current Fedora with SELinux enforcing.
IMAGES = {"ubuntu": "ubuntu-24.04", "fedora": "fedora-44"}

INSTALL = {
    "ubuntu": """
set -eux
export DEBIAN_FRONTEND=noninteractive
apt-get update -qq
apt-get install -y -qq ca-certificates curl gnupg podman
curl -fsSL {key} | gpg --dearmor > /usr/share/keyrings/sokar.gpg
echo "deb [signed-by=/usr/share/keyrings/sokar.gpg] {base}/sokar-dist-deb snapshots main" \
    > /etc/apt/sources.list.d/sokar.list
apt-get update
apt-get install -y -qq sokar sokar-agent-claude
""",
    "fedora": """
set -eux
cat > /etc/yum.repos.d/sokar.repo <<'EOF'
[sokar]
name=Sokar
baseurl={base}/sokar-dist-rpm/snapshots
enabled=1
gpgcheck=0
EOF
dnf install -y -q podman
dnf install -y -q sokar sokar-agent-claude
""",
}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--os", default="ubuntu", dest="operating_system", choices=sorted(IMAGES))
    parser.add_argument("--type", default="cpx12", dest="server_type",
                        help="1 core and 2 GB: this installs packages and runs one prompt, so "
                             "it needs no build machine. Raise it if an image build runs short.")
    parser.add_argument("--location", default=None)
    parser.add_argument("--artifactory", default="https://fuinorg.jfrog.io/artifactory")
    parser.add_argument("--ssh-private-key", default=None,
                        help="defaults to the SSH environment variable")
    parser.add_argument("--keep", action="store_true", help="leave it running, for debugging")
    args = parser.parse_args()

    environment = hetzner.agent(args.ssh_private_key)
    client = hetzner.client()
    location = args.location or hetzner.location_for(client, args.server_type)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S")

    with hetzner.provisioned(client,
                             name=f"sokar-acc-{args.operating_system}-{stamp}",
                             server_type=args.server_type,
                             image_name=IMAGES[args.operating_system],
                             location=location, ssh_key_name=None,
                             environment=environment, keep=args.keep) as (_server, address):

        hetzner.await_ssh(address)

        print(f"\n-- installing from {args.artifactory}, as an operator would --")
        key_url = f"{args.artifactory}/api/security/keypair/sokar-packages/public"
        hetzner.ssh(address, environment,
                    INSTALL[args.operating_system].format(base=args.artifactory, key=key_url))

        # An unprivileged user, because that is the shape a task runs in: rootless podman,
        # the operator's own directories. Running the suite as root would prove less.
        print(f"\n-- creating the {USER} user --")
        hetzner.ssh(address, environment,
                    f"id -u {USER} >/dev/null 2>&1 || useradd -m -s /bin/bash {USER}; "
                    f"loginctl enable-linger {USER}; "
                    f"install -d -m 0700 -o {USER} -g {USER} /home/{USER}/.ssh && "
                    f"install -m 0600 -o {USER} -g {USER} /root/.ssh/authorized_keys "
                    f"/home/{USER}/.ssh/authorized_keys")

        print("\n-- sending the suite --")
        script = (Path(__file__).resolve().parents[1] / "acceptance.sh").read_text()
        run(address, environment, f"cat > /home/{USER}/acceptance.sh <<'SOKAR_EOF'\n{script}\nSOKAR_EOF\n"
                                  f"chmod +x /home/{USER}/acceptance.sh")

        print("\n-- acceptance --")
        # The credential travels as an environment variable on the remote shell, never on a
        # command line: argv is readable by every process on that machine.
        exported = ""
        for name in ("SOKAR_E2E_OPENROUTER_API_KEY", "SOKAR_E2E_MODEL"):
            value = os.environ.get(name, "")
            if value:
                exported += f"{name}={shell_quote(value)} "
        run(address, environment,
            f"cd /home/{USER} && XDG_RUNTIME_DIR=/run/user/$(id -u) {exported}./acceptance.sh")

    return 0


def shell_quote(value: str) -> str:
    """Quotes a value for a remote shell. Single quotes, with the closing trick for any inside."""
    return "'" + value.replace("'", "'\\''") + "'"


def run(address: str, environment: dict[str, str], command: str) -> None:
    """Runs a command as the unprivileged user, failing the script if it fails."""
    result = subprocess.run(hetzner.ssh_argv(address, command, user=USER),
                            env={**os.environ, **environment})
    if result.returncode != 0:
        raise SystemExit(result.returncode)


if __name__ == "__main__":
    sys.exit(main())
