#!/usr/bin/env bash
#
# bump_submodule.sh - open (or update) a PR in couchbase-lite-java-ee-root that
# points a submodule at the latest commit of its branch.
#
# Usage: bump_submodule.sh [-s common|ce] [-b branch] [-u account] [-a assignee] [-n]
#   -s  submodule to bump (default: common)
#   -b  ee-root branch to target. The submodule follows the branch with the
#       same name, e.g. -b release/4.1 bumps to common's release/4.1 (default: master)
#   -u  gh account to act as, e.g. cbl-bot (default: the active gh account)
#   -a  GitHub user to assign the PR to
#   -n  dry run: show what would change, don't create anything
#
# Works entirely through the GitHub API, so it needs no checkout or SSH key,
# only `gh` logged in to the account it acts as. Safe to run on a schedule:
# it does nothing when the submodule is current or the open PR already is,
# and doesn't raise again a bump that was closed without merging.
#
set -euo pipefail

ROOT_REPO="couchbaselabs/couchbase-lite-java-ee-root"

# Prints the header comment, from line 3 up to the first line that isn't a comment.
usage() { awk 'NR > 2 { if (!/^#/) exit; sub(/^# ?/, ""); print }' "$0"; exit 1; }
die() { echo "bump-submodule: $*" >&2; exit 1; }

SUB=common
BASE=master
ACCOUNT=""
ASSIGNEE=""
DRY_RUN=false
while getopts "s:b:u:a:nh" opt; do
    case "$opt" in
        s) SUB="$OPTARG" ;;
        b) BASE="$OPTARG" ;;
        u) ACCOUNT="$OPTARG" ;;
        a) ASSIGNEE="$OPTARG" ;;
        n) DRY_RUN=true ;;
        *) usage ;;
    esac
done

command -v gh >/dev/null || die "gh isn't installed"

# Act as $ACCOUNT for every gh call without switching the active gh account.
if [ -n "$ACCOUNT" ]; then
    GH_TOKEN=$(gh auth token --hostname github.com --user "$ACCOUNT" 2>/dev/null) \
        || die "gh isn't logged in as $ACCOUNT"
    export GH_TOKEN
fi

# Note: on an error, `gh api` prints the error body to stdout even with -q,
# so results are judged by exit status, never by what was printed.
err_file=$(mktemp)
trap 'rm -f "$err_file"' EXIT

# gh_api_404 <args>: `gh api`, but returns 2 when GitHub answers 404 (doesn't
# exist) and dies on any other error (auth, rate limit, network), so a failed
# lookup is never mistaken for "not there".
gh_api_404() {
    gh api "$@" 2>"$err_file" && return 0
    grep -q '(HTTP 404)' "$err_file" && return 2
    die "gh api $1: $(cat "$err_file")"
}

info=$(gh api "repos/$ROOT_REPO/contents/$SUB?ref=$BASE" -q '"\(.type) \(.sha) \(.submodule_git_url)"') \
    || die "can't read $SUB on $ROOT_REPO $BASE"
read -r type old sub_url <<<"$info"
[ "$type" = submodule ] || die "$SUB isn't a submodule on $BASE"
sub_repo=$(echo "$sub_url" | sed -E 's#^.*github.com[:/]##; s#\.git$##')

# A submodule repo without a matching branch (e.g. no release branches) has nothing to bump.
rc=0
new=$(gh_api_404 "repos/$sub_repo/branches/$BASE" -q .commit.sha) || rc=$?
if [ "$rc" -eq 2 ]; then
    echo "$sub_repo has no branch $BASE, skipping"
    exit 0
fi
[ "$rc" -eq 0 ] || exit 1

if [ "$old" = "$new" ]; then
    echo "$SUB on $BASE is up to date (${old:0:8})"
    exit 0
fi

# What the bump brings in. PR numbers in the submodule's commit titles point
# at the submodule repo, not at ee-root.
if titles=$(gh api "repos/$sub_repo/compare/$old...$new" -q '.commits[].commit.message | split("\n")[0]'); then
    changes=$(echo "$titles" | sed -E "s#\(\#([0-9]+)\)#(${sub_repo}\#\1)#g; s#^#- #")
else
    changes="(couldn't list the commits between ${old:0:8} and ${new:0:8})"
fi

title="Update $SUB submodule"
body="Points \`$SUB\` at ${sub_repo}@${new:0:8} (\`$BASE\`).

