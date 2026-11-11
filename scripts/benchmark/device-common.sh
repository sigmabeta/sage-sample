#!/usr/bin/env bash
#
# device-common.sh — the shared engine behind the per-axis device benchmark scripts
# (device-ui.sh, device-startup.sh). Sourced, never run directly.
#
# A wrapper sets two variables and calls `benchmark_device_main "$@"`:
#   AXIS        the perfbench axis id ("ui", "startup")
#   TEST_CLASS  the fully-qualified instrumentation test class that measures it
#
# It runs the axis on EVERY attached device: only that axis's test class (AGP fans out to all
# connected devices, in the reused Gradle daemon), then diffs each device's result against that
# device's locally-recorded baseline with `perfbench diff --json` (ratio = baseline ÷ current;
# below the threshold — default 0.95 — is a regression).
#
# Unlike the audio harness this was adapted from, there is no corpus to scan, push, or pin: the
# sample's demo content is deterministic, and each UI scenario starts on its screen through the
# app's own deep link.
#
# The device set is deliberately NOT pinned to one serial: AGP's `--serial` option throws
# UnsupportedOperationException on AGP 9, and the ANDROID_SERIAL environment is read at
# configuration time, so a reused Gradle daemon can never see a newly exported value. Fan-out needs
# neither. (A device attached over two transports — e.g. mDNS and IP — would be run twice and
# collide, so that is detected and the run is skipped with a flag.)
#
# Exit codes, shared by every axis script:
#   0  passed — or the baseline was (re)recorded
#   3  skipped — no device, or a device attached over two transports; a FLAG, not a failure
#   1  failed — the test run failed, or an axis regressed past the threshold
#
# The baseline is *local* and never committed; the first run (or `--record`) writes it. It is keyed
# by each device's hardware identity (model + ABI), so a new device records a fresh baseline rather
# than gating one device's numbers against another's.
#
# Usage (as printed by `-h`, per wrapper):
#   scripts/benchmark/device-<axis>.sh [--record] [--threshold <ratio>]
#
# Environment:
#   SAGE_SAMPLE_PERF_BASELINE   baseline root (default build/perfbench-baseline/device)
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

EXIT_PASS=0
EXIT_FAIL=1
EXIT_SKIP=3

ADDITIONAL_OUT="$ROOT/benchmark/build/outputs/connected_android_test_additional_output"

: "${AXIS:?device-common.sh must be sourced with AXIS set}"
: "${TEST_CLASS:?device-common.sh must be sourced with TEST_CLASS set}"

THRESHOLD="0.95"
RECORD=0
OUT="$ROOT/build/perfbench-device/$AXIS"
CURRENT="$OUT/current"
RESULT_FILE="results-$AXIS.json"

if [ -t 1 ]; then R=$'\e[31m'; G=$'\e[32m'; Y=$'\e[33m'; B=$'\e[1m'; Z=$'\e[0m'; else R= G= Y= B= Z=; fi

die() { echo "${R}error:${Z} $*" >&2; exit "$EXIT_FAIL"; }
usage() { echo "${R}error:${Z} $*" >&2; exit 2; }
flag() { echo "${Y}flag:${Z} $*" >&2; }
skip() { flag "$1"; exit "$EXIT_SKIP"; }

# Absolute-izes a path that may not exist yet. BSD `realpath` (macOS) has no `-m`, and the device
# host is a Mac, so normalise without it: default relative paths against $PWD and squeeze duplicate
# slashes. Callers never pass `..` segments.
absolute_path() {
  local path="$1"
  case "$path" in
    /*) ;;
    *) path="$PWD/$path" ;;
  esac
  while [ "${path#*//}" != "$path" ]; do path="${path%%//*}/${path#*//}"; done
  printf '%s\n' "$path"
}

# The baseline dir for one device, from its result's fingerprint (model + ABI), under the override root.
baseline_dir_for() {
  # -j (no trailing newline) so tr's complement doesn't turn the newline into a stray trailing '_'.
  local label
  label="$(jq -jr '"\(.machine.model)-\(.machine.abi)"' "$1" 2>/dev/null | tr -c 'A-Za-z0-9._-' '_')"
  [ -n "$label" ] || label="unknown"
  absolute_path "${SAGE_SAMPLE_PERF_BASELINE:-$ROOT/build/perfbench-baseline/device}/$label"
}

