#!/usr/bin/env bash
#
# device-startup.sh — run the on-device startup benchmark and gate on a regression.
#
# The startup axis: cold and warm launch time to first content, measured with a macrobenchmark.
# See device-common.sh for the shared flow.
#
# Runs on every attached device.
#
# Usage:
#   scripts/benchmark/device-startup.sh [--record] [--threshold <ratio>]
#
# Environment:
#   SAGE_SAMPLE_PERF_BASELINE   baseline root (default build/perfbench-baseline/device)
set -uo pipefail
AXIS="startup"
TEST_CLASS="net.sigmabeta.sage.sample.benchmark.StartupBenchmark"
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/device-common.sh"
benchmark_device_main "$@"