Changes since ${old:0:8}:
$changes"

echo "$SUB on $BASE: ${old:0:8} -> ${new:0:8}"
echo
echo "$body"
echo

head_branch="bump/$SUB-${BASE//\//-}"
base_sha=$(gh api "repos/$ROOT_REPO/git/ref/heads/$BASE" -q .object.sha)

# An existing bump branch that already sits on the current base and points
# at the new commit needs nothing; don't push again and re-trigger CI.
rc=0
branch_sha=$(gh_api_404 "repos/$ROOT_REPO/git/ref/heads/$head_branch" -q .object.sha) || rc=$?
case "$rc" in
    0) ;;
    2) branch_sha="" ;;
    *) exit 1 ;;
esac

# A bump to this commit that someone closed without merging was declined;
# don't raise it again every day. A newer submodule commit raises a new PR.
declined=$(gh pr list --repo "$ROOT_REPO" --head "$head_branch" --base "$BASE" --state closed --json mergedAt,body \
    -q "[.[] | select(.mergedAt == null and (.body | contains(\"@${new:0:8}\")))] | length" 2>"$err_file") \
    || die "can't list closed bump PRs: $(cat "$err_file")"
if [ "$declined" -gt 0 ]; then
    echo "A bump of $SUB to ${new:0:8} on $BASE was closed without merging, skipping"
    exit 0
fi

open_pr=$(gh pr list --repo "$ROOT_REPO" --head "$head_branch" --base "$BASE" --state open --json url \
    -q '.[0].url // ""' 2>"$err_file") \
    || die "can't list open bump PRs: $(cat "$err_file")"
if [ -n "$branch_sha" ] && [ -n "$open_pr" ]; then
    parent=$(gh api "repos/$ROOT_REPO/commits/$branch_sha" -q '.parents[0].sha')
    linked=$(gh api "repos/$ROOT_REPO/contents/$SUB?ref=$branch_sha" -q .sha)
    if [ "$parent" = "$base_sha" ] && [ "$linked" = "$new" ]; then
        # The branch is current, but an earlier run may have updated it and then
        # failed to edit the PR, so bring the description up to date too.
        if [ "$(gh pr view "$open_pr" --json body -q .body)" = "$body" ]; then
            echo "The open PR is already up to date: $open_pr"
        elif $DRY_RUN; then
            echo "Dry run: would update the description of $open_pr"
        else
            gh pr edit "$open_pr" --body "$body" >/dev/null
            echo "Updated the description of $open_pr"
        fi
        exit 0
    fi
fi

if $DRY_RUN; then
    if [ -n "$open_pr" ]; then
        echo "Dry run: would point $head_branch at a new commit on $BASE and update $open_pr"
    else
        echo "Dry run: would point $head_branch at a new commit on $BASE and open a PR"
    fi
    exit 0
fi

# Build the commit with the Git Data API: graft the new gitlink into the
# base tree, commit it on top of the base, and point the bump branch at it.
base_tree=$(gh api "repos/$ROOT_REPO/git/commits/$base_sha" -q .tree.sha)
tree=$(gh api -X POST "repos/$ROOT_REPO/git/trees" \
    -f base_tree="$base_tree" \
    -f "tree[][path]=$SUB" -f "tree[][mode]=160000" -f "tree[][type]=commit" -f "tree[][sha]=$new" \
    -q .sha) || die "couldn't create the tree"
commit=$(gh api -X POST "repos/$ROOT_REPO/git/commits" \
    -f message="$title" -f tree="$tree" -f "parents[]=$base_sha" \
    -q .sha) || die "couldn't create the commit"

if [ -n "$branch_sha" ]; then
    gh api -X PATCH "repos/$ROOT_REPO/git/refs/heads/$head_branch" -f sha="$commit" -F force=true >/dev/null \
        || die "couldn't update $head_branch"
else
    gh api -X POST "repos/$ROOT_REPO/git/refs" -f ref="refs/heads/$head_branch" -f sha="$commit" >/dev/null \
        || die "couldn't create $head_branch"
fi

if [ -n "$open_pr" ]; then
    gh pr edit "$open_pr" --body "$body" >/dev/null
    echo "Updated: $open_pr"
else
    gh pr create --repo "$ROOT_REPO" --base "$BASE" --head "$head_branch" \
        --title "$title" --body "$body" ${ASSIGNEE:+--assignee "$ASSIGNEE"}
fi
