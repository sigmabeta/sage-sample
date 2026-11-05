#!/usr/bin/env bash
#
# check-worktree-hygiene.sh — pre-merge/pre-handoff checks for a ticket worktree.
#
# Fails when the tree is dirty, a stash exists (the stash is shared across linked worktrees),
# the SAGE submodule is uninitialised or out of date, or the pinned SAGE commit is not on a
# remote branch. A detached HEAD is a warning, not a failure.
#
# Usage:
#   scripts/dev/check-worktree-hygiene.sh [dir]
#
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DIR="${1:-$ROOT}"

if [ ! -d "$DIR" ]; then echo "no such directory: $DIR" >&2; exit 2; fi
if ! git -C "$DIR" rev-parse --git-dir >/dev/null 2>&1; then
  echo "not a git worktree: $DIR" >&2; exit 2
fi

if [ -t 1 ]; then R=$'\e[31m'; G=$'\e[32m'; Y=$'\e[33m'; Z=$'\e[0m'; else R= G= Y= Z=; fi
fail=0
bad() { printf '%sFAIL%s %s\n' "$R" "$Z" "$1"; fail=1; }
warn() { printf '%sWARN%s %s\n' "$Y" "$Z" "$1"; }
ok() { printf '%sPASS%s %s\n' "$G" "$Z" "$1"; }

echo "worktree: $DIR"
echo "branch:   $(git -C "$DIR" rev-parse --abbrev-ref HEAD)"

stash="$(git -C "$DIR" stash list 2>/dev/null || true)"
if [ -n "$stash" ]; then
  bad "git stash is not empty — the stash is shared across linked worktrees; drop or pop it first"
  echo "$stash" | sed 's/^/       /'
else
  ok "no stash entries"
fi

status="$(git -C "$DIR" status --porcelain 2>/dev/null || true)"
if [ -n "$status" ]; then
  bad "working tree is dirty"
  echo "$status" | head -20 | sed 's/^/       /'
else
  ok "working tree clean"
fi

branch="$(git -C "$DIR" rev-parse --abbrev-ref HEAD)"
if [ "$branch" = "HEAD" ]; then
  warn "detached HEAD — check out the ticket branch before committing"
fi

if [ -e "$DIR/sage/.git" ]; then
  sub="$(git -C "$DIR" submodule status --recursive 2>/dev/null || true)"
  if printf '%s\n' "$sub" | grep -Eq '^[+-U]'; then
    bad "SAGE submodule is uninitialised, out of date, or conflicted"
    printf '%s\n' "$sub" | grep -E '^[+-U]' | sed 's/^/       /'
  else
    ok "SAGE submodule matches the gitlink"
  fi

  pin="$(git -C "$DIR" rev-parse :sage 2>/dev/null || true)"
  if [ -n "$pin" ]; then
    if git -C "$DIR/sage" branch -r --contains "$pin" >/dev/null 2>&1; then
      ok "pinned SAGE commit is on a remote branch"
    else
      bad "pinned SAGE commit ${pin:0:8} is not on any remote branch — CI checkout would fail"
    fi
  fi
else
  warn "SAGE submodule not initialised"
fi

echo
if [ "$fail" = 0 ]; then echo "hygiene: OK"; else echo "hygiene: FAILED"; fi
exit "$fail"
