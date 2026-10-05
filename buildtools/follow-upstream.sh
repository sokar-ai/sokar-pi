#!/usr/bin/env bash
#
# Asks upstream about everything this package pins - the Pi CLI and each pin of the tree - and moves
# every one with a release old enough, one commit each, so the update job verifies them together and
# opens one pull request. A person who wants one of them out drops its commit.
#
# Sokar's release tool answers and moves; this only walks the list, commits, and writes down what it
# found. What upstream is asked, how old a release must be and which properties a move writes are all
# in pom.xml.
#
# Environment:
#   WHAT     all (default), or one of the names in ITEMS
#   VERSION  a version to take instead of asking upstream; only with one name in WHAT
#   CHANNEL  the npm dist-tag the CLI follows (default: latest)
#   SOKAR_RELEASE  an absolute path to a command running the release tool, for a run on a
#            workstation; unset, it runs from the classpath in target/cp.txt, as CI resolves it
#
# Writes target/update/summary.md, and these outputs to GITHUB_OUTPUT (or stdout without one):
#   moved=yes|no, title, components (bill names of what moved), cli (the CLI's new version, or empty),
#   stop (why the result is not for automation to decide, or empty)
#
# Exit 2 on a refused input, 1 when something went wrong, 0 whenever the answers were written -
# including when upstream could not be asked, which is a stop, not a failure of this script.
set -euo pipefail

MODULE="$(cd "$(dirname "$0")/.." && pwd)"
cd "$MODULE"

# What is followed. UpdateRulesTest keeps this the pins pom.xml declares, plus the CLI. The CLI comes
# last: moving it regenerates the lockfile in the Node image the pom names, and that has to be the
# image this same run moved Node to, or the tree installs a lockfile another npm resolved.
ITEMS=(node fd rg cli)

refuse() { echo "follow-upstream: $1" >&2; exit 2; }

WHAT="${WHAT:-all}"
VERSION="${VERSION:-}"
CHANNEL="${CHANNEL:-latest}"
# Each of these reaches a command line, so each has a strict shape.
[[ "$WHAT" == all || " ${ITEMS[*]} " == *" $WHAT "* ]] \
    || refuse "WHAT='$WHAT' is not all or one of: ${ITEMS[*]}"
