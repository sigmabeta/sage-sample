package net.sigmabeta.sage.sample.ui.components.api.subs

import androidx.compose.runtime.Composable

/**
 * Runs [content] inside a named tracing section.
 *
 * A consumer can wrap this around device-trace sections for macrobenchmarks. The sample has no
 * benchmark target, so the section is a structural no-op; keeping the call sites means a
 * consumer that adds profiling can fill this in without touching the renderers.
 */
@Composable
fun TraceSection(label: String, content: @Composable () -> Unit) {
    content()
}
