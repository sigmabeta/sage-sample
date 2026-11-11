package net.sigmabeta.sage.sample.ui.components.api.subs

import androidx.compose.runtime.Composable
import androidx.tracing.Trace

/**
 * Android actual: a synchronous `Trace` section around the renderer's composition body. It lands in
 * a macrobenchmark Perfetto trace as the section name the UI scenario sums.
 *
 * The section is opened and closed around the composition call rather than in a `try/finally`:
 * Compose rejects try/catch around composable invocations. A composition exception aborts the frame
 * and reaches the crash handler with the section still open, which is the only path that leaves the
 * trace unbalanced — and that frame has already failed.
 */
@Composable
actual fun TraceSection(label: String, content: @Composable () -> Unit) {
    Trace.beginSection(label)
    content()
    Trace.endSection()
}
