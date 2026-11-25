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
# than gating one device's numbers against another's. `--record` measures several passes (default
# 3) and records, per scenario, the pass with the median gate metric (`perfbench baseline`); the
# passes are kept under <baseline>/records/. A first run with no baseline records its single pass,
# flagged as such.
#
# Usage (as printed by `-h`, per wrapper):
#   scripts/benchmark/device-<axis>.sh [--record [--record-runs <n>]] [--threshold <ratio>]
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
RECORD_RUNS=3
OUT="$ROOT/build/perfbench-device/$AXIS"
CURRENT="$OUT/current"
RECORDINGS="$OUT/recordings"
RESULT_FILE="results-$AXIS.json"
DIFF_FILE="diff-$AXIS.json"

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

# The harness's device label for an attached serial: `<model>-<abi>` with every character outside
# [A-Za-z0-9._-] replaced by '_'. It is the key the result documents, the baselines and the
# InfluxDB device tag all share, so a device with no document can still be named.
device_label_for() {
  local serial="$1" model abi
  model="$(bounded_probe adb -s "$serial" shell getprop ro.product.model 2>/dev/null | tr -d '\r')"
  abi="$(bounded_probe adb -s "$serial" shell getprop ro.product.cpu.abi 2>/dev/null | tr -d '\r')"
  # -c on a newline-less printf, so the complement does not turn a trailing newline into '_'.
  printf '%s-%s' "$model" "$abi" | tr -c 'A-Za-z0-9._-' '_'
}

# The benchmark's own explanation for a device that produced no document. A macrobenchmark
# device-state refusal (LOW-BATTERY, a locked screen) writes its reason into that device's message
# file, which is the only place the cause survives the failed Gradle task.
device_note() {
  local model="$1" dir file note
  [ -n "$model" ] || return 0
  dir="$(find "$ADDITIONAL_OUT" -maxdepth 4 -type d -name "$model - *" 2>/dev/null | head -1)"
  [ -n "$dir" ] || return 0
  file="$(find "$dir" -maxdepth 1 -name 'additionaltestoutput.benchmark.message_*' 2>/dev/null | head -1)"
  [ -n "$file" ] || return 0
  note="$(grep -m1 -E 'ERRORS \(not suppressed\)|low battery|Unable to|java\.lang\.' "$file" 2>/dev/null |
    tr -d '\r' | cut -c1-200)"
  printf '%s' "$note"
}

# `timeout` is GNU coreutils; macOS ships none by default. It only bounds the cheap hardware-id
# probe, so fall back to a plain call rather than making the whole axis unavailable.
#
# stdin is /dev/null: every caller probes inside a `while read … done <<<"$online"` loop, and
# `adb shell` would otherwise read the rest of the device list, so only the first device was probed.
bounded_probe() {
  if command -v timeout >/dev/null 2>&1; then
    timeout 20 "$@" </dev/null
  else
    "$@" </dev/null
  fi
}

# Runs the axis once on every attached device. Each device's document is copied to
# $RECORDINGS/<pass>/<label>/ and to $CURRENT/<label>/, where the newest pass wins.
measure_axis() {
  local pass="$1" passes="$2" bench_rc=0 f label slot
  slot="$(printf '%02d' "$pass")"
  # No serial is passed: AGP fans `connectedBenchmarkAndroidTest` out to every attached device, which
  # keeps the reused Gradle daemon in play (see the header on why pinning a serial doesn't).
  echo "==> Running the $AXIS benchmark (${TEST_CLASS##*.}) on all devices — pass $pass of $passes"
  rm -rf "$ADDITIONAL_OUT"
  # tee needs $OUT to exist; on a fresh checkout nothing has created it yet, and under pipefail a
  # failed tee would read as a failed benchmark.
  mkdir -p "$OUT"
  ./gradlew --console=plain \
    -Pandroid.testInstrumentationRunnerArguments.class="$TEST_CLASS" \
    :benchmark:connectedBenchmarkAndroidTest 2>&1 |
    tee "$OUT/last-run.log" || bench_rc=$?
  [ "$passes" -gt 1 ] && cp "$OUT/last-run.log" "$OUT/run-$slot.log"
  if [ "$bench_rc" -ne 0 ]; then
    # One phone refusing to start fails the whole Gradle task: macrobenchmark's LOW-BATTERY and other
    # device-state checks abort that device's test, and AGP treats the task as failed. The phones
    # that did measure have already written their documents, so keep them — the refusal belongs to
    # one cell, not to everyone else's measurement.
    echo "${Y}warning:${Z} the benchmark task exited $bench_rc — keeping the results it produced"
  fi

  while IFS= read -r f; do
    [ -n "$f" ] || continue
    label="$(basename "$(baseline_dir_for "$f")")"
    mkdir -p "$RECORDINGS/$slot/$label" "$CURRENT/$label"
    cp "$f" "$RECORDINGS/$slot/$label/$RESULT_FILE"
    cp "$f" "$CURRENT/$label/$RESULT_FILE"
  done <<<"$(find "$ADDITIONAL_OUT" -name "$RESULT_FILE" -type f 2>/dev/null | sort)"
}

