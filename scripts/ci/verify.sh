#!/usr/bin/env bash
#
# verify.sh — run the same verification tasks CI runs, summarize pass/fail, and collate every
# task's artifacts (lint/ktlint/detekt reports, JUnit XML, Paparazzi diffs, APKs) into one folder.
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
# The `web` task is OPT-IN (`--web`, or naming `web` in the filter): it runs the Node/Yarn webpack
# bundle and the shippable server distribution, which needs Node 22 + Yarn 1.x on PATH and is much
# slower than the JVM tasks. A default run skips it. CI still runs it in its own `web` job.
#
# The `device-ui` / `device-startup` tasks are the ones CI does NOT mirror: each runs ONE on-device
# benchmark axis (scripts/benchmark/device-<axis>.sh) and gates it on a performance regression
# against that device's locally-recorded baseline. They are opt-in via their own flag and exit 3 to
# mean "skipped — no device", which the runner maps to SKIP instead of FAIL.
#
# Usage:
#   scripts/ci/verify.sh                     # run everything except the opt-in web suite
#   scripts/ci/verify.sh static-analysis unit-test   # run only the named task(s)
#   scripts/ci/verify.sh --skip-apps         # everything except lint + app builds (fast inner loop)
#   scripts/ci/verify.sh --web               # also run the browser bundle + server distribution
#   scripts/ci/verify.sh --device-ui         # also run the on-device UI-scroll gate
#   scripts/ci/verify.sh --device-startup    # also run the on-device startup gate
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
# Mirrors the CI jobs: static-analysis, unit-test, screenshot, android-lint + apk.
ALL_TASKS=(
  "static-analysis|ktlintCheck detekt --continue"
  # Compiling the test APK is the only CI-sized check the benchmark module has; it is not run on a
  # device here (that is the opt-in device-* tasks below).
  "benchmark|:benchmark:compileBenchmarkKotlin"
  # The generic `test` lifecycle would also run each screenshot module's Paparazzi verify task;
  # those goldens are LFS objects and the dedicated `screenshot` task below owns them, so exclude
  # the modules here (the CI unit-test job does the same).
  "unit-test|test -x :features:browse:screenshot:test -x :features:contentdetail:screenshot:test -x :features:home:screenshot:test -x :features:listitems:screenshot:test -x :features:search:screenshot:test -x :features:settings:screenshot:test :core:models:api:jvmTest :core:nav:jvmTest :core:settings:jvmTest :core:viewmodel:jvmTest :core:repository:api:jvmTest :core:repository:remote:jvmTest :core:bitmaps:jvmTest :core:strings:real:jvmTest :core:ui:shell:jvmTest :features:browse:jvmTest :features:contentdetail:jvmTest :features:home:jvmTest :features:listitems:jvmTest :features:search:jvmTest :features:settings:jvmTest :testing:perfbench-core:jvmTest :testing:perfbench:test :apps:server:test --continue"
  "screenshot|verifyPaparazziDebug --continue"
  "android-lint|:apps:android:lintRelease"
  "apk|:apps:android:assembleDebug :apps:android:assembleRelease"
  "jvm-app|:apps:jvm:assemble"
  # Browser client + the server distribution that ships it. Needs Node 22 + Yarn 1.x on PATH and
  # -Psage.js=true (the JS modules are gated behind that flag).
  "web|-Psage.js=true :apps:js:ktlintCheck :apps:js:detekt :apps:js:jsBrowserDistribution :apps:server:installDist"
)
# The screenshot/lint/APK/JVM-build/web tasks are the heavy ones: they need an Android SDK
# (local.properties / ANDROID_HOME), package the desktop app, or run the Node/Yarn browser build.
# --skip-apps leaves them out for a quick inner loop; `web` is also opt-in via --web.
APP_BUILD_TASKS="benchmark screenshot android-lint apk jvm-app web"

# The opt-in on-device tasks (run by scripts/benchmark/device-<axis>.sh, not Gradle). Each axis is
# selected by its own flag; the axis script exits 3 to mean "skipped — a flag, not a failure" (no
# device), which the runner maps to SKIP instead of FAIL.
DEVICE_AXES="ui startup"

