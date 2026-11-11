package net.sigmabeta.sage.sample.perfbench

/**
 * `perfbench` — the host-side companion to the Android `:benchmark` module. The device measures;
 * this CLI compares two runs so a local baseline can gate a regression.
 */
fun main(rawArgs: Array<String>) {
    when (val command = rawArgs.firstOrNull()) {
        null, "-h", "--help", "help" -> printUsage()

        "diff" -> runDiff(parseDiffArgs(rawArgs.drop(1)))

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

        diff compares every axis (ui, startup) of two runs. A run argument is a
        results-<axis>.json file. Every scenario must exist in both runs; otherwise the tool
        refuses to print a ratio that would be meaningless.

        The ratio is baseline ÷ current over each axis's primary cost (UI frame p50, startup
        time-to-first-content p50), so above 1 means the current run is better. --json emits the
        machine-readable report; scripts/benchmark/device-*.sh read its `geometric_mean`.

        Run it:
          ./gradlew :testing:perfbench:run --args="diff --a build/perfbench-baseline/device/<label> --b build/perfbench-device/ui/current/<label> --json"
        """.trimIndent(),
    )
}
