#!/usr/bin/env bash
#
# verify.sh — run the same verification tasks CI runs, summarize pass/fail, and collate every
# task's artifacts (lint/ktlint/detekt reports, JUnit XML, APKs) into one folder.
#
# It verifies the WORKING TREE, not the commit you're about to push: it warns (without failing)
# when there are uncommitted/untracked changes, when the SAGE submodule has local modifications,
# or when the pinned SAGE commit is not on any remote branch — CI checks the submodule out
# recursively, so that push would fail at checkout.
#
# Mirrors the gradle commands in .github/workflows/ci.yml. CI splits these across parallel jobs;
# locally they run sequentially with --continue-style independence: a failing task does NOT stop
# the others, so one run gives you the full picture. Differences from CI, on purpose: the Gradle
# daemon is left on (faster local reruns; CI uses --no-daemon) and --max-workers is left at the
# machine default.
#
# Usage:
#   scripts/ci/verify.sh                     # run everything
#   scripts/ci/verify.sh static-analysis unit-test   # run only the named task(s)
#   scripts/ci/verify.sh --skip-apps         # everything except lint + app builds (fast inner loop)
#   scripts/ci/verify.sh --rerun             # force every task to re-run (Gradle --rerun-tasks)
#   scripts/ci/verify.sh --list              # list task names
#   VERIFY_OUT=/tmp/v scripts/ci/verify.sh   # override the output folder
#
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

OUT="${VERIFY_OUT:-build/verification}"
LOGS="$OUT/logs"
GRADLE="./gradlew"
# Match CI's caching flags; plain console so logs are grep-friendly.
GRADLE_FLAGS=(--build-cache --configuration-cache --console=plain)

# name | gradle args. Order = cheapest feedback first, then the jobs that need the Android SDK.
# Mirrors the CI jobs: static-analysis, unit-test, android-lint + apk.
ALL_TASKS=(
  "static-analysis|ktlintCheck detekt --continue"
  "unit-test|test :core:bitmaps:jvmTest --continue"
  "android-lint|:apps:android:lintRelease"
  "apk|:apps:android:assembleDebug :apps:android:assembleRelease"
  "jvm-app|:apps:jvm:assemble"
)
# The lint/APK/JVM-build tasks are the heavy ones: they need an Android SDK (local.properties /
# ANDROID_HOME) or package the desktop app. --skip-apps leaves them out for a quick inner loop.
APP_BUILD_TASKS="android-lint apk jvm-app"

# ---- arg parsing -----------------------------------------------------------
filter=()
skip_apps=0
rerun=0
while [ $# -gt 0 ]; do
  arg="$1"
  case "$arg" in
    -h|--help) awk 'NR>1 && /^#/ {sub(/^# ?/, ""); print; next} NR>1 {exit}' "${BASH_SOURCE[0]}"; exit 0 ;;
    --list) printf '%s\n' "${ALL_TASKS[@]%%|*}"; exit 0 ;;
    --skip-apps) skip_apps=1 ;;
    --rerun|--rerun-tasks) rerun=1 ;;
    -*) echo "unknown option: $arg" >&2; exit 2 ;;
    *) filter+=("$arg") ;;
  esac
  shift
done

# --rerun forces a from-scratch build (ignores up-to-date state AND build-cache hits).
[ "$rerun" = 1 ] && GRADLE_FLAGS+=(--rerun-tasks)

valid_names=" ${ALL_TASKS[*]%%|*} "
# bash 3.2 (macOS) treats "${empty[@]}" as unbound under `set -u`; build a guarded text form and
# only expand the array inside a length check.
filter_text=""
if [ "${#filter[@]}" -gt 0 ]; then
  filter_text=" ${filter[*]} "
  for name in "${filter[@]}"; do
    if [[ "$valid_names" != *" $name "* ]]; then
      echo "unknown task: $name (see --list)" >&2
      exit 2
    fi
  done
fi

selected=()
for entry in "${ALL_TASKS[@]}"; do
  name="${entry%%|*}"
  if [ "$skip_apps" = 1 ] && [[ " $APP_BUILD_TASKS " == *" $name "* ]]; then continue; fi
  if [ "${#filter[@]}" -gt 0 ] && [[ "$filter_text" != *" $name "* ]]; then continue; fi
  selected+=("$entry")
done
if [ "${#selected[@]}" -eq 0 ]; then echo "no tasks selected" >&2; exit 2; fi

# ---- colors (only on a tty) ------------------------------------------------
if [ -t 1 ]; then R=$'\e[31m'; G=$'\e[32m'; Y=$'\e[33m'; B=$'\e[1m'; Z=$'\e[0m'; else R= G= Y= B= Z=; fi

# ---- run -------------------------------------------------------------------
# OUT is wiped below, so refuse anything that would take the repo (or worse) with it.
case "$(realpath -m "$OUT" 2>/dev/null || echo "$OUT")" in
  / | "$ROOT" | "$HOME" | "$ROOT"/build) echo "refusing to use '$OUT' as VERIFY_OUT (it gets rm -rf'd)" >&2; exit 2 ;;
esac

# The run checks the working tree; flag the ways that can diverge from what CI will see.
warnings=()
if [ -n "$(git status --porcelain --ignore-submodules=dirty)" ]; then
  warnings+=("working tree has uncommitted or untracked changes — this run may not match what you push")
