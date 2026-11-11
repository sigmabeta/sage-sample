package net.sigmabeta.sage.sample.perfbench

import java.io.File
import java.util.Locale
import kotlinx.serialization.encodeToString

private const val SCENARIO_WIDTH = 30
private const val COLUMN_WIDTH = 10
private const val DECIMALS = 2

/**
 * `diff` — compare every axis of two runs. Run A is the baseline, run B the current run. Prints the
 * per-axis, per-scenario ratio and the geometric mean (per axis and overall), or refuses (with the
 * reason) when the runs are not comparable. A refusal is not an error: the tool declines rather than
 * report a wrong number.
 */
fun runDiff(args: DiffArgs) {
    val baseline = readOrRefuse(args.baseline) ?: return
    val current = readOrRefuse(args.current) ?: return

    when (val outcome = compareRuns(baseline, current)) {
        is DiffOutcome.Refused -> println("Refusing to compare: ${outcome.reason.message}")
        is DiffOutcome.Compared -> if (args.json) printJson(outcome.report) else printReport(args, outcome.report)
    }
}

/**
 * Emits the report as the one perfbench JSON dialect. A comparison the harness *refuses* still
 * prints its prose refusal (to stdout) rather than JSON — there is no report to encode — so a
 * consumer must treat the absence of a JSON document as "not comparable".
 */
private fun printJson(report: DiffReport) {
    println(PerfbenchJson.encodeToString(report))
}

/** Reads a run, printing the refusal line and returning `null` when the file is not compatible. */
private fun readOrRefuse(file: File): PerfResult? = when (val read = readResult(file)) {
    is ResultRead.Decoded -> read.result

    is ResultRead.Incompatible -> {
        println(
            "Refusing to compare: ${read.file.absolutePath} is not a compatible perfbench result — " +
                "re-record the baseline (${read.reason})",
        )
        null
    }
}

private fun printReport(args: DiffArgs, report: DiffReport) {
    println("Diff (A = baseline, B = current)")
    println("  A: ${args.baseline.absolutePath}")
    println("  B: ${args.current.absolutePath}")
    report.axes.forEach(::printAxis)
    println()
    println("overall geometric mean: ${decimal(report.geometricMean)}x")
}

private fun printAxis(axis: AxisComparison) {
    val width = maxOf(SCENARIO_WIDTH, axis.scenarios.maxOf { it.scenario.length })
    println()
    println("${axis.axis} — ${axis.metricLabel}; ratio = A ÷ B, above 1 means B is better")
    println(
        "  ${"scenario".padEnd(width)}${"baseline".padStart(COLUMN_WIDTH)}" +
            "${"current".padStart(COLUMN_WIDTH)}${"ratio".padStart(COLUMN_WIDTH)}",
    )
    axis.scenarios.forEach { scenario ->
        println(
            "  ${scenario.scenario.padEnd(width)}${decimal(scenario.baselineMetric).padStart(COLUMN_WIDTH)}" +
                "${decimal(scenario.currentMetric).padStart(COLUMN_WIDTH)}" +
                "${"${decimal(scenario.ratio)}x".padStart(COLUMN_WIDTH)}",
        )
    }
    println("  geometric mean: ${decimal(axis.geometricMean)}x")
}

private fun decimal(value: Double): String = String.format(Locale.ROOT, "%.${DECIMALS}f", value)
