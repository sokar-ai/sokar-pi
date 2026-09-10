#!/usr/bin/env bash
#
# What only a real machine and a real credential can answer, run against the PUBLISHED
# packages rather than a build tree.
#
# This is the last step for this agent, and it is deliberately the only one that installs
# from the package repository: everything before it proves the code is right, this proves
# what an operator actually gets is right. The two have been different before - a package
# that carried a stale binary, a package whose dependency would not resolve.
#
# Two halves:
#
#   install    the packages install from Artifactory, sokar finds the agent it was never
#              linked against, and the hooks register. No credential needed.
#
#   tier 2     a task authenticates against a real provider and completes a prompt, and the
#              real credential never enters the container or any log. Needs
#              SOKAR_E2E_OPENROUTER_API_KEY; without it this half is skipped and the script
#              still exits 0, so it is safe where there is no account.
#
# CREDENTIALS COME FROM THE ENVIRONMENT, NEVER FROM ARGV - a command line is readable by
# every process on the machine.
set -uo pipefail

FAILURES=0
pass() { printf '  \033[32mPASS\033[0m  %s\n' "$1"; }
fail() { printf '  \033[31mFAIL\033[0m  %s\n' "$1"; FAILURES=$((FAILURES + 1)); }
info() { printf '        %s\n' "$1"; }
skip() { printf '  \033[33mSKIP\033[0m  %s\n' "$1"; }

WORK="$(mktemp -d)"
PROJECT="acceptance-$$"
CONTAINER=""

cleanup() {
    [ -n "$CONTAINER" ] && podman rm -f "$CONTAINER" >/dev/null 2>&1
    podman rmi -f "sokar/$PROJECT" >/dev/null 2>&1
    sokar vault unlock --forget >/dev/null 2>&1
    rm -rf "$WORK" \
           "${XDG_DATA_HOME:-$HOME/.local/share}/sokar/build/$PROJECT" \
           "${XDG_DATA_HOME:-$HOME/.local/share}/sokar/mirrors/$PROJECT.git" \
           "${XDG_RUNTIME_DIR:-/run/user/$(id -u)}/sokar/sokar-$PROJECT-"*
    :
}
trap cleanup EXIT

echo "== acceptance: the published packages on a clean machine =="

# ------------------------------------------------------------------ install
echo
echo "-- what got installed --"

if command -v sokar >/dev/null 2>&1; then
    pass "sokar is on PATH ($(sokar --version))"
else
    fail "sokar is not installed"
    exit 1
fi

# The whole point of the split: this binary was built in another repository, against a
# published contract, and sokar has never heard of it.
if sokar agents 2>/dev/null | grep -q '^pi'; then
    pass "sokar discovers the agent it was never linked against"
    info "$(sokar agents 2>/dev/null | grep '^pi')"
else
    fail "sokar does not list the pi agent"
    sokar agents 2>&1 | head -5 | while read -r line; do info "$line"; done
fi

if sokar setup >/dev/null 2>&1; then
    pass "sokar setup registered the podman hooks"
else
    fail "sokar setup failed"
fi

# Is $1 older than $2? Asked of whichever package manager this machine has: this suite runs on a
# Debian machine AND a Fedora one, and 'dpkg --compare-versions' does not exist on the second.
# Both compare digit runs numerically, which is what makes build 10 beat build 9.
version_lt() {
    if command -v dpkg >/dev/null 2>&1; then
        dpkg --compare-versions "$1" lt "$2"
    else
        [ "$(rpm --eval "%{lua:print(rpm.vercmp('$1','$2'))}")" = "-1" ]
    fi
}

# The version the installed package carries, and whether a later build could ever replace it.
# Asked of dpkg and rpm rather than reasoned about: a flat snapshot is the same version every
# build, so 'apt upgrade' has nothing to do and whoever installed yesterday stays there until they
# purge - a repository called 'snapshots' that never updates anybody. The run number is what
# distinguishes one from the next, and 9 < 10 has to be a NUMERIC comparison or the whole scheme
# stops working at the tenth build.
INSTALLED_VERSION="$(dpkg-query -W -f='${Version}' sokar-agent-pi 2>/dev/null \
    || rpm -q --qf '%{VERSION}' sokar-agent-pi 2>/dev/null)"
