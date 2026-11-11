#!/usr/bin/env bash
#
# device-ui.sh — run the on-device UI-scroll benchmark and gate on a regression.
#
# The ui axis: frame timing for each core surface (Home, the text and image List Items lists, the
# 50-image grid, Content detail), measured with a macrobenchmark (per-scenario compilation reset).
# Each scenario starts through the app's deep link, so navigation is not part of the measurement.
# See device-common.sh for the shared flow.
#
# Runs on every attached device.
#
# Usage:
#   scripts/benchmark/device-ui.sh [--record] [--threshold <ratio>]
#
# Environment:
#   SAGE_SAMPLE_PERF_BASELINE   baseline root (default build/perfbench-baseline/device)
set -uo pipefail
AXIS="ui"
TEST_CLASS="net.sigmabeta.sage.sample.benchmark.UiScrollBenchmark"
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/device-common.sh"
benchmark_device_main "$@"