# `timeout` is GNU coreutils; macOS ships none by default. It only bounds the cheap hardware-id
# probe, so fall back to a plain call rather than making the whole axis unavailable.
bounded_probe() {
  if command -v timeout >/dev/null 2>&1; then
    timeout 20 "$@"
  else
    "$@"
  fi
}

# ---- the one entry point a wrapper calls -----------------------------------
benchmark_device_main() {
  # ---- arg parsing ---------------------------------------------------------
  while [ $# -gt 0 ]; do
    case "$1" in
      -h|--help)
        cat <<EOF
device-$AXIS.sh — run the on-device $AXIS benchmark on every attached device and gate on a regression.

Runs :benchmark:connectedBenchmarkAndroidTest filtered to $TEST_CLASS on all connected
devices, compares each device's measured $AXIS axis against its local baseline, and fails on a
regression or a test failure. A missing device is a flag (exit 3), not a failure.

Usage:
  scripts/benchmark/device-$AXIS.sh [options]

Options:
  --record             (re)record each device's baseline from this run instead of gating against it
  --threshold <ratio>  regress when a device's geometric-mean ratio drops below <ratio>  (default: 0.95)

Environment:
  SAGE_SAMPLE_PERF_BASELINE   baseline root (default build/perfbench-baseline/device)

Artifacts: build/perfbench-device/$AXIS/ (current/<device>/$RESULT_FILE).
EOF
        return 0
        ;;
      --record) RECORD=1 ;;
      --threshold) [ $# -ge 2 ] || usage "--threshold needs a value"; THRESHOLD="$2"; shift ;;
      --threshold=*) THRESHOLD="${1#--threshold=}" ;;
      -*) usage "unknown option: $1" ;;
      *) usage "unexpected argument: $1" ;;
    esac
    shift
  done

  awk -v t="$THRESHOLD" 'BEGIN { exit !(t + 0 > 0 && t + 0 <= 1) }' ||
    usage "--threshold must be a ratio in (0, 1] (was '$THRESHOLD')"

  command -v adb >/dev/null || die "adb not found on PATH"
  command -v jq >/dev/null || die "jq not found on PATH (needed to read the diff report)"

  # ---- the device set ------------------------------------------------------
  # Run on everything attached. But one phone can be attached over two transports at once (mDNS and
  # IP); AGP would then run two sessions on it and collide. Group the online serials by hardware
  # serial (ro.serialno) and skip if any device appears twice.
  #
  # Plain newline-separated strings rather than arrays: macOS bash 3.2 errors on `${#empty[@]}` under
  # `set -u`, and this script is also run from the Mac device host.
  local online
  online="$(adb devices | awk 'NR > 1 && $2 == "device" { print $1 }')"
  [ -n "$online" ] || skip "no device attached — attach a physical device (or fix adb authorization) and re-run"

  local device_count
  device_count="$(printf '%s\n' "$online" | wc -l | tr -d ' ')"

  local pairs=""
  local serial hw
  while IFS= read -r serial; do
    [ -n "$serial" ] || continue
    hw="$(bounded_probe adb -s "$serial" shell getprop ro.serialno 2>/dev/null | tr -d '\r')"
    [ -n "$hw" ] || hw="$serial"
    # Tab-separated so a serial containing spaces stays a single field.
    pairs+="$hw"$'\t'"$serial"$'\n'
  done <<<"$online"

  local duplicated
  duplicated="$(printf '%s' "$pairs" | cut -f1 | sort | uniq -d)"
  if [ -n "$duplicated" ]; then
    skip "a device is attached over more than one transport ($(printf '%s' "$duplicated" | tr '\n' ' ')) — adb disconnect the duplicate(s) and re-run"
  fi

  echo "${B}On-device $AXIS benchmark${Z} — $device_count device(s): $(printf '%s' "$online" | tr '\n' ' ')"

  # ---- run this axis on every device ---------------------------------------
  # No serial is passed: AGP fans `connectedBenchmarkAndroidTest` out to every attached device, which
  # keeps the reused Gradle daemon in play (see the header on why pinning a serial doesn't).
  echo "==> Running the $AXIS benchmark (${TEST_CLASS##*.}) on all devices"
  rm -rf "$ADDITIONAL_OUT"
  ./gradlew --console=plain \
    -Pandroid.testInstrumentationRunnerArguments.class="$TEST_CLASS" \
    :benchmark:connectedBenchmarkAndroidTest ||
    die "the on-device $AXIS benchmark failed"

  # ---- collect -------------------------------------------------------------
  rm -rf "$CURRENT"
  mkdir -p "$CURRENT"
  local results_file
  results_file="$(mktemp)"
  find "$ADDITIONAL_OUT" -name "$RESULT_FILE" -type f 2>/dev/null | sort >"$results_file"
  if [ ! -s "$results_file" ]; then
    rm -f "$results_file"
    die "the run produced no $RESULT_FILE under $ADDITIONAL_OUT"
  fi

  # ---- record or gate, per device ------------------------------------------
  local regressed=0
  local flagged=0
  local compared=0
  local recorded=0
  local f baseline label report mean
  while IFS= read -r f; do
    baseline="$(baseline_dir_for "$f")"
    label="$(basename "$baseline")"
    mkdir -p "$CURRENT/$label"
    cp "$f" "$CURRENT/$label/$RESULT_FILE"

    if [ "$RECORD" -eq 1 ] || [ ! -f "$baseline/$RESULT_FILE" ]; then
      mkdir -p "$baseline"
      cp "$f" "$baseline/$RESULT_FILE"
      printf '    %-24s %srecorded%s → %s\n' "$label" "$B" "$Z" "$baseline/$RESULT_FILE"
      recorded=$((recorded + 1))
      continue
    fi

    # Diff against the sanitized copy, never $f: AGP's per-device output dir is named after the
    # device (e.g. "Pixel 6a - 13"), and its spaces would be split by Gradle's --args into bogus
    # positional arguments. $CURRENT/$label/$RESULT_FILE is the space-free copy made above.
    report="$(./gradlew -q --console=plain :testing:perfbench:run \
      --args="diff --a $baseline/$RESULT_FILE --b $CURRENT/$label/$RESULT_FILE --json" 2>/dev/null)"
    if [ $? -ne 0 ]; then
      flag "$label: diff failed for the $AXIS axis"
      flagged=1
      continue
    fi
    if ! printf '%s' "$report" | jq -e '.geometric_mean' >/dev/null 2>&1; then
      # No JSON: the harness refused to compare (scenario drift). Not a regression.
      flag "$label: cannot compare the $AXIS axis — $(printf '%s' "$report" | head -1)"
      flagged=1
      continue
    fi
    mean="$(printf '%s' "$report" | jq -r '.geometric_mean')"
    compared=$((compared + 1))
    if awk -v m="$mean" -v t="$THRESHOLD" 'BEGIN { exit !(m < t) }'; then
      printf '    %-24s %sx  %sREGRESSION%s\n' "$label" "$mean" "$R" "$Z"
      regressed=1
    else
      printf '    %-24s %sx  %sok%s\n' "$label" "$mean" "$G" "$Z"
    fi
  done <"$results_file"
  rm -f "$results_file"

  # ---- verdict -------------------------------------------------------------
  if [ "$regressed" -eq 1 ]; then
    echo "${R}${B}x the $AXIS axis regressed on at least one device${Z} — re-run once to rule out noise, or --record to accept"
    return "$EXIT_FAIL"
  fi
  if [ "$flagged" -eq 1 ]; then
    echo "${Y}flagged${Z}: no regression found in the $compared device(s) compared — see the flags above for what was skipped"
    return "$EXIT_PASS"
  fi
  if [ "$recorded" -gt 0 ] && [ "$compared" -eq 0 ]; then
    echo "${G}${B}✔ recorded $recorded $AXIS baseline(s)${Z} — re-run without --record to gate — artifacts in $OUT/"
    return "$EXIT_PASS"
  fi
  echo "${G}${B}✔ no $AXIS regression${Z} ($compared device(s) compared) — artifacts in $OUT/"
  return "$EXIT_PASS"
}
