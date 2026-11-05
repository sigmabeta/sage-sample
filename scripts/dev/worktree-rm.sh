#!/usr/bin/env bash
#
# worktree-rm.sh — remove a worktree that contains the SAGE submodule.
#
# A plain `git worktree remove` can refuse a worktree with a submodule; this retries directly
# and falls back to deleting the directory plus `git worktree prune`.
#
# Do NOT run `git submodule deinit` here: the submodule config lives in the common .git/config,
# so deinitialising in a linked worktree also deinitialises the main checkout.
#
# Usage:
#   scripts/dev/worktree-rm.sh <dir> [--force]
#
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DIR=""
FORCE=0

usage() { awk 'NR>1 && /^#/ {sub(/^# ?/, ""); print; next} NR>1 {exit}' "${BASH_SOURCE[0]}"; }

while [ $# -gt 0 ]; do
  case "$1" in
    -h|--help) usage; exit 0 ;;
    --force) FORCE=1; shift ;;
    -*) echo "unknown option: $1" >&2; exit 2 ;;
    *) if [ -z "$DIR" ]; then DIR="$1"; else echo "unexpected argument: $1" >&2; exit 2; fi; shift ;;
  esac
done

if [ -z "$DIR" ]; then usage; exit 2; fi
case "$DIR" in /*) ;; *) DIR="$PWD/$DIR" ;; esac
if [ ! -e "$DIR" ]; then echo "no such worktree: $DIR" >&2; exit 2; fi

real() { (cd "$1" 2>/dev/null && pwd -P) || echo "$1"; }
if [ "$(real "$DIR")" = "$(real "$ROOT")" ]; then
  echo "refusing to remove the main checkout: $DIR" >&2
  exit 2
fi

if [ "$FORCE" = 1 ]; then
  rm -rf "$DIR"
  git -C "$ROOT" worktree prune
  echo "removed $DIR"
  exit 0
fi

if git -C "$ROOT" worktree remove --force "$DIR" >/dev/null 2>&1; then
  echo "removed $DIR"
else
  rm -rf "$DIR"
  git -C "$ROOT" worktree prune
  echo "removed $DIR (deleted directly; registration pruned)"
fi