[[ -z "$VERSION" || "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[0-9A-Za-z.]+)?$ ]] \
    || refuse "VERSION='$VERSION' is not a version"
[[ "$CHANNEL" =~ ^[a-z0-9][a-z0-9.-]*$ ]] || refuse "CHANNEL='$CHANNEL' is not a dist-tag"
# One version for all of them would pin each of them to it.
[[ -z "$VERSION" || "$WHAT" != all ]] || refuse "a version is taken for one of them - set WHAT to one of: ${ITEMS[*]}"
if [ -n "${SOKAR_RELEASE:-}" ]; then
    [[ "$SOKAR_RELEASE" == /* && -x "$SOKAR_RELEASE" ]] \
        || refuse "SOKAR_RELEASE='$SOKAR_RELEASE' is not an absolute path to an executable"
fi
# Every move is committed, so a change already in the tree would be committed with the first one.
if ! git diff --quiet || ! git diff --cached --quiet; then
    refuse "the working tree has changes, and every move is committed"
fi

release() {
    if [ -n "${SOKAR_RELEASE:-}" ]; then
        "$SOKAR_RELEASE" "$@"
    else
        java -cp "$(cat target/cp.txt)" org.fuin.sokar.release.Main "$@"
    fi
}

OUT=target/update
mkdir -p "$OUT"
SUMMARY="$OUT/summary.md"
{
    echo "| | pinned | upstream | |"
    echo "|---|---|---|---|"
} > "$SUMMARY"

[ "$WHAT" = all ] || ITEMS=("$WHAT")
moved=()       # "Label version", for the title
components=()  # names in the shipped tree's bill, for the comparison
stops=()
cli=""

for item in "${ITEMS[@]}"; do
    case "$item" in
        cli)  label=Pi;      component=pi-coding-agent; ask=(--channel "$CHANNEL"); move=() ;;
        node) label=Node;    component=node;            ask=(--pin node);          move=(--pin node) ;;
        fd)   label=fd;      component=fd;              ask=(--pin fd);            move=(--pin fd) ;;
        rg)   label=ripgrep; component=ripgrep;         ask=(--pin rg);            move=(--pin rg) ;;
        *)    refuse "nothing is followed as '$item'" ;;
    esac
    [ -z "$VERSION" ] || ask+=(--upstream "$VERSION")

    # A blank GITHUB_OUTPUT makes the tool answer on stdout only: four answers there would overwrite
    # each other.
    if ! answer="$(GITHUB_OUTPUT='' release upstream-version "${ask[@]}" 2> "$OUT/$item.err")"; then
        stops+=("$label: upstream could not be asked - $(tail -n 1 "$OUT/$item.err")")
        echo "| $label | | | could not tell - see the run log |" >> "$SUMMARY"
        cat "$OUT/$item.err" >&2
        continue
    fi
    field() { sed -n "s/^$1=//p" <<< "$answer" | head -n 1; }
    pinned="$(field pinned)"; upstream="$(field upstream)"; update="$(field update)"
    major="$(field major)"; waiting="$(field waiting)"; source="$(field source)"

    case "$update" in
        yes)
            if ! release update "$upstream" "${move[@]}" > "$OUT/$item.log" 2>&1; then
                cat "$OUT/$item.log" >&2
                git checkout -q -- .
                stops+=("$label $upstream could not be pinned - see the run log")
                echo "| $label | \`$pinned\` | \`$upstream\` | could not be pinned |" >> "$SUMMARY"
                continue
            fi
            cat "$OUT/$item.log"
            git commit -q -am "Pin $label $upstream"
            moved+=("$label $upstream")
            components+=("$component")
            [ "$item" != cli ] || cli="$upstream"
            if [ "$major" = moved ]; then
                stops+=("$label's major version moved, $pinned -> $upstream: flags, configuration or an API change by definition")
                echo "| $label | \`$pinned\` | \`$upstream\` | **moved - major version** |" >> "$SUMMARY"
            else
                echo "| $label | \`$pinned\` | \`$upstream\` | moved |" >> "$SUMMARY"
            fi
            ;;
        rollback)
            # Never moved backwards by a schedule: a verified downgrade is not what decides this.
            stops+=("$label: $source points at $upstream, older than the pinned $pinned - to go back, dispatch with this one and version=$upstream")
            echo "| $label | \`$pinned\` | \`$upstream\` | **not moved - older than the pin** |" >> "$SUMMARY"
            ;;
        *)
            if [ -n "$waiting" ]; then
                echo "| $label | \`$pinned\` | \`$upstream\` | taken from $waiting, when it is old enough |" >> "$SUMMARY"
            else
                echo "| $label | \`$pinned\` | | current |" >> "$SUMMARY"
            fi
            ;;
    esac
done

title=""
if [ "${#moved[@]}" -gt 0 ]; then
    title="Update $(printf '%s, ' "${moved[@]}")"
    title="${title%, }"
fi
stop=""
if [ "${#stops[@]}" -gt 0 ]; then
    stop="$(printf '%s; ' "${stops[@]}")"
    stop="${stop%; }"
fi

{
    echo "moved=$([ "${#moved[@]}" -gt 0 ] && echo yes || echo no)"
    echo "title=$title"
    echo "components=${components[*]}"
    echo "cli=$cli"
    echo "stop=$stop"
} > "$OUT/outputs"
if [ -n "${GITHUB_OUTPUT:-}" ]; then
    cat "$OUT/outputs" >> "$GITHUB_OUTPUT"
fi
cat "$SUMMARY"
cat "$OUT/outputs"