# ---- arg parsing -----------------------------------------------------------
filter=()
skip_apps=0
rerun=0
web=0
device_ui=0
device_startup=0
while [ $# -gt 0 ]; do
  arg="$1"
  case "$arg" in
    -h|--help) awk 'NR>1 && /^#/ {sub(/^# ?/, ""); print; next} NR>1 {exit}' "${BASH_SOURCE[0]}"; exit 0 ;;
    --list) printf '%s\n' "${ALL_TASKS[@]%%|*}"; for axis in $DEVICE_AXES; do echo "device-$axis"; done; exit 0 ;;
    --skip-apps) skip_apps=1 ;;
    --web) web=1 ;;
    --device-ui) device_ui=1 ;;
    --device-startup) device_startup=1 ;;
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
    case "$name" in
      device-ui|device-startup) ;;
      *)
        if [[ "$valid_names" != *" $name "* ]]; then
          echo "unknown task: $name (see --list)" >&2
          exit 2
        fi
        ;;
    esac
  done
fi

selected=()
for entry in "${ALL_TASKS[@]}"; do
  name="${entry%%|*}"
  # The web suite is opt-in (--web), or explicitly named in the filter.
  if [ "$name" = "web" ] && [ "$web" != 1 ] && [[ "$filter_text" != *" web "* ]]; then continue; fi
  if [ "$skip_apps" = 1 ] && [[ " $APP_BUILD_TASKS " == *" $name "* ]]; then continue; fi
  if [ "${#filter[@]}" -gt 0 ] && [[ "$filter_text" != *" $name "* ]]; then continue; fi
  selected+=("$entry")
done
# Each device task is opt-in via its own flag; honor a name filter like any task.
if [ "$device_ui" = 1 ] && { [ "${#filter[@]}" -eq 0 ] || [[ "$filter_text" == *" device-ui "* ]]; }; then
  selected+=("device-ui")
fi
if [ "$device_startup" = 1 ] && { [ "${#filter[@]}" -eq 0 ] || [[ "$filter_text" == *" device-startup "* ]]; }; then
  selected+=("device-startup")
fi
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

results=()   # "name|PASS/FAIL/SKIP|seconds"
overall=0
for entry in "${selected[@]}"; do
  name="${entry%%|*}"; args="${entry#*|}"
  if [[ "$name" == device-* ]]; then
    axis="${name#device-}"
    printf '%s──▶ %-16s%s scripts/benchmark/device-%s.sh\n' "$B" "$name" "$Z" "$axis"
    start=$SECONDS
    "scripts/benchmark/device-$axis.sh" >"$LOGS/$name.log" 2>&1
    rc=$?
  else
    printf '%s──▶ %-16s%s ./gradlew %s\n' "$B" "$name" "$Z" "$args"
    start=$SECONDS
    # shellcheck disable=SC2086
    "$GRADLE" $args "${GRADLE_FLAGS[@]}" >"$LOGS/$name.log" 2>&1
    rc=$?
  fi
  dur=$((SECONDS - start))
  if [ "$rc" -eq 0 ]; then
    printf '    %sPASS%s in %ds\n' "$G" "$Z" "$dur"
    results+=("$name|PASS|$dur")
  elif [ "$rc" -eq 3 ]; then
    # The device script's "no device attached" flag, not a failure.
    printf '    %sSKIP%s in %ds (see %s)\n' "$Y" "$Z" "$dur" "$LOGS/$name.log"
    results+=("$name|SKIP|$dur")
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
if [[ "$ran" == *" screenshot "* ]]; then
  # Paparazzi failure / diff images live under build/paparazzi, not build/reports.
  find . "${prune[@]}" -o -path '*/build/paparazzi' -type d -print -prune | while read -r d; do
    mod=$(echo "$d" | sed 's|^\./||;s|/build/paparazzi$||;s|/|-|g')
    mkdir -p "$OUT/paparazzi/$mod" && cp -r "$d"/. "$OUT/paparazzi/$mod"/
  done
fi
for axis in $DEVICE_AXES; do
  if [[ "$ran" == *" device-$axis "* ]] && [ -d "build/perfbench-device/$axis/current" ]; then
    mkdir -p "$OUT/device-$axis"
    cp -r "build/perfbench-device/$axis/current/." "$OUT/device-$axis/"
  fi
done

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
  desc paparazzi    "screenshot diff/failure images (per module)"
  desc apk          "debug + release APKs (apps/android/build/outputs/apk; release is unsigned)"
  desc device-ui    "on-device ui-axis result (only when --device-ui ran)"
  desc device-startup "on-device startup-axis result (only when --device-startup ran)"
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
