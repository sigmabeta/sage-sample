package net.sigmabeta.sage.sample.benchmark

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.File
import java.time.OffsetDateTime
import kotlinx.serialization.encodeToString
import net.sigmabeta.sage.sample.perfbench.Axis
import net.sigmabeta.sage.sample.perfbench.DeviceEnvironment
import net.sigmabeta.sage.sample.perfbench.PerfConfig
import net.sigmabeta.sage.sample.perfbench.PerfResult
import net.sigmabeta.sage.sample.perfbench.PerfbenchJson
import net.sigmabeta.sage.sample.nav.SampleDeepLink
import net.sigmabeta.sage.sample.perfbench.axisResultFileName
import net.sigmabeta.sage.sample.perfbench.deviceFingerprint

/**
 * Device-side plumbing shared by the macrobenchmarks: the target package, the fixed iteration
 * count, the device fingerprint, the deep-link launcher, and the per-axis result sink + progress
 * shell.
 *
 * The benchmark process runs in its own APK and cannot read the app's files; it only needs the
 * installed app's version for the fingerprint. The sample's demo content is deterministic, so there
 * is no corpus or snapshot to seed — a scenario starts on its screen through the app's own deep
 * link.
 */
internal object DeviceBenchmark {
    const val TARGET_PACKAGE = "net.sigmabeta.sage.sample"

    /** Measured iterations per scenario, shared so every device axis reports a comparable repeat. */
    const val ITERATIONS = 5

    private const val TAG = "SageBench"
    private const val UNKNOWN = "unknown"

    /**
     * The label every axis's progress line carries, so a run's begin/end/section lines are greppable
     * in logcat (`adb logcat -s SageBench`) amid the app's own logging.
     */
    private const val BENCH_LABEL = "[bench]"

    private var testStartNanos = 0L

    /** Logs the start of a device test and the clock its matching [endTest] measures against. */
    fun beginTest(name: String) {
        testStartNanos = System.nanoTime()
        Log.i(TAG, "$BENCH_LABEL begin test=$name")
    }

    /**
     * Logs the end of a device test with its wall duration. Call it from a `finally` so an exception
     * mid-run still emits the end; only a hard process crash leaves a `begin` with no matching `end`.
     */
    fun endTest(name: String) {
        Log.i(TAG, "$BENCH_LABEL end test=$name ms=${elapsedMillis(testStartNanos)}")
    }

    /**
     * Runs [deepLinkCommand] and logs its output. The command is the only place a launch can fail
     * without the app's own landing wait being able to name the cause, so its stdout/stderr is
     * worth keeping in logcat.
     */
    fun launchDeepLink(device: UiDevice, route: String): String {
        val output = device.executeShellCommand(deepLinkCommand(route))
        Log.i(TAG, "$BENCH_LABEL deep-link start route=$route output=${output.trim()}")
        return output
    }

    /**
     * Runs [block] as a labelled section: logs its begin, then its end with the wall duration, on
     * both success and failure. Extra [attributes] (such as a scenario id) are appended as
     * `key=value` pairs.
     */
    fun <T> section(name: String, vararg attributes: Pair<String, String>, block: () -> T): T {
        val start = System.nanoTime()
        Log.i(TAG, sectionLine("begin", name, attributes, ms = null))
        return try {
            block()
        } finally {
            Log.i(TAG, sectionLine("end", name, attributes, ms = elapsedMillis(start)))
        }
    }

    /** The device facts a [DeviceEnvironment] records, probed from the benchmark process. */
    fun environment(): DeviceEnvironment {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return DeviceEnvironment(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidBuild = Build.FINGERPRINT,
            abi = Build.SUPPORTED_ABIS.firstOrNull() ?: UNKNOWN,
            thermalStatus = thermalStatusName(context),
            appVersion = appVersion(context),
        )
    }

    /**
     * Assembles one run's perfbench document around the axis-specific [config] and [axes]. The
     * fingerprint is probed once by the caller before the measured loop, so it records the thermal
     * state at the start rather than after the run.
     */
    fun perfResult(
        environment: DeviceEnvironment,
        config: PerfConfig,
        axes: Map<String, Axis>,
    ): PerfResult = PerfResult(
        generated = OffsetDateTime.now().toString(),
        config = config,
        machine = deviceFingerprint(environment),
        axes = axes,
    )