fi
if [ ! -e sage/.git ]; then
  warnings+=("SAGE submodule is not initialised — run 'git submodule update --init --recursive'")
else
  sage_head=$(git -C sage rev-parse HEAD 2>/dev/null)
  if [ -n "$(git -C sage status --porcelain)" ]; then
    warnings+=("SAGE submodule has uncommitted changes — CI builds the pinned commit, not this tree")
  fi
  if [ -n "$sage_head" ] && [ -z "$(git -C sage branch -r --contains "$sage_head" 2>/dev/null)" ]; then
    warnings+=("SAGE submodule is at ${sage_head:0:8}, which no remote branch contains — push SAGE or CI checkout fails")
  fi
fi
for w in ${warnings[@]+"${warnings[@]}"}; do echo "${Y}warning:${Z} $w" >&2; done

rm -rf "$OUT"
mkdir -p "$LOGS"
echo "${B}Running ${#selected[@]} verification task(s); logs -> $LOGS${Z}"

results=()   # "name|PASS/FAIL|seconds"
overall=0
for entry in "${selected[@]}"; do
  name="${entry%%|*}"; args="${entry#*|}"
  printf '%s──▶ %-16s%s ./gradlew %s\n' "$B" "$name" "$Z" "$args"
  start=$SECONDS
  # shellcheck disable=SC2086
  "$GRADLE" $args "${GRADLE_FLAGS[@]}" >"$LOGS/$name.log" 2>&1
  rc=$?
  dur=$((SECONDS - start))
  if [ "$rc" -eq 0 ]; then
    printf '    %sPASS%s in %ds\n' "$G" "$Z" "$dur"
    results+=("$name|PASS|$dur")
  else
    printf '    %sFAIL%s in %ds (see %s)\n' "$R" "$Z" "$dur" "$LOGS/$name.log"
    results+=("$name|FAIL|$dur")
    overall=1
  fi
done

# ---- collate artifacts -----------------------------------------------------
# Same sweeps CI uses, keyed by module path. Prune the vendored sage submodule (separate build)
# and our own output folder.
echo "${B}Collating artifacts -> $OUT${Z}"
prune=(-path ./sage -prune -o -path "./$OUT" -prune)
ran=" "; for r in "${results[@]}"; do ran+="${r%%|*} "; done   # " name1 name2 … " of tasks that ran

# build/reports + JUnit XML from every module — generic (any task can produce them). On a partial
# run these may include reports left in build/ by an earlier full run.
find . "${prune[@]}" -o -path '*/build/reports' -type d -print -prune | while read -r d; do
  mod=$(echo "$d" | sed 's|^\./||;s|/build/reports$||;s|/|-|g')
  mkdir -p "$OUT/reports/$mod" && cp -r "$d"/. "$OUT/reports/$mod"/
done
find . "${prune[@]}" -o -path '*/build/test-results/*' -name 'TEST-*.xml' -print | while read -r f; do
  mod=$(echo "$f" | sed 's|^\./||;s|/build/test-results/.*$||;s|/|-|g')
  mkdir -p "$OUT/test-results/$mod" && cp "$f" "$OUT/test-results/$mod"/
done
# Task-specific outputs — only collate when that task actually ran this invocation, so a partial
# run doesn't surface stale artifacts from a previous build.
if [[ "$ran" == *" apk "* ]] && [ -d apps/android/build/outputs/apk ]; then
  mkdir -p "$OUT/apk"; cp -r apps/android/build/outputs/apk/. "$OUT/apk/"
fi

# ---- summary ---------------------------------------------------------------
summary="$OUT/summary.txt"
{
  echo "Verification summary — $(date '+%Y-%m-%d %H:%M:%S')"
  echo
  printf '  %-16s %-6s %8s\n' "TASK" "RESULT" "TIME"
  printf '  %-16s %-6s %8s\n' "----" "------" "----"
  for r in ${results[@]+"${results[@]}"}; do
    IFS='|' read -r n s d <<<"$r"
    printf '  %-16s %-6s %7ds\n' "$n" "$s" "$d"
  done
  echo
  if [ "$overall" -eq 0 ]; then echo "OVERALL: PASS"; else echo "OVERALL: FAIL"; fi
  for w in ${warnings[@]+"${warnings[@]}"}; do echo "WARNING: $w"; done
  echo
  echo "Artifacts under $OUT/:"
  printf '  %-14s %s\n' "logs/" "full gradle output per task (start here for any FAIL)"
  desc() { [ -d "$OUT/$1" ] && printf '  %-14s %s\n' "$1/" "$2"; }
  desc reports      "ktlint, detekt, android-lint, HTML test reports (per module)"
  desc test-results "JUnit XML (unit tests)"
  desc apk          "debug + release APKs (apps/android/build/outputs/apk; release is unsigned)"
} >"$summary"

echo
cat "$summary"
echo
if [ "$overall" -eq 0 ]; then
  echo "${G}${B}✔ all verification tasks passed${Z} — artifacts in $OUT/"
else
  echo "${R}${B}x verification failed${Z} — see $OUT/logs/ and the summary above"
fi
exit "$overall"
