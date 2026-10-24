#!/bin/sh
set -e
path=${1:?usage: tools/sync.sh <path> [source branch]}
src=${2:-$(git rev-parse --abbrev-ref HEAD)}
tree=$(git rev-parse "$src:$path")
for branch in $(git for-each-ref --format='%(refname:short)' refs/heads | grep -E '^([0-9]+\.[0-9.]+|main)$'); do
    [ "$branch" = "$src" ] && continue
    [ "$(git rev-parse "$branch:$path" 2>/dev/null)" = "$tree" ] && continue
    index=$(mktemp)
    rm -f "$index"
    GIT_INDEX_FILE=$index git read-tree "$branch"
    GIT_INDEX_FILE=$index git rm --cached -f -r -q --ignore-unmatch "$path"
    GIT_INDEX_FILE=$index git read-tree --prefix="$path/" "$tree"
    new=$(GIT_INDEX_FILE=$index git write-tree)
    rm -f "$index"
    old=$(git rev-parse "refs/heads/$branch")
    commit=$(git commit-tree "$new" -p "$old" -m "chore: sync $path from $src")
    git update-ref -m "sync $path from $src" "refs/heads/$branch" "$commit" "$old"
    echo "synced $path to $branch"
done
