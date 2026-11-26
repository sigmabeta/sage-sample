package net.sigmabeta.sage.sample.perfbench

/**
 * `perfbench` — the host-side companion to the Android `:benchmark` module. The device measures;
 * this CLI compares two runs so a local baseline can gate a regression, and merges several
 * recordings of one device into that baseline.
 */
fun main(rawArgs: Array<String>) {
    when (val command = rawArgs.firstOrNull()) {
        null, "-h", "--help", "help" -> printUsage()

        "diff" -> runDiff(parseDiffArgs(rawArgs.drop(1)))

        "baseline" -> runBaseline(parseBaselineArgs(rawArgs.drop(1)))

        else -> {
            println("Unknown command: $command\n")
            printUsage()
        }
    }
}

private fun printUsage() {
    println(
        """
        perfbench — performance benchmark results for the SAGE sample.

        Usage:
          perfbench diff --a <baseline> --b <current> [--json]
          perfbench baseline --runs <a.json,b.json,...> --out <baseline.json>

        diff compares every axis (ui, startup) of two runs. A run argument is a
        results-<axis>.json file. Every scenario must exist in both runs; otherwise the tool
        refuses to print a ratio that would be meaningless.

        The ratio is baseline ÷ current over each axis's primary cost (UI frame p50, startup
        time-to-first-content p50), so above 1 means the current run is better. --json emits the
        machine-readable report; scripts/benchmark/device-*.sh read its `geometric_mean`.

        baseline merges several recordings of one device into a baseline. Per scenario it keeps
        the whole entry of the recording whose diff metric is the median, so one lucky or unlucky
        run does not become the reference every later run is gated against. It prints each
        scenario's spread across the recordings, and refuses recordings diff would not compare.
        --runs lists the result files oldest first; the newest supplies the fingerprint.

        Run it:
          ./gradlew :testing:perfbench:run --args="diff --a build/perfbench-baseline/device/<label> --b build/perfbench-device/ui/current/<label> --json"
        """.trimIndent(),
    )
}