case "$INSTALLED_VERSION" in
    *-SNAPSHOT)
        fail "version is '$INSTALLED_VERSION': '-SNAPSHOT' sorts ABOVE the release" ;;
    *~SNAPSHOT)
        fail "version is '$INSTALLED_VERSION': a flat snapshot never supersedes the last one" ;;
    *~snapshot.*)
        RELEASE="${INSTALLED_VERSION%%~*}"
        RUN="${INSTALLED_VERSION##*~snapshot.}"
        if ! command -v dpkg >/dev/null 2>&1 && ! command -v rpm >/dev/null 2>&1; then
            # Said as what it is. The first version of this check asked dpkg on both legs, and
            # on Fedora - which has no dpkg - reported "does not order correctly": a verdict it
            # never reached, about a version that was in fact correct. A check that cannot run
            # must say so rather than answer.
            fail "neither dpkg nor rpm is here, so the ordering could not be checked at all"
        elif version_lt "$INSTALLED_VERSION" "$RELEASE" \
                && version_lt "$INSTALLED_VERSION" "${RELEASE}~snapshot.$((RUN + 1))" \
                && version_lt "${RELEASE}~snapshot.9" "${RELEASE}~snapshot.10"
        then
            pass "version $INSTALLED_VERSION is below $RELEASE and the next build supersedes it"
        else
            fail "version $INSTALLED_VERSION does not order correctly against $RELEASE"
        fi ;;
    "")  fail "the installed package reports no version at all" ;;
    *)   info "version $INSTALLED_VERSION is not a snapshot" ;;
esac

echo

# The bill the package carries, checked where it matters: on a machine that installed the
# package rather than in the build that made it. An automated update gate diffs this against
# the published one, so a package that ships none, or one describing something else, breaks
# the gate silently rather than loudly.
BOM=/usr/share/sokar/sbom/sokar-agent-pi.cdx.json
if [ -r "$BOM" ] && python3 -c "
import json, sys
bom = json.load(open('$BOM'))
assert bom.get('bomFormat') == 'CycloneDX'
assert bom['metadata']['component']['name'] == 'sokar-agent-pi'
def walk(items):
    for c in items or []:
        yield c
        yield from walk(c.get('components'))
names = {c['name'] for c in walk(bom.get('components'))}
assert 'sokar-agent-pi-tree' in names, 'the bill does not name sokar-agent-pi-tree'
print(len(names))
" > /tmp/bom-names 2>/dev/null; then
    pass "the installed package carries a bill naming sokar-agent-pi-tree ($(cat /tmp/bom-names) components)"
else
    fail "the installed package carries no usable bill at $BOM"
fi
rm -f /tmp/bom-names

# Asked of the MACHINE, not of the build: two sources that cannot drift apart unnoticed.
# This agent's row, not the first 'installs:': a machine with two agents reported the other one.
DECLARED="$(sokar agents --supply-chain 2>/dev/null \
    | awk '$1 == "pi" { found = 1; next } found && $1 == "installs:" { print $2; exit }')"
# The scope is stripped in a bill, so '@earendil-works/pi-coding-agent' appears under its bare
# name; the purl keeps the scope. Components nest, so this walks rather than iterates.
IN_BILL="$(python3 -c "
import json
def walk(items):
    for c in items or []:
        yield c
        yield from walk(c.get('components'))
bom = json.load(open('$BOM'))
print(next((c.get('version','') for c in walk(bom.get('components'))
            if c['name'] == 'pi-coding-agent'), ''))
" 2>/dev/null)"

if [ -z "$DECLARED" ]; then
    fail "the installed adapter does not say which Pi version it ships"
elif [ "$DECLARED" != "$IN_BILL" ]; then
    fail "the adapter ships $DECLARED, the bill it carries names ${IN_BILL:-nothing}"
elif [ -n "${SOKAR_E2E_EXPECT_CLI:-}" ] && [ "$DECLARED" != "$SOKAR_E2E_EXPECT_CLI" ]; then
    # Set by the update pipeline: proof that the candidate, not an older package, got installed.
    fail "expected Pi $SOKAR_E2E_EXPECT_CLI, this machine ships $DECLARED"
else
    pass "this machine ships Pi $DECLARED, and its bill agrees"
fi

echo
echo "-- what sokar thinks of this machine --"
podman --version | while read -r line; do info "$line"; done
sokar doctor 2>&1 | while read -r line; do info "$line"; done

# ------------------------------------------------------------------ tier 2
echo
echo "-- tier 2: a real credential --"

