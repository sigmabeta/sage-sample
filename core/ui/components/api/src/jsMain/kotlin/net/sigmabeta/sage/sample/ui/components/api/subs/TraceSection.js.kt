package net.sigmabeta.sage.sample.ui.components.api.subs

import androidx.compose.runtime.Composable

/** Browser actual: no Perfetto in the JS target, so the section is a structural no-op. */
@Composable
actual fun TraceSection(label: String, content: @Composable () -> Unit) {
    content()
}