    /**
     * Writes [result] into the run's additional test output dir, one artifact per axis it carries.
     *
     * Every `@Test` in a run writes into the *same* directory, so each axis goes to its own
     * [axisResultFileName] rather than a shared `results.json` (see [axisResultFileName] for why).
     */
    fun write(result: PerfResult) {
        val outDir = resultsOutputDir().apply { mkdirs() }
        result.axes.forEach { (axis, axisResult) ->
            val perAxis = result.copy(axes = mapOf(axis to axisResult))
            File(outDir, axisResultFileName(axis)).writeText(PerfbenchJson.encodeToString(perAxis))
        }
    }

    /**
     * The file a diagnostic is written to: the same additional-test-output directory AGP pulls,
     * under [fileName]. Returns a path even before the file exists.
     */
    fun artifactFile(fileName: String): File {
        val outDir = resultsOutputDir().apply { mkdirs() }
        return File(outDir, fileName)
    }

    /**
     * Writes a free-form artifact into the same additional-test-output directory AGP pulls, for a
     * diagnostic that is not a perfbench [Axis] — the UI hierarchy a scenario failed to reach.
     */
    fun writeArtifact(fileName: String, text: String) {
        artifactFile(fileName).writeText(text)
    }

    /**
     * The shell command that launches the app's own deep link for [route] (`home`,
     * `browse/list-items`, …), starting a fresh task each time.
     *
     * A shell `am start -W` rather than `MacrobenchmarkScope.startActivityAndWait`: the latter also
     * polls `dumpsys gfxinfo` for a new frame and fails on some devices (the OnePlus 7 Pro on API 30
     * never reports one) even though the activity is up. `-W` still waits for the start to be
     * accepted; the scenario's own landing wait is the real completion signal.
     *
     * `0x10008000` is `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`, so each iteration starts
     * from a clean task instead of restoring the previous scroll position.
     */
    fun deepLinkCommand(route: String): String =
        // No shell quotes around the URI: executeShellCommand does not strip them, so the quotes
        // become part of the data and the intent stops resolving. A route has no shell metacharacters.
        "am start -W -f 0x10008000 -a android.intent.action.VIEW " +
            "-d ${SampleDeepLink.SCHEME}://$route -p $TARGET_PACKAGE"

    private fun sectionLine(
        phase: String,
        name: String,
        attributes: Array<out Pair<String, String>>,
        ms: Long?,
    ): String = buildList {
        add(BENCH_LABEL)
        add(phase)
        add("section=$name")
        attributes.forEach { add("${it.first}=${it.second}") }
        if (ms != null) add("ms=$ms")
    }.joinToString(" ")

    private fun elapsedMillis(startNanos: Long): Long = (System.nanoTime() - startNanos) / NANOS_PER_MILLI

    /** The app's versionName, so a baseline is visibly tied to the build it measured. */
    @Suppress("DEPRECATION")
    private fun appVersion(context: Context): String =
        runCatching { context.packageManager.getPackageInfo(TARGET_PACKAGE, 0).versionName }
            .getOrNull()
            ?: UNKNOWN

    /** The platform thermal-status label, or `"unknown"` on API levels that can't report it. */
    private fun thermalStatusName(context: Context): String {
        val powerManager = context.getSystemService(PowerManager::class.java)
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || powerManager == null) {
            UNKNOWN
        } else {
            when (powerManager.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> "none"
                PowerManager.THERMAL_STATUS_LIGHT -> "light"
                PowerManager.THERMAL_STATUS_MODERATE -> "moderate"
                PowerManager.THERMAL_STATUS_SEVERE -> "severe"
                PowerManager.THERMAL_STATUS_CRITICAL -> "critical"
                PowerManager.THERMAL_STATUS_EMERGENCY -> "emergency"
                PowerManager.THERMAL_STATUS_SHUTDOWN -> "shutdown"
                else -> UNKNOWN
            }
        }
    }

    /** The directory AGP pulls additional test output from, falling back to app files when unset. */
    private fun resultsOutputDir(): File {
        val additional = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        return if (additional != null) {
            File(additional)
        } else {
            File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "perfbench")
        }
    }

    private const val NANOS_PER_MILLI = 1_000_000L
}