if [ -z "${SOKAR_E2E_OPENROUTER_API_KEY:-}" ]; then
    skip "no SOKAR_E2E_OPENROUTER_API_KEY, so nothing here runs"
    info "the install half above is what this run proved"
    echo
    echo "== $FAILURES check(s) failed =="
    exit "$FAILURES"
fi

CREDENTIAL="$SOKAR_E2E_OPENROUTER_API_KEY"

# This run's own vault, with its own passphrase, in its own directory. The operator's is
# never read or replaced - and on a CI server there is not one anyway, which is exactly why
# the redirect must be explicit rather than incidental.
export SOKAR_VAULT="$WORK/vault.bin"

if ! sokar vault unlock --passphrase-command "printf acceptance" >/dev/null 2>&1; then
    fail "could not create this run's vault at $SOKAR_VAULT"
    echo; echo "== $FAILURES check(s) failed =="; exit 1
fi

# Keyed by PROVIDER, not by agent: the credential belongs to whoever issued it, so any agent
# pointed at OpenRouter finds this one entry.
if printf '%s' "$CREDENTIAL" | sokar vault put openrouter --type api-key >/dev/null 2>&1; then
    pass "the vault stored the credential"
else
    fail "the vault would not store the credential"
    echo; echo "== $FAILURES check(s) failed =="; exit 1
fi

# A real credential makes these searchable in a way a fake one cannot: a fake one may
# coincidentally not be stored at all.
if sokar vault list 2>/dev/null | grep -qF "$CREDENTIAL"; then
    fail "vault list printed the credential value"
else
    pass "vault list does not print the value"
fi

if grep -qF "$CREDENTIAL" "$SOKAR_VAULT" 2>/dev/null; then
    fail "the credential is in the clear in the vault file"
else
    pass "the credential is not recoverable from the vault file"
fi

cat > "$WORK/project.yml" <<EOF
project:
  name: "$PROJECT"
  security_class: "guarded"
image:
  base_image: "ubuntu:24.04"
EOF

# --clearance deny: an acceptance run must never raise a prompt on somebody's desktop and
# then wait for it.
START_LOG="$WORK/start.log"
(cd "$WORK" && timeout 900 sokar task run --agent pi \
    --keep --no-attach --clearance deny > "$START_LOG" 2>&1)

CONTAINER="$(grep '^container ' "$START_LOG" | awk '{print $2}')"
if [ -z "$CONTAINER" ]; then
    fail "the task did not start"
    tail -8 "$START_LOG" | while read -r line; do info "$line"; done
    echo; echo "== $FAILURES check(s) failed =="; exit 1
fi
pass "the task started (container $CONTAINER)"

# CI sets SOKAR_E2E_MODEL in the workflow; this fallback is for running the suite by hand.
# Not a ':free' variant - 'z-ai/glm-5.2:free' returned rate_limit_exceeded on three consecutive
# attempts, and a gate that fails on somebody else's quota is not a gate.
MODEL="${SOKAR_E2E_MODEL:-z-ai/glm-5.3-flash}"
ANSWER="$(podman exec "$CONTAINER" sh -c \
    "timeout 240 pi --print --model '$MODEL' 'Reply with exactly the word SOKARLIVE and nothing else.' 2>&1" \
    2>/dev/null)"

if echo "$ANSWER" | grep -q "SOKARLIVE"; then
    pass "the agent authenticated against OpenRouter and completed a prompt"
else
    fail "the agent did not complete the prompt"
    echo "$ANSWER" | head -6 | while read -r line; do info "$line"; done
fi

# These two are worth having even when the one above fails. A credential scheme that
# authenticates by handing the real key to the agent has not failed loudly - it has failed
# quietly, and only a real credential makes the leak searchable.
if podman exec "$CONTAINER" sh -c 'env' 2>/dev/null | grep -qF "$CREDENTIAL"; then
    fail "the real credential is in the container's environment"
else
    pass "the container holds no credential, only a task-scoped token"
fi

STATE="${XDG_RUNTIME_DIR:-/run/user/$(id -u)}/sokar"
if grep -rqF "$CREDENTIAL" "$STATE" "$START_LOG" 2>/dev/null; then
    fail "the real credential appears in Sokar's own logs"
else
    pass "the credential appears in no log this run produced"
fi

echo
if [ "$FAILURES" -eq 0 ]; then
    echo "== all checks passed =="
else
    echo "== $FAILURES check(s) failed =="
fi
exit "$FAILURES"