# Records one device's baseline as the per-scenario median of its recordings in this run
# (`perfbench baseline`), and keeps those recordings under <baseline>/records/ for provenance.
# The merge writes to a temp file first, so a refused merge leaves the previous baseline in place.
# Returns non-zero (after a flag) when the merge is refused.
record_baseline() {
  local label="$1" baseline="$2" passes="$3" joined="" count=0 f summary i=0
  for f in "$RECORDINGS"/*/"$label/$RESULT_FILE"; do
    [ -f "$f" ] || continue
    joined="${joined:+$joined,}$f"
    count=$((count + 1))
  done
  mkdir -p "$baseline"
  # </dev/null: never let the merge's Gradle run read the caller's stdin.
  if ! summary="$(./gradlew -q --console=plain :testing:perfbench:run \
    --args="baseline --runs $joined --out $baseline/$RESULT_FILE.new" </dev/null 2>/dev/null)"; then
    rm -f "$baseline/$RESULT_FILE.new"
    flag "$label: could not record the $AXIS baseline — $(printf '%s' "$summary" | grep -m1 '^Refusing' || echo 'perfbench baseline failed')"
    return 1
  fi
  mv "$baseline/$RESULT_FILE.new" "$baseline/$RESULT_FILE"
  rm -rf "$baseline/records"
  mkdir -p "$baseline/records"
  for f in "$RECORDINGS"/*/"$label/$RESULT_FILE"; do
    [ -f "$f" ] || continue
    i=$((i + 1))
    cp "$f" "$baseline/records/$(printf '%02d' "$i")-$RESULT_FILE"
  done

  printf '    %-24s %srecorded%s (median of %d) → %s\n' "$label" "$B" "$Z" "$count" "$baseline/$RESULT_FILE"
  # Only the report: a cold `:testing:perfbench:run` can print build output first.
  printf '%s\n' "$summary" | sed -n '/^Baseline: /,$p' | tail -n +2 | sed 's/^/      /'
  if [ "$RECORD" -eq 0 ]; then
    flag "$label: no $AXIS baseline existed, so this run's single recording became it — re-record with --record for a median of $RECORD_RUNS"
  elif [ "$count" -lt "$passes" ]; then
    flag "$label: only $count of $passes recording passes produced a result — the baseline is the median of $count"
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
  --record             (re)record each device's baseline instead of gating against it: measure
                       --record-runs passes and keep each scenario's median recording
  --record-runs <n>    passes a --record measures                                   (default: 3)
  --threshold <ratio>  regress when a device's geometric-mean ratio drops below <ratio>  (default: 0.95)

Environment:
  SAGE_SAMPLE_PERF_BASELINE   baseline root (default build/perfbench-baseline/device)

Artifacts: build/perfbench-device/$AXIS/ (current/<device>/$RESULT_FILE, plus
current/<device>/$DIFF_FILE — the perfbench diff --json report — when the device was compared).
EOF
        return 0
        ;;
      --record) RECORD=1 ;;
      --record-runs) [ $# -ge 2 ] || usage "--record-runs needs a value"; RECORD_RUNS="$2"; shift ;;
      --record-runs=*) RECORD_RUNS="${1#--record-runs=}" ;;
      --threshold) [ $# -ge 2 ] || usage "--threshold needs a value"; THRESHOLD="$2"; shift ;;
      --threshold=*) THRESHOLD="${1#--threshold=}" ;;
      -*) usage "unknown option: $1" ;;
      *) usage "unexpected argument: $1" ;;
    esac
    shift
  done

  awk -v t="$THRESHOLD" 'BEGIN { exit !(t + 0 > 0 && t + 0 <= 1) }' ||
    usage "--threshold must be a ratio in (0, 1] (was '$THRESHOLD')"
  case "$RECORD_RUNS" in
    '' | *[!0-9]* | 0) usage "--record-runs must be a positive integer (was '$RECORD_RUNS')" ;;
  esac

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

  # A dozing phone draws nothing: a startup axis never sees first content and a scroll axis records
  # no frames, so the measurement fails for a reason that has nothing to do with the code. A farm
  # phone is plugged in, so `stayon` keeps it awake for the whole axis instead of letting the screen
  # time out between iterations — nothing new here touches the screen, so a plain wake is not enough.
  # Every command below is idempotent. Best effort: a device that stays asleep shows up in the
  # per-device pass below rather than failing here.
  #
  # The UI scenarios drive portrait layouts, so auto-rotate goes off and the natural rotation is
  # pinned too: a phone lying on its side otherwise turns the app to landscape, a different layout
  # from the one the baseline measured. This must happen here, before the instrumentation connects.
  # When UiAutomation disconnects it restores the rotation state it saw at connect, so a lock set
  # only inside the test is undone at the end. Nothing turns auto-rotate back on afterwards.
  while IFS= read -r s; do
    [ -n "$s" ] || continue
    # </dev/null: `adb shell` would otherwise read the rest of the device list from this loop's
    # stdin, so only the first device was woken.
    adb -s "$s" shell svc power stayon true </dev/null >/dev/null 2>&1
    adb -s "$s" shell input keyevent KEYCODE_WAKEUP </dev/null >/dev/null 2>&1
    adb -s "$s" shell wm dismiss-keyguard </dev/null >/dev/null 2>&1
    adb -s "$s" shell settings put system accelerometer_rotation 0 </dev/null >/dev/null 2>&1
    adb -s "$s" shell settings put system user_rotation 0 </dev/null >/dev/null 2>&1
  done <<<"$online"

  # ---- run this axis on every device ---------------------------------------
  # A gate measures once. `--record` measures $RECORD_RUNS passes and records each device's
  # per-scenario median (see record_baseline): one recording is a single draw from a noisy
  # distribution, and a baseline that happens to be a fast draw makes later runs look regressed.
  local passes=1 pass
  [ "$RECORD" -eq 1 ] && passes="$RECORD_RUNS"
  rm -rf "$CURRENT" "$RECORDINGS"
  mkdir -p "$CURRENT"
  for ((pass = 1; pass <= passes; pass++)); do
    measure_axis "$pass" "$passes"
  done

  # ---- record or gate, per device ------------------------------------------
  local regressed=0
  local flagged=0
  local compared=0
  local recorded=0
  local dir baseline label report mean
  # A glob, not a read loop: the per-device diff runs Gradle, which would otherwise inherit the
  # loop's stdin and swallow the remaining entries.
  for dir in "$CURRENT"/*/; do
    [ -f "$dir$RESULT_FILE" ] || continue
    baseline="$(baseline_dir_for "$dir$RESULT_FILE")"
    label="$(basename "$baseline")"
    # A refused or failed compare must leave no diff document behind for a consumer to pick up.
    rm -f "$CURRENT/$label/$DIFF_FILE"

    if [ "$RECORD" -eq 1 ] || [ ! -f "$baseline/$RESULT_FILE" ]; then
      if record_baseline "$label" "$baseline" "$passes"; then
        recorded=$((recorded + 1))
      else
        flagged=1
      fi
      continue
    fi

    # Diff against the sanitized copy, never $f: AGP's per-device output dir is named after the
    # device (e.g. "Pixel 6a - 13"), and its spaces would be split by Gradle's --args into bogus
    # positional arguments. $CURRENT/$label/$RESULT_FILE is the space-free copy made above.
    # </dev/null: never let the diff's Gradle run read the caller's stdin.
    report="$(./gradlew -q --console=plain :testing:perfbench:run \
      --args="diff --a $baseline/$RESULT_FILE --b $CURRENT/$label/$RESULT_FILE --json" </dev/null 2>/dev/null)"
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
    # Keep the diff beside the result it compared, so a consumer (sagefarm's publisher reads
    # diff-<axis>.json next to results-<axis>.json) needn't re-run the compare.
    printf '%s\n' "$report" >"$CURRENT/$label/$DIFF_FILE"
    mean="$(printf '%s' "$report" | jq -r '.geometric_mean')"
    compared=$((compared + 1))
    if awk -v m="$mean" -v t="$THRESHOLD" 'BEGIN { exit !(m < t) }'; then
      printf '    %-24s %sx  %sREGRESSION%s\n' "$label" "$mean" "$R" "$Z"
      regressed=1
    else
      printf '    %-24s %sx  %sok%s\n' "$label" "$mean" "$G" "$Z"
    fi
  done

  # ---- devices that produced nothing ---------------------------------------
  # A device the run attached but that has no document refused to start (macrobenchmark's
  # LOW-BATTERY and other device-state checks abort its test) or never reached the measurement.
  # Name each one and fail the axis, but only their own cells: every phone that did measure has
  # already printed its verdict above, and the farm attributes the outcome per device from those
  # lines. This is what stops one drained phone from discarding the whole night's data.
  local missing=0
  local missing_labels=""
  local s model label note
  while IFS= read -r s; do
    [ -n "$s" ] || continue
    model="$(bounded_probe adb -s "$s" shell getprop ro.product.model 2>/dev/null | tr -d '\r')"
    label="$(device_label_for "$s")"
    [ -f "$CURRENT/$label/$RESULT_FILE" ] && continue
    note="$(device_note "$model")"
    flag "$label: no $AXIS result — ${note:-the benchmark produced no document for this device}"
    missing=$((missing + 1))
    missing_labels="$missing_labels $label"
  done <<<"$online"

  # ---- verdict -------------------------------------------------------------
  if [ "$regressed" -eq 1 ]; then
    echo "${R}${B}x the $AXIS axis regressed on at least one device${Z} — re-run once to rule out noise, or --record to accept"
    return "$EXIT_FAIL"
  fi
  if [ "$missing" -gt 0 ]; then
    echo "${R}${B}x no $AXIS result from${missing_labels}${Z} — charge and unlock the device(s) and re-run"
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
