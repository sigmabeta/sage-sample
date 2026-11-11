package net.sigmabeta.sage.sample.ui.components.api.subs

import androidx.compose.runtime.Composable

/**
 * Runs [content] inside a named Perfetto / systrace section.
 *
 * The Android actual delegates to `androidx.tracing.Trace`, so the device scroll benchmark can sum
 * the labelled composition work next to its frame-time percentiles. Desktop and the browser have no
 * Perfetto, so their actuals are structural no-ops and the calls cost nothing.
 */
@Composable
expect fun TraceSection(label: String, content: @Composable () -> Unit)
