#!/usr/bin/env bash
#
# worktree-add.sh — create a build-ready worktree for a ticket or chore branch.
#
# Unlike a bare `git worktree add`, this copies the main checkout's `local.properties` and
# initialises the SAGE submodule with the main checkout's objects as a reference clone, so
# each worktree does not clone another 81 MB.
#
# Usage:
#   scripts/dev/worktree-add.sh <name> [base-ref] [--branch <branch>] [--dir <path>] [--no-submodule]
#
# Defaults:
#   base-ref  HEAD
#   branch    worktree/<name>
#   dir       ${WORKTREE_BASE:-<repo>-worktrees}/<name>
#
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
NAME=""
BASE=""
BRANCH=""
DIR=""
SUBMODULE=1

usage() { awk 'NR>1 && /^#/ {sub(/^# ?/, ""); print; next} NR>1 {exit}' "${BASH_SOURCE[0]}"; }

while [ $# -gt 0 ]; do
  case "$1" in
    -h|--help) usage; exit 0 ;;
    --branch) BRANCH="${2:?--branch needs a value}"; shift 2 ;;
    --dir) DIR="${2:?--dir needs a value}"; shift 2 ;;
    --no-submodule) SUBMODULE=0; shift ;;
    -*) echo "unknown option: $1" >&2; exit 2 ;;
    *)
      if [ -z "$NAME" ]; then NAME="$1"
      elif [ -z "$BASE" ]; then BASE="$1"
      else echo "unexpected argument: $1" >&2; exit 2
      fi
      shift ;;
  esac
done

if [ -z "$NAME" ]; then usage; exit 2; fi
BASE="${BASE:-HEAD}"
BRANCH="${BRANCH:-worktree/$NAME}"
DIR="${DIR:-${WORKTREE_BASE:-$(dirname "$ROOT")/$(basename "$ROOT")-worktrees}/$NAME}"

if [ -e "$DIR" ]; then echo "refusing to reuse existing path: $DIR" >&2; exit 2; fi
mkdir -p "$(dirname "$DIR")"

if git -C "$ROOT" show-ref --verify --quiet "refs/heads/$BRANCH"; then
  echo "branch $BRANCH already exists; checking it out in the new worktree" >&2
  git -C "$ROOT" worktree add "$DIR" "$BRANCH"
else
  git -C "$ROOT" worktree add -b "$BRANCH" "$DIR" "$BASE"
fi || { echo "git worktree add failed" >&2; exit 1; }

if [ -f "$ROOT/local.properties" ]; then
  cp "$ROOT/local.properties" "$DIR/local.properties"
else
  echo "warning: $ROOT/local.properties is missing; set sdk.dir in the new worktree" >&2
fi

if [ "$SUBMODULE" = 1 ]; then
  if [ -d "$ROOT/.git/modules/sage" ]; then REF="$ROOT/.git/modules/sage"
  elif [ -d "$ROOT/sage" ]; then REF="$ROOT/sage"
  else REF=""
  fi

  if [ -n "$REF" ]; then
    git -C "$DIR" submodule update --init --recursive --reference "$REF" \
      || { echo "reference clone failed; retrying without --reference" >&2
           git -C "$DIR" submodule update --init --recursive; }
  else
    git -C "$DIR" submodule update --init --recursive
  fi
fi

echo
echo "worktree: $DIR"
echo "branch:   $BRANCH"
if [ "$SUBMODULE" = 1 ] && [ -e "$DIR/sage/.git" ]; then
  echo "sage:     $(git -C "$DIR/sage" rev-parse --short HEAD 2>/dev/null)"
fi
echo "next:     cd \"$DIR\" && scripts/ci/verify.sh --skip-apps"
