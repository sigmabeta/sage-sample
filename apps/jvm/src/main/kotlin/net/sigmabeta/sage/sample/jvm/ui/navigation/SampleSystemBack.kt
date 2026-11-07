package net.sigmabeta.sage.sample.jvm.ui.navigation

import androidx.compose.runtime.Composable

/**
 * The platform's whole-window Back input.
 *
 * Desktop has no system Back yet; ticket 02 feeds Escape, Backspace and the mouse back button
 * through this seam so they run the same shared Back policy. Keeping the call site here means both
 * apps' shells have the same shape.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun SampleSystemBack(enabled: Boolean, onBack: () -> Unit) {
    // Desktop Back inputs are wired in ticket 02.
}
