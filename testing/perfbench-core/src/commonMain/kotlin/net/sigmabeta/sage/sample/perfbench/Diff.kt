package net.sigmabeta.sage.sample.perfbench

import kotlin.math.exp
import kotlin.math.ln
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One scenario compared between a baseline run and a current run, within its enclosing
 * [AxisComparison].
 *
 * [baselineMetric]/[currentMetric] are that axis's own primary cost metric, and [ratio] is the
 * baseline value divided by the current one. Every primary metric is lower-is-better, so a ratio
 * above one means the current run is better. The two raw values are carried alongside so a report
 * can show the metric the ratio was derived from.
 *
 * [scenario] is axis-specific: a scenario id for both the UI and startup axes. It is the key the
 * axis's two runs are paired by.
 */
@Serializable
data class ScenarioComparison(
    val scenario: String,
    @SerialName("baseline_metric") val baselineMetric: Double,
    @SerialName("current_metric") val currentMetric: Double,
    val ratio: Double,
)

/**
 * Every scenario compared in one axis, plus that axis's summary: the geometric mean of its
 * per-scenario ratios. [metricLabel] names the primary metric the ratios were derived from, so a
 * report can say what its numbers mean without knowing the axis's schema.
 */
@Serializable
data class AxisComparison(
    val axis: String,
    @SerialName("metric_label") val metricLabel: String,
    val scenarios: List<ScenarioComparison>,
    @SerialName("geometric_mean") val geometricMean: Double,
)

/** Every compared axis, plus the overall geometric mean across every compared scenario. */
@Serializable
data class DiffReport(
    val axes: List<AxisComparison>,
    @SerialName("geometric_mean") val geometricMean: Double,
)

/**
 * Why two runs may not be compared. Each variant names what differed so a report can explain the
 * refusal instead of printing a silently wrong ratio.
 */
sealed interface DiffRefusal {
    val message: String

    /** The two runs measured a different set of scenarios, so not every scenario has a baseline. */
    data class ScenarioSetMismatch(val baselineOnly: List<String>, val currentOnly: List<String>) : DiffRefusal {
        override val message: String = buildString {
            append("scenario set differs")
            if (baselineOnly.isNotEmpty()) append(" — only baseline: ${baselineOnly.joinToString(", ")}")
            if (currentOnly.isNotEmpty()) append(" — only current: ${currentOnly.joinToString(", ")}")
        }
    }

    /** Both runs carry no measurable scenarios, so there is nothing to compare. */
    data object EmptyScenarioSet : DiffRefusal {
        override val message: String = "both runs measured no scenarios — nothing to compare"
    }
}

/** The result of comparing two runs: either a refusal, or the per-axis report. */
sealed interface DiffOutcome {
    data class Refused(val reason: DiffRefusal) : DiffOutcome
    data class Compared(val report: DiffReport) : DiffOutcome
}

/**
 * Compares every axis present in both runs, refusing unless they are comparable.
 *
 * The guards run before any ratio is computed: a different scenario set means no comparison is
 * meaningful, so the caller is handed a [DiffRefusal] rather than a number. When the sets match,
 * each axis's scenarios are paired by its own key and their ratio computed from that axis's
 * [primaryMetric], plus the geometric mean of each axis's ratios and of every scenario overall.
 */
fun compareRuns(baseline: PerfResult, current: PerfResult): DiffOutcome {
    val refusal = comparabilityRefusal(baseline, current)
    if (refusal != null) return DiffOutcome.Refused(refusal)

    val axes = baseline.axes.keys.intersect(current.axes.keys).sorted().mapNotNull { axisId ->
        val baselineMetric = baseline.axes.getValue(axisId).primaryMetric()
        val currentValues = current.axes.getValue(axisId).primaryMetric().values
        val keys = baselineMetric.values.keys.intersect(currentValues.keys).sorted()
        if (keys.isEmpty()) return@mapNotNull null

        val scenarios = keys.map { key ->
            val baselineValue = baselineMetric.values.getValue(key)
            val currentValue = currentValues.getValue(key)
            ScenarioComparison(
                scenario = key,
                baselineMetric = baselineValue,
                currentMetric = currentValue,
                ratio = baselineValue / currentValue,
            )
        }
        AxisComparison(
            axis = axisId,
            metricLabel = baselineMetric.label,
            scenarios = scenarios,
            geometricMean = geometricMean(scenarios.map { it.ratio }),
        )
    }
    val ratios = axes.flatMap { it.scenarios }.map { it.ratio }
    if (ratios.isEmpty()) return DiffOutcome.Refused(DiffRefusal.EmptyScenarioSet)
    return DiffOutcome.Compared(DiffReport(axes, geometricMean(ratios)))
}

/** The first reason the two runs are not comparable, or `null` when every guard passes. */
private fun comparabilityRefusal(baseline: PerfResult, current: PerfResult): DiffRefusal? {
    val baselineScenarios = baseline.scenarioKeys()
    val currentScenarios = current.scenarioKeys()
    return when {
        baselineScenarios != currentScenarios -> DiffRefusal.ScenarioSetMismatch(
            baselineOnly = (baselineScenarios - currentScenarios).sorted(),
            currentOnly = (currentScenarios - baselineScenarios).sorted(),
        )

        baselineScenarios.isEmpty() -> DiffRefusal.EmptyScenarioSet

        else -> null
    }
}

/** Every scenario the run measured, across every axis, as `axis:scenario` keys. */
private fun PerfResult.scenarioKeys(): Set<String> =
    axes.entries.flatMap { (axis, value) ->
        value.resultKeys().map { key -> "$axis:$key" }
    }.toSet()

/** Every result identity an axis carries. */
private fun Axis.resultKeys(): List<String> = when (this) {
    is UiAxis -> results.map { it.scenario }
    is StartupAxis -> results.map { it.scenario }
}

/** One axis's primary cost metric: what its ratios are computed from, and how to label it. */
private data class PrimaryMetric(val label: String, val values: Map<String, Double>)

/**
 * The single per-axis metric lookup: the headline cost of each axis's scenarios, lower-is-better.
 *
 * [Axis] is sealed, so a new axis cannot be added without this `when` failing to compile — there is
 * one place, not a cascade, that decides what each axis is measured by.
 */
private fun Axis.primaryMetric(): PrimaryMetric = when (this) {
    is UiAxis -> PrimaryMetric(
        label = "frame p50 (ms)",
        values = results.associate { it.scenario to it.frameP50Ms },
    )

    is StartupAxis -> PrimaryMetric(
        label = "time to first content p50 (ms)",
        values = results.associate { it.scenario to it.timeToFirstContentP50Ms },
    )
}

/**
 * The geometric mean of [values] — the right summary for ratios, because a 2× improvement and a 2×
 * regression cancel. Computed through logs so a long list of large ratios cannot overflow.
 */
fun geometricMean(values: List<Double>): Double {
    require(values.isNotEmpty()) { "geometric mean of an empty list is undefined" }
    require(values.all { it > 0.0 }) { "geometric mean requires positive values, was $values" }
    return exp(values.sumOf { ln(it) } / values.size)
}
